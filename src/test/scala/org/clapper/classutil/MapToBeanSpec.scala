package org.clapper.classutil

import java.lang.reflect.{Method, Proxy}

import scala.annotation.tailrec

class MapToBeanSpec extends BaseSpec {
  val map =  Map("oneInt" -> 1,
                 "twoFloat" -> 2f,
                 "threeString" -> "three",
                 "fourIntClass" -> classOf[Int],
                 "fiveMap" -> Map("one" -> 1, "two" -> 2.0))

  "MapToBean" should "recursively map values" in {
    val bean = MapToBean(map)

    val expectedNames = List(
      ("getOneInt", classOf[java.lang.Integer], false),
      ("getTwoFloat", classOf[java.lang.Float], false),
      ("getThreeString", classOf[String], false),
      ("getFourIntClass", classOf[Class[Int]], false),
      ("getFiveMap", classOf[Object], true),
      ("getFiveMap.getOne", classOf[Integer], false),
      ("getFiveMap.getTwo", classOf[java.lang.Double], false)
    )

    for ((name, cls, isProxy) <- expectedNames) {
      val method = resolveMethod(name, bean)
      val methodReturnType = method.getReturnType
      val isAssignable = cls.isAssignableFrom(methodReturnType)
      isAssignable shouldBe true
      if (isProxy)
        Proxy.isProxyClass(methodReturnType) shouldBe true
    }
  }

  it should "behave properly when non-recursive" in {
    val bean = MapToBean(map, recurse = false)
    val methodNames = bean.getClass.getMethods.map(_.getName).toSet
    val expectedNames = List(("getOneInt", classOf[java.lang.Integer]),
                             ("getTwoFloat", classOf[java.lang.Float]),
                             ("getThreeString", classOf[String]),
                             ("getFourIntClass", classOf[Class[Int]]),
                             ("getFiveMap", classOf[Map[String, Any]]))
    for ((name, cls) <- expectedNames) {
      val isAssignable = cls.isAssignableFrom(
        bean.getClass.getMethod(name).getReturnType
      )

      methodNames.contains(name) shouldBe true
      isAssignable shouldBe true
    }
  }

  it should "handle lots of beans" in {
    // Test a large number of generated beans, to ensure that none of the
    // generated class names clash. Added in response to Issue #1.

    val m = Map("a" -> "1", "b" -> "2")
    for (i <- 1 to 10000) MapToBean(m)
    System.gc()
    ()
  }

  it should "generate both Java and Scala getters" in {
    val bean = MapToBean(map)
    val expected = map.keySet ++
                   map.keySet.map(Util.scalaGetterNameToBeanName)

    // The bean class will have more methods than the map, because of standard
    // JVM methods like notify(), wait(), etc. But subtracting the generated
    // methods from the expected methods should yield an empty set (meaning
    // all expected methods were generated).
    val methods = bean.getClass.getMethods.map(_.getName).toSet

    (expected -- methods) shouldBe empty
  }

  private def resolveMethod(methodPath: String, obj: AnyRef): Method = {
    @tailrec
    def resolve(parts: List[String], o: AnyRef): Method = {
      parts match {
        case Nil => throw new Exception("unexpected Nil")
        case part :: Nil => o.getClass.getMethod(part)
        case part :: rest => resolve(rest, o.getClass.getMethod(part).invoke(o))
      }
    }

    resolve(methodPath.split("""\.""").toList, obj)
  }
}
