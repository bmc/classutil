/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the names "clapper.org", "ClassUtil", nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
*/

/**
 *
 */
package org.clapper.classutil.asm

import org.clapper.classutil._

import org.objectweb.asm.{ClassWriter, Type}
import org.objectweb.asm.Opcodes._

import java.lang.reflect.{Method, Proxy, InvocationHandler}

import scala.reflect.Manifest

/**
 * Takes a Scala `Map`, with `String` keys and object values, and generates
 * an object, with fields for each map value. Field that are, themselves,
 * `Map[String,Any]` objects can be recursively mapped, as well.
 *
 * The transformation results in an object that can only really be used
 * via reflection; however, that fits fine with some APIs that want to receive
 * Java Beans as parameters.
 *
 * There are some restrictions imposed on the map. First, each key must be
 * a valid Java identifier. Second, the keys are mapped to Java Bean
 * `get` accessors. For instance, a key name "foo" is mapped to a method
 * called `getFoo()`.
 */
private[classutil] class MapToObjectMapperImpl extends MapToObjectMapper
{
    private val AS_MAP_METHOD_NAME = "asMap"

    /* ---------------------------------------------------------------------- *\
                              Public Methods
    \* ---------------------------------------------------------------------- */

    /**
     * Transform a map into an object.
     *
     * @param map      the map
     * @param recurse  `true` to recursively map nested maps, `false` otherwise
     *
     * @return an instantiated object representing the map
     */
    def makeObject(map: Map[String, Any],
                   className: String,
                   recurse: Boolean = true): AnyRef =
    {
        // Strategy: Create an interface, load it, and generate a proxy that
        // implements the interface dynamically. The proxy handler resolves
        // references from the map.

        // Methods for each field, including bean methods.

        def transformValueIfMap(value: Any) =
        {
            if (recurse && isOfType[Map[String,Any]](value))
                makeObject(value.asInstanceOf[Map[String,Any]],
                           MapToObject.generatedClassName,
                           recurse)
            else
                value
        }

        def keyToMethodName(key: String) =
        {
            if (! key.forall(Character.isJavaIdentifierPart(_)))
                throw new IllegalArgumentException("Map key \"" + key + 
                                                   "\" is not a valid " +
                                                   "Java identifier.")
            "get" + key.take(1).toUpperCase + key.drop(1)
        }

        // If we're recursing, then first map any value that is, itself, a
        // Map[String,Any].

        val tuples1 = map.map(kv => (kv._1 -> transformValueIfMap(kv._2)))
        val newMap = Map(tuples1.toList: _*)

        // Map the keys to method names, with the same values as the existing
        // map.
        val tuples2 = newMap.keys.map(k => (keyToMethodName(k) -> newMap(k)))
        val methodNameMap = Map(tuples2.toList: _*)
        val classLoader = map.getClass.getClassLoader
        val interface = loadClass(
            classLoader,
            className,
            makeInterface(methodNameMap, map, className, recurse)
        )

        makeProxy(methodNameMap, map, interface, classLoader)
    }

    /* ---------------------------------------------------------------------- *\
                             * Private Methods
    \* ---------------------------------------------------------------------- */

    private def makeInterface(methodNameMap: Map[String, Any],
                              originalMap: Map[String, Any],
                              className: String,
                              recurse: Boolean): Array[Byte] =
    {
        val cw = new ClassWriter(0)
        cw.visit(V1_6,
                 ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
                 binaryClassName(className),
                 null,
                 "java/lang/Object",
                 null)

        for (methodName <- methodNameMap.keys)
        {
            val value = methodNameMap(methodName)
            val valueClass = value.asInstanceOf[AnyRef].getClass
            val asmType = Type.getType(valueClass)
            val returnType = asmType.getDescriptor

            cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT,
                           methodName,
                           "()" + returnType,
                           null,
                           null).
               visitEnd
        }

        val asMapClass = originalMap.getClass
        val asMapAsmType = Type.getType(asMapClass)
        val asMapReturnType = asMapAsmType.getDescriptor

        cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT,
                       AS_MAP_METHOD_NAME,
                       "()" + asMapReturnType,
                       null,
                       null).
            visitEnd

        cw.visitEnd
        cw.toByteArray
    }

    private def isOfType[T](v: Any)(implicit man: Manifest[T]): Boolean =
        man >:> Manifest.classType(v.asInstanceOf[AnyRef].getClass)

    private def makeProxy(methodNameMap: Map[String, Any],
                          originalMap: Map[String, Any],
                          interface: Class[_],
                          classLoader: ClassLoader): AnyRef =
    {
        val handler = new InvocationHandler
        {
            def invoke(proxy: Object,
                       method: Method,
                       args: Array[Object]): Object =
            {
                // It could be an invocation of a method that isn't one of the
                // ones we created from the map. In that case, just delegate
                // the method call to the original map.
                val methodName = method.getName
                if (methodName == AS_MAP_METHOD_NAME)
                    originalMap

                else
                {
                    methodNameMap.get(methodName) match
                    {
                        case None => method.invoke(methodNameMap, args: _*)
                        case Some(v) => v.asInstanceOf[AnyRef]
                    }
                }
            }
        }

        Proxy.newProxyInstance(classLoader, List(interface).toArray, handler)
    }

    private def binaryClassName(className: String): String =
        className.replaceAll("""\.""", "/")

    private def loadClass(classLoader: ClassLoader,
                          className: String,
                          classBytes: Array[Byte]): Class[_] =
    {
        val cls = Class.forName("java.lang.ClassLoader")
        val method = cls.getDeclaredMethod("defineClass",
                                           classOf[String],
                                           classOf[Array[Byte]],
                                           classOf[Int],
                                           classOf[Int])
        method.setAccessible(true)
        method.invoke(classLoader, className, classBytes,
                      new java.lang.Integer(0),
                      new java.lang.Integer(classBytes.length)).
        asInstanceOf[Class[_]]
    }
}
