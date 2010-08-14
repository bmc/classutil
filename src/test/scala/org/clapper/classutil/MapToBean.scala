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

import org.scalatest.{FunSuite, Assertions}
import org.clapper.classutil.MapToBean

class MapToBeanTest extends FunSuite
{
    val map =  Map("oneInt" -> 1,
                   "twoFloat" -> 2f,
                   "threeString" -> "three",
                   "fourIntClass" -> classOf[Int],
                   "fiveMap" -> Map("one" -> 1, "two" -> 2))

    test("MapToBean, recursive")
    {
        val bean = MapToBean(map)
        val methodNames = bean.getClass.getMethods.map(_.getName).toSet
        val expectedNames = List(("getOneInt", classOf[java.lang.Integer]),
                                 ("getTwoFloat", classOf[java.lang.Float]),
                                 ("getThreeString", classOf[String]),
                                 ("getFourIntClass", classOf[Class[Int]]),
                                 ("getFiveMap", classOf[Object]))
        for ((name, cls) <- expectedNames)
        {
            expect(true, "Looking for method name " + name + "()")
            {
                methodNames.contains(name)
            }

            expect(true, "Method " + name + " has appropriate type.")
            {
                cls.isAssignableFrom(
                    bean.getClass.getMethod(name).getReturnType
                )
            }
        }
    }

    test("MapToBean, non-recursive")
    {
        val bean = MapToBean(map, false)
        val methodNames = bean.getClass.getMethods.map(_.getName).toSet
        val expectedNames = List(("getOneInt", classOf[java.lang.Integer]),
                                 ("getTwoFloat", classOf[java.lang.Float]),
                                 ("getThreeString", classOf[String]),
                                 ("getFourIntClass", classOf[Class[Int]]),
                                 ("getFiveMap", classOf[Map[String, Any]]))
        for ((name, cls) <- expectedNames)
        {
            expect(true, "Looking for method name " + name + "()")
            {
                methodNames.contains(name)
            }

            expect(true, "Method " + name + " has appropriate type.")
            {
                cls.isAssignableFrom(
                    bean.getClass.getMethod(name).getReturnType
                )
            }
        }
    }
}
