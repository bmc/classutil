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

/**
 * Takes a Scala `Map`, with `String` keys and object values, and generates
 * a Java Bean object, with fields for each map value. Field that are,
 * themselves, `Map[String,Any]` objects can be recursively mapped, as
 * well.
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
private[classutil] class MapToBeanMapperImpl extends MapToBeanMapper
{
    /* ---------------------------------------------------------------------- *\
                              Public Methods
    \* ---------------------------------------------------------------------- */

    /**
     * Transform a map into a bean.
     *
     * @param map       the map
     * @param className name of generated class
     * @param recurse   `true` to recursively map nested maps, `false` otherwise
     *
     * @return an instantiated object representing the map
     */
    def makeBean(map: Map[String, Any],
                 className: String,
                 recurse: Boolean = true): AnyRef =
    {
        // Strategy: Create an interface, load it, and generate a proxy that
        // implements the interface dynamically. The proxy handler resolves
        // references from the map.

        // Methods for each field, including bean methods.

        def transformValueIfMap(value: Any) =
        {
            if (recurse && ClassUtil.isOfType[Map[String,Any]](value))
                makeObject(value.asInstanceOf[Map[String,Any]],
                           MapToBean.newGeneratedClassName,
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

        // Create the interface bytes. We need a map of names to return types
        // here.
        val interfaceBytes = InterfaceMaker.makeInterface(
            methodNameMap.map(kv => (kv._1, 
                                     InterfaceMaker.NoParams,
                                     kv._2.asInstanceOf[AnyRef].getClass)).
                          toSeq,
            className
        )

        // Load the class we just generated.

        val classLoader = map.getClass.getClassLoader
        val interface = ClassUtil.loadClass(classLoader,
                                            className,
                                            interfaceBytes)

        makeProxy(methodNameMap, map, interface, classLoader)
    }

    /* ---------------------------------------------------------------------- *\
                              Private Methods
    \* ---------------------------------------------------------------------- */

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
                methodNameMap.get(methodName) match
                {
                    case None => method.invoke(methodNameMap, args: _*)
                    case Some(v) => v.asInstanceOf[AnyRef]
                }
            }
        }

        Proxy.newProxyInstance(classLoader, List(interface).toArray, handler)
    }

    private def binaryClassName(className: String): String =
        className.replaceAll("""\.""", "/")
}
