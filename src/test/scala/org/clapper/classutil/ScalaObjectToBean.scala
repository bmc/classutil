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
import org.clapper.classutil.ScalaObjectToBean

class ScalaObjectToBean extends FunSuite
{
    test("ScalaObjectToBean, non-recursive")
    {
        case class Foo(name: String, 
                       iValue: Int,
                       fValue: Float)

        val foo = Foo("foo", 1009, 199392.0f)
        val bean = ScalaObjectToBean(foo)

        val expected = List(
            ("getName",   classOf[java.lang.String], foo.name),
            ("getIValue", java.lang.Integer.TYPE,    foo.iValue),
            ("getFValue", java.lang.Float.TYPE,      foo.fValue)
        )

        for ((name, cls, value) <- expected)
        {
            val methods = bean.getClass.getMethods.filter(_.getName == name)
            expect(1, "Should be 1 method named \"" + name + "\"")
            {
                methods.length
            }

            val method = methods(0)
            expect(true, "Method " + name + "() has appropriate type.")
            {
                cls.isAssignableFrom(method.getReturnType)
            }

            expect(value, "Method " + name + "() returns " + value)
            {
                method.invoke(bean)
            }
        }
    }

    test("ScalaObjectToBean, recursive")
    {
        println("*** NEED TO ADD RECURSIVE TEST")
    }
}
