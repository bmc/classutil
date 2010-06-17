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
 *
 * There are some restrictions imposed on the map. First, the key must be
 * a valid Java identifier.
 */
trait MapToObjectMapper
{
    /**
     * Transform a map into an object.
     *
     * @param map       the map
     * @param className the name to give the class
     * @param recurse   `true` to recursively map nested maps, `false` otherwise
     *
     * @return an instantiated object representing the map
     */
    def makeObject(map: Map[String, Any],
                   className: String,
                   recurse: Boolean = true): AnyRef
}

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
object MapToObject
{
    private val mapper = new org.clapper.classutil.asm.MapToObjectMapperImpl
    private val rng = new java.security.SecureRandom

    /**
     * Transform a map into an object.
     *
     * @param map       the map
     * @param className the name to give the class
     * @param recurse   `true` to recursively map nested maps, `false` otherwise
     *
     * @return an instantiated object representing the map
     */
    def apply(map: Map[String, Any], 
              className: String,
              recurse: Boolean): AnyRef =
        mapper.makeObject(map, className, recurse)

    /**
     * Transform a map into an object. The class name will be generated,
     * will be in the `org.clapper.classutil` package, and will have
     * a class name prefix of `MapToObject_`.
     *
     * @param map       the map
     * @param recurse   `true` to recursively map nested maps, `false` otherwise
     *
     * @return an instantiated object representing the map
     */
    def apply(map: Map[String, Any], recurse: Boolean = true): AnyRef =
        mapper.makeObject(map, generatedClassName, recurse)

    /**
     * Generate a class name.
     *
     * @return the class name
     */
    private[classutil] def generatedClassName =
        "org.clapper.classutil.MapToObject_" + rng.nextInt(100000)
}
