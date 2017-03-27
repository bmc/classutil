package org.clapper.classutil

import java.lang.reflect.{Method, Proxy}

import scala.util.Try
import scala.util.control.NonFatal

class ScalaObjectToBeanSpec extends BaseSpec {
  implicit class EnrichedClass(cls: Class[_]) {
    def methodForName(name: String): Option[Method] = {
      cls.getMethods.find(_.getName == name)
    }

    def hasMethod(name: String): Boolean = methodForName(name).isDefined
  }

  "apply" should "handle class getters and setters in non-recursive mode" in {
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
    val foo      = new Foo(OldName, OldInt, OldFloat)
    val bean     = ScalaObjectToBean(foo)

    val expectedGetters = List(
      ("getName",   classOf[java.lang.String], foo.name),
      ("getIValue", java.lang.Integer.TYPE,    foo.iValue),
      ("getFValue", java.lang.Float.TYPE,      foo.fValue)
    )

    for ((name, cls, value) <- expectedGetters) {
      val oMethod = bean.getClass.methodForName(name)
      oMethod shouldBe defined

      val method = oMethod.get
      cls.isAssignableFrom(method.getReturnType) shouldBe true

      method.invoke(bean) shouldBe value

      Proxy.isProxyClass(method.invoke(bean).getClass) shouldBe false
    }

    val expectedSetters = List(
      ("setName", "getName", classOf[java.lang.String], OldName, NewName),
      ("setIValue", "getIValue", java.lang.Integer.TYPE, OldInt, NewInt),
      ("setFValue", "getFValue", java.lang.Float.TYPE, OldFloat, NewFloat)
    )

    val beanClass = bean.getClass
    for ((setterName, getterName, cls, oldVal, newVal) <- expectedSetters) {
      val oSetterMethod = beanClass.methodForName(setterName)
      val oGetterMethod = beanClass.methodForName(getterName)

      oSetterMethod shouldBe defined
      oGetterMethod shouldBe defined

      val setterMethod = oSetterMethod.get
      val getterMethod = oGetterMethod.get

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
      obj.getClass.hasMethod(methodName) shouldBe expected

    // Ensure that nest methods are proxies.

    val isProxy = List(
      ("getName", beanFoo, false),
      ("getName", beanBar, false),
      ("getValue", beanFoo, false),
      ("getMyFoo", beanBar, true)
    )

    for ((methodName, obj, expected) <- isProxy) {
      val oMethod = obj.getClass.methodForName(methodName)
      oMethod shouldBe defined
      val value = oMethod.get.invoke(obj)
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

  it should "allow existing methods on the generated proxy" in {
    class PassThrough(val x: Int) {
      def mult(y: Int): Int = x * y
    }

    val obj = ScalaObjectToBean(new PassThrough(10))
    val cls = obj.getClass

    val oGetX = cls.methodForName("getX")
    oGetX shouldBe defined
    oGetX.map { _.invoke(obj) } shouldBe Some(10)

    val oMult = cls.methodForName("mult")
    oMult shouldBe defined
    oMult.map { _.invoke(obj, 2.asInstanceOf[AnyRef]) } shouldBe Some(20)
  }

  "generateBeanInterface" should "generate all getters/setters" in {
    class Foobar(i: Int, name: String, var address: String)

    val cls = classOf[Foobar]
    val settersGetters = ClassUtil.scalaAccessorMethods(cls)
    val beanNames = settersGetters.map(ClassUtil.beanName)
    val expectedMethods = (
      ClassUtil.nonFinalPublicMethods(cls).map(_.getName) ++ beanNames
    ).toSet

    val interface = ScalaObjectToBean.generateBeanInterface(cls)
    val generatedMethods = interface.getMethods.map(_.getName).toSet
    generatedMethods shouldBe expectedMethods
  }
}
