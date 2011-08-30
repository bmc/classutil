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

class ScalaObjectToBeanTest extends FunSuite {
  test("ScalaObjectToBean, non-recursive, getters and setters") {
    class Foo(var name: String, 
              var iValue: Int,
              var fValue: Float) {
      var k: Int = 0
      def setK(i: Int): Unit = {}
    }

    val OldName  = "foo"
    val OldInt   = 1009
    val OldFloat = 199392.0f
    val NewName  = "bar"
    val NewInt   = 234621
    val NewFloat = 123.3f
    val foo = new Foo(OldName, OldInt, OldFloat)
    val bean = ScalaObjectToBean(foo)

    val expectedGetters = List(
      ("getName",   classOf[java.lang.String], foo.name),
      ("getIValue", java.lang.Integer.TYPE,    foo.iValue),
      ("getFValue", java.lang.Float.TYPE,      foo.fValue)
    )

    for ((name, cls, value) <- expectedGetters) {
      val methods = bean.getClass.getMethods.filter(_.getName == name)
      expect(1, "Should be 1 method named \"" + name + "\"") {
        methods.length
      }

      val method = methods(0)
      expect(true, "Method " + name + "() has appropriate type.") {
        cls.isAssignableFrom(method.getReturnType)
      }

      expect(value, "Method " + name + "() returns " + value.toString) {
        method.invoke(bean)
      }

      expect(false, "Method should not return a proxy") {
        import java.lang.reflect.Proxy

        Proxy.isProxyClass(method.invoke(bean).getClass)
      }
    }

    val expectedSetters = List(
      ("setName", "getName", classOf[java.lang.String], OldName, NewName),
      ("setIValue", "getIValue", java.lang.Integer.TYPE, OldInt, NewInt),
      ("setFValue", "getFValue", java.lang.Float.TYPE, OldFloat, NewFloat)
    )

    val allMethods = bean.getClass.getMethods

    for ((setterName, getterName, cls, oldVal, newVal) <- expectedSetters) {
      val setterMethods = allMethods.filter(_.getName == setterName)
      val getterMethods = allMethods.filter(_.getName == getterName)

      expect(1, "Should be 1 method named \"" + setterName + "\"") {
        setterMethods.length
      }

      expect(1, "Should be 1 method named \"" + getterName + "\"") {
        getterMethods.length
      }

      val setterMethod = setterMethods(0)
      val getterMethod = getterMethods(0)

      expect(true, getterName + "() has appropriate type.") {
        cls.isAssignableFrom(getterMethod.getReturnType)
      }

      expect(oldVal, getterName + "() returns " + oldVal.toString) {
        getterMethod.invoke(bean)
      }

      expect(false, getterName + "() should not return a proxy") {
        import java.lang.reflect.Proxy

        Proxy.isProxyClass(getterMethod.invoke(bean).getClass)
      }

      expect(1, setterName + "() total parameters") {
        setterMethod.getParameterTypes.length
      }

      expect(true, setterName + "()'s parameter type is correct") {
        val paramTypes = setterMethod.getParameterTypes
        cls.isAssignableFrom(paramTypes(0))
      }

      expect(true, setterName + "() returns void") {
        setterMethod.getReturnType.getName == "void"
      }

      expect(true, setterName + "() changes the value") {
        setterMethod.invoke(bean, newVal.asInstanceOf[AnyRef])
        getterMethod.invoke(bean) == newVal
      }
    }
  }

  test("ScalaObjectToBean, recursive, getters only") {
    case class Foo(name: String, value: Int)
    case class Bar(name: String, myFoo: Foo)

    val foo = Foo("foo", 100)
    val bar = Bar("bar", foo)

    val beanFoo = ScalaObjectToBean(foo)
    val beanBar = ScalaObjectToBean(bar)

    def hasMethod(obj: AnyRef, methodName: String) = {
      try {
        obj.getClass.getMethod(methodName)
        true
      }

      catch {
        case _: NoSuchMethodException => false
      }
    }

    // Ensure that all the methods are present.

    val hasMethods = List(
      ("getName", "foo", foo, false),
      ("getValue", "foo", foo, false),
      ("getName", "beanFoo", beanFoo, true),
      ("getValue", "beanFoo", beanFoo, true),

      ("getName", "bar", bar, false),
      ("getValue", "bar", bar, false),
      ("getMyFoo", "bar", bar, false),
      ("getName", "beanBar", beanBar, true),
      ("getValue", "beanBar", beanBar, false),
      ("getMyFoo", "beanBar", beanBar, true)
    )

    for ((methodName, label, obj, expected) <- hasMethods) {
      expect(expected, label + " has " + methodName + " method") {
        hasMethod(obj, methodName)
      }
    }

    // Ensure that nest methods are proxies.

    val isProxy = List(
      ("getName", "beanFoo", beanFoo, false),
      ("getName", "beanBar", beanBar, false),
      ("getValue", "beanFoo", beanFoo, false),
      ("getMyFoo", "beanBar", beanBar, true)
    )

    for ((methodName, label, obj, expected) <- isProxy) {
      expect(expected, label + "." + methodName + " returns Proxy") {
        import java.lang.reflect.Proxy
        val value = obj.getClass.getMethod(methodName).invoke(obj)
        Proxy.isProxyClass(value.getClass)
      }
    }

    // Ensure that the nest proxies return the right values.

    val beanBarFoo = beanBar.getClass.getMethod("getMyFoo").invoke(beanBar)

    val values = List(
      ("beanFoo", beanFoo, "getValue", foo.value),
      ("beanFoo", beanFoo, "getName", foo.name),
      ("beanBar.getMyFoo.getValue", beanBarFoo, "getValue", foo.value),
      ("beanBar.getMyFoo.getName", beanBarFoo, "getName", foo.name)
    )

    for ((label, obj, methodName, expected) <- values) {
      expect(expected, label + "." + methodName + "=" + expected) {
        obj.getClass.getMethod(methodName).invoke(obj)
      }
    }
  }
}
