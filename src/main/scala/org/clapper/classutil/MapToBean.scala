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
package org.clapper.classutil

/**
 * Takes a Scala `Map`, with `String` keys and object values, and generates
 * an object, with fields for each map value. Field that are, themselves,
 * `Map[String,Any]` objects can be recursively mapped, as well.
 */
trait MapToBeanMapper
{
    /**
     * Transform a map into a bean.
     *
     * @param map       the map
     * @param className the name to give the class
     * @param recurse   `true` to recursively map nested maps, `false` otherwise
     *
     * @return an instantiated object representing the map
     *
     * @deprecated Use makeBean() instead.
     */
    def makeObject(map: Map[String, Any],
                   className: String,
                   recurse: Boolean = true): AnyRef =
    {
        makeBean(map, className, recurse)
    }

    /**
     * Transform a map into a bean.
     *
     * @param map       the map
     * @param className the name to give the class
     * @param recurse   `true` to recursively map nested maps, `false` otherwise
     *
     * @return an instantiated object representing the map
     */
    def makeBean(map: Map[String, Any],
                 className: String,
                 recurse: Boolean = true): AnyRef
}

/**
 * Takes a Scala `Map`, with `String` keys and object values, and generates
 * a Java Bean object, with fields for each map value. Field that are,
 * themselves, `Map[String,Any]` objects can be recursively mapped, as
 * well. The map's keys are mapped to Java Bean `get` accessors. For
 * instance, a key name "foo" is mapped to a method called `getFoo()`.
 *
 * The transformation results in an object that can only really be used
 * via reflection; however, that fits fine with some APIs that want to receive
 * Java Beans as parameters.
 *
 *
 * There are some restrictions imposed on any map that is to be converted.
 *
 * <ul>
 *   <li> Only maps with string keys can be converted.
 *   <li> The string keys must be valid Java identifiers.
 * </ul>
 *
 * Here's a simple example:
 *
 * {{{
 * import org.clapper.classutil._
 *
 * val charList = List('1', '2', '3')
 *
 * val subMap = Map("sub1" -> 1, "sub2" -> 2)
 * val m =  Map("oneInt" -> 1,
 *              "twoFloat" -> 2f,
 *              "threeString" -> "three",
 *              "fourIntClass" -> classOf[Int],
 *              "fiveMap" -> subMap,
 *              "sixList" -> charList)
 * val obj = MapToBean(m)
 *
 * def showName(name: String) = (name startsWith "get")
 *
 * obj.getClass.getMethods.filter(m => showName(m.getName)).foreach(println _)
 *
 * def call(methodName: String) =
 * {
 *     val method = obj.getClass.getMethod(methodName)
 *     method.invoke(obj)
 * }
 *
 * println
 * println("getFiveMap returns " + call("getFiveMap"))
 * }}}
 *
 * That Scala script will produce output like the following:
 *
 * {{{
 * public final $Proxy0 $Proxy1.getFiveMap()
 * public final java.lang.Integer $Proxy1.getOneInt()
 * public final java.lang.Class $Proxy1.getFourIntClass()
 * public final scala.collection.immutable.$colon$colon $Proxy1.getSixList()
 * public final java.lang.Float $Proxy1.getTwoFloat()
 * public final java.lang.String $Proxy1.getThreeString()
 * public static java.lang.Class java.lang.reflect.Proxy.getProxyClass(java.lang.ClassLoader,java.lang.Class[]) throws java.lang.IllegalArgumentException
 * public static java.lang.reflect.InvocationHandler java.lang.reflect.Proxy.getInvocationHandler(java.lang.Object) throws java.lang.IllegalArgumentException
 * public final native java.lang.Class java.lang.Object.getClass()
 *
 * getFiveMap returns Map(getSub1 -> 1, getSub2 -> 2)
 * }}}
 */
object MapToBean extends ClassNameGenerator
{
    val ClassNamePrefix = "org.clapper.classutil.MapBean"

    private val mapper = new org.clapper.classutil.asm.MapToBeanMapperImpl

    /**
     * Transform a map into an object. The class name will be generated,
     * will be in the `org.clapper.classutil` package, and will have
     * a class name prefix of `MapBean_`.
     *
     * @param map       the map
     * @param recurse   `true` to recursively map nested maps, `false` otherwise
     *
     * @return an instantiated object representing the map
     */
    def apply(map: Map[String, Any], recurse: Boolean = true): AnyRef =
        mapper.makeBean(map, newGeneratedClassName, recurse)

    /**
     * Transform a map into an object.
     *
     * @param map       the map
     * @param className the desired class name
     * @param recurse   `true` to recursively map nested maps, `false`
     *                  otherwise. Recursively mapped maps will have generated
     *                  class names.
     *
     * @return an instantiated object representing the map
     */
    def apply(map: Map[String, Any],
              className: String,
              recurse: Boolean): AnyRef =
        mapper.makeBean(map, className, recurse)
}
