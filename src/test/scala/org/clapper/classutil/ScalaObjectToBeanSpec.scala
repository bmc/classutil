package org.clapper.classutil

import java.lang.reflect.Proxy

import scala.util.Try
import scala.util.control.NonFatal

class ScalaObjectToBeanSpec extends BaseSpec {
  "ScalaObjectToBean" should "handle class getters and setters in non-recursive mode" in {
    class Foo(var name: String, var iValue: Int, var fValue: Float) {
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
      methods.length shouldBe 1

      val firstMethod = methods.head
      cls.isAssignableFrom(firstMethod.getReturnType) shouldBe true

      firstMethod.invoke(bean) shouldBe value

      Proxy.isProxyClass(firstMethod.invoke(bean).getClass) shouldBe false
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

      setterMethods.length shouldBe 1
      getterMethods.length shouldBe 1

      val setterMethod = setterMethods.head
      val getterMethod = getterMethods.head

      cls.isAssignableFrom(getterMethod.getReturnType) shouldBe true
      getterMethod.invoke(bean) shouldBe oldVal
      Proxy.isProxyClass(getterMethod.invoke(bean).getClass) shouldBe false

      setterMethod.getParameterTypes.length shouldBe 1
      val paramTypes = setterMethod.getParameterTypes
      cls.isAssignableFrom(paramTypes.head) shouldBe true
      setterMethod.getReturnType.getName shouldBe "void"

      setterMethod.invoke(bean, newVal.asInstanceOf[AnyRef])
      getterMethod.invoke(bean) shouldBe newVal
    }
  }
  it should "handle class getters in recursive mode" in {
    case class Foo(name: String, value: Int)
    case class Bar(name: String, myFoo: Foo)

    val foo = Foo("foo", 100)
    val bar = Bar("bar", foo)

    val beanFoo = ScalaObjectToBean(foo)
    val beanBar = ScalaObjectToBean(bar)

    def hasMethod(obj: AnyRef, methodName: String) = {
      Try {
        obj.getClass.getMethod(methodName)
        true
      }
      .recover {
        case NonFatal(_) => false
      }
      .get
    }

    // Ensure that all the methods are present.

    val hasMethods = List(
      ("getName", foo, false),
      ("getValue", foo, false),
      ("getName", beanFoo, true),
      ("getValue", beanFoo, true),

      ("getName", bar, false),
      ("getValue", bar, false),
      ("getMyFoo", bar, false),
      ("getName", beanBar, true),
      ("getValue", beanBar, false),
      ("getMyFoo", beanBar, true)
    )

    for ((methodName, obj, expected) <- hasMethods)
      hasMethod(obj, methodName) shouldBe expected

    // Ensure that nest methods are proxies.

    val isProxy = List(
      ("getName", beanFoo, false),
      ("getName", beanBar, false),
      ("getValue", beanFoo, false),
      ("getMyFoo", beanBar, true)
    )

    for ((methodName, obj, expected) <- isProxy) {
      val value = obj.getClass.getMethod(methodName).invoke(obj)
      Proxy.isProxyClass(value.getClass) shouldBe expected
    }

    // Ensure that the nest proxies return the right values.

    val beanBarFoo = beanBar.getClass.getMethod("getMyFoo").invoke(beanBar)

    val values = List(
      (beanFoo, "getValue", foo.value),
      (beanFoo, "getName", foo.name),
      (beanBarFoo, "getValue", foo.value),
      (beanBarFoo, "getName", foo.name),
      (beanBar, "getName", bar.name),
      (beanBar, "getMyFoo", bar.myFoo)
    )

    for ((obj, methodName, expected) <- values) {
      obj.getClass.getMethod(methodName).invoke(obj) shouldBe expected
    }
  }
}
