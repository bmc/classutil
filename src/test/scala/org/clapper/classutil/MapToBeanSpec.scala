package org.clapper.classutil

class MapToBeanSpec extends BaseSpec {
  val map =  Map("oneInt" -> 1,
                 "twoFloat" -> 2f,
                 "threeString" -> "three",
                 "fourIntClass" -> classOf[Int],
                 "fiveMap" -> Map("one" -> 1, "two" -> 2))

  "MapToBean" should "recursively map values" in {
    val bean = MapToBean(map)
    val methodNames = bean.getClass.getMethods.map(_.getName).toSet
    val expectedNames = List(("getOneInt", classOf[java.lang.Integer]),
                             ("getTwoFloat", classOf[java.lang.Float]),
                             ("getThreeString", classOf[String]),
                             ("getFourIntClass", classOf[Class[Int]]),
                             ("getFiveMap", classOf[Object]))
    for ((name, cls) <- expectedNames) {
      val isAssignable = cls.isAssignableFrom(
        bean.getClass.getMethod(name).getReturnType
      )
      methodNames.contains(name) shouldBe true
      isAssignable shouldBe true
    }

    val getFiveMap = bean.getClass.getMethod("getFiveMap")
    val obj = getFiveMap.invoke(bean)
    val getOne = obj.getClass.getMethod("getOne")

    getOne.invoke(obj) shouldBe 1
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
                   map.keySet.map(MapToBeanUtil.keyToBeanMethodName)

    // The bean class will have more methods than the map, because of standard
    // JVM methods like notify(), wait(), etc. But subtracting the generated
    // methods from the expected methods should yield an empty set (meaning
    // all expected methods were generated).
    val methods = bean.getClass.getMethods.map(_.getName).toSet

    (expected -- methods) shouldBe empty
  }
}
