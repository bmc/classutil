package org.clapper.classutil

import java.lang.reflect.{Method, Proxy}

class ScalaObjectToBeanSpec extends BaseSpec {
  implicit class EnrichedClass(cls: Class[_]) {
    def methodForName(name: String): Option[Method] = {
      cls.getMethods.find(_.getName == name)
    }

    def hasMethod(name: String): Boolean = methodForName(name).isDefined

    def invokeOn(obj: AnyRef, method: String, args: AnyRef*): Option[AnyRef] = {
      methodForName(method).map { _.invoke(obj, args: _*) }
    }
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

    // Ensure that returned nested objects are of type Proxy, where
    // appropriate.

    val isProxy = List(
      ("getName", beanFoo, false),
      ("getName", beanBar, false),
      ("getValue", beanFoo, false),
      ("getMyFoo", beanBar, true)
    )

    for ((methodName, obj, expected) <- isProxy) {
      val oValue = obj.getClass.methodForName(methodName).map(_.invoke(obj))
      oValue shouldBe defined
      oValue.map(v => Proxy.isProxyClass(v.getClass)) shouldBe Some(expected)
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
      obj.getClass.methodForName(methodName).map(_.invoke(obj)) shouldBe Some(expected)
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

  "withResultMapper" should "permit a result mapper" in {
    class Baz(val x: Int, val y: Double, var z: Int)

    val baseObj = new Baz(10, 20.0, 30)
    val XMult = 10
    val YMult = 20

    var interceptorCalled = false
    val bean = ScalaObjectToBean.withResultMapper(baseObj) { (name, res) =>
      interceptorCalled = true
      (name, res) match {
        case ("getX", n: java.lang.Integer) => new Integer(n * XMult)
        case ("getY", n: java.lang.Double)  => new java.lang.Double(n * YMult)
        case ("getZ", n: java.lang.Integer) => res
        case _                              => res
      }
    }

    val cls = bean.getClass
    cls.invokeOn(bean, "getX") shouldBe Some(baseObj.x * XMult)
    interceptorCalled shouldBe true

    interceptorCalled = false
    cls.invokeOn(bean, "getY") shouldBe Some(baseObj.y * YMult)
    interceptorCalled shouldBe true

    interceptorCalled = false
    cls.invokeOn(bean, "getZ") shouldBe Some(baseObj.z)
    interceptorCalled shouldBe true
  }

  it should "permit a mapper with nested objects recursively mapped" in {
    case class Address(street: String, city: String, state: String, zip: String)
    case class Person(name: String, age: Int, address: Address)

    val addr = Address("123 Main St", "Anytown", "PA", "19999")
    val person = Person("James Foobar", 62, addr)
    val bean = ScalaObjectToBean.withResultMapper(person) { (name, res) =>
      (name, res) match {
        case ("getAddress", addr1) =>
          // Should be a proxy.
          Proxy.isProxyClass(addr1.getClass) shouldBe true
          addr1

        case ("address", addr2) =>
          // should be a proxy. Swap it out for the real thing.
          Proxy.isProxyClass(addr2.getClass) shouldBe true
          addr

        case _ => res
      }
    }

    val cls = bean.getClass
    val oAddrProxy1 = cls.invokeOn(bean, "getAddress")
    oAddrProxy1.map { v => Proxy.isProxyClass(v.getClass) } shouldBe Some(true)

    val oAddrProxy2 = cls.invokeOn(bean, "address")
    // The post-call callback should've changed the result to an Address
    oAddrProxy2.map { v => Proxy.isProxyClass(v.getClass) } shouldBe Some(false)
    oAddrProxy2.get should be theSameInstanceAs addr
  }

  it should "permit a mapper with nested objects not recursively mapped" in {
    case class Address(street: String, city: String, state: String, zip: String)
    case class Person(name: String, age: Int, address: Address)

    val addr = Address("123 Main St", "Anytown", "PA", "19999")
    val person = Person("James Foobar", 62, addr)
    val bean = ScalaObjectToBean.withResultMapper(person, recurse=false) { (name, res) =>
      (name, res) match {
        case ("getAddress", addr1) =>
          // Should not be a proxy.
          Proxy.isProxyClass(addr1.getClass) shouldBe false
          addr1

        case ("address", addr2) =>
          // Should not be a proxy.
          Proxy.isProxyClass(addr2.getClass) shouldBe false
          addr2

        case _ => res
      }
    }

    val cls = bean.getClass
    val oAddrProxy1 = cls.invokeOn(bean, "getAddress")
    oAddrProxy1.map { v => Proxy.isProxyClass(v.getClass) } shouldBe Some(false)
    oAddrProxy1.get should be theSameInstanceAs addr

    val oAddrProxy2 = cls.invokeOn(bean, "address")
    // The post-call callback should've changed the result to an Address
    oAddrProxy2.map { v => Proxy.isProxyClass(v.getClass) } shouldBe Some(false)
    oAddrProxy2.get should be theSameInstanceAs addr
  }
}
