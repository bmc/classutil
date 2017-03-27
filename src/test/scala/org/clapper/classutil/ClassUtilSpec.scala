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

package org.clapper.classutil

import org.scalatest.{FunSuite, Assertions}

class ClassUtilSpec extends BaseSpec {
  "isPrimitive" should "handle Scala values" in {
    val data = List(
      (1,          true),
      (1.0f,       true),
      (1.0,        true),
      (1.0.toLong, true),
      ('c',        true),
      ('b'.toByte, true),
      ("string",   false),
      (this,       false)
    )

    for ((value, expected) <- data) {
      val valueClass = value.asInstanceOf[AnyRef].getClass
      ClassUtil.isPrimitive(valueClass) shouldBe expected
    }
  }

  it should "handle Scala classes" in {
    import scala.language.existentials

    val data = List[(Class[_], Boolean)](
      (classOf[Int],    true),
      (classOf[Byte],   true),
      (classOf[Char],   true),
      (classOf[Short],  true),
      (classOf[Int],    true),
      (classOf[Long],   true),
      (classOf[Float],  true),
      (classOf[Double], true),
      (classOf[Unit],   true),
      (this.getClass,   false)
    )

    for ((cls, expected) <- data)
      ClassUtil.isPrimitive(cls) shouldBe expected
  }

  it should "handle Java classes" in {
    val data = List(
      (java.lang.Integer.valueOf(1),                    true),
      (java.lang.Float.valueOf(1.0f),                   true),
      (java.lang.Double.valueOf(1.0),                   true),
      (java.lang.Long.valueOf(1),                       true),
      (java.lang.Character.valueOf('c'),                true),
      (java.lang.Byte.valueOf(0x33.asInstanceOf[Byte]), true)
    )

    for ((value, expected) <- data) {
      val valueClass = value.asInstanceOf[AnyRef].getClass
      ClassUtil.isPrimitive(valueClass) shouldBe expected
    }
  }

  "classSignature" should "produce a valid signature" in {
    import scala.language.existentials

    val Primitives = List[Class[_]](
      classOf[Boolean],
      classOf[Byte],
      classOf[Char],
      classOf[Short],
      classOf[Int],
      classOf[Long],
      classOf[Float],
      classOf[Double],
      classOf[Unit]
    )

    val PrimitivesToTest = Primitives.map { cls =>
      (cls, ClassUtil.PrimitiveSigMap(cls.getName))
    }

    val ClassesToTest = Seq[(Class[_], String)](
      (getClass,        "Lorg/clapper/classutil/ClassUtilSpec;"),
      (classOf[String], "Ljava/lang/String;")
    )

    for ((cls, signature) <- PrimitivesToTest ++ ClassesToTest)
      ClassUtil.classSignature(cls) shouldBe signature
  }

  "methodSignature" should "produce a valid method signature" in {
    class Foo {
      def a: Unit = {}
      def b: String = {""}
      def c(s: String): Unit = {}
      def d(s: String): String = {""}
      def e(s: Array[String]): String = {""}
      def f(s: Array[String], i: Int, f: Float): Double = {0.0d}
      def g(m: Map[Int, String], s: String): Seq[String] = {Seq("")}
    }

    val methodMap = classOf[Foo].getMethods.map{m => m.getName -> m}.toMap
    val data = List(
      (methodMap("a"), "()V"),
      (methodMap("b"), "()Ljava/lang/String;"),
      (methodMap("c"), "(Ljava/lang/String;)V"),
      (methodMap("d"), "(Ljava/lang/String;)Ljava/lang/String;"),
      (methodMap("e"), "([Ljava/lang/String;)Ljava/lang/String;"),
      (methodMap("f"), "([Ljava/lang/String;IF)D"),
      (methodMap("g"), "(Lscala/collection/immutable/Map;Ljava/lang/String;)Lscala/collection/Seq;")
    )

    def methodToString(m: java.lang.reflect.Method): String = {
      val params = m.getParameterTypes.map(_.getCanonicalName).mkString(", ")
      s"${m.getName}($params)"
    }

    for ((method, signature) <- data)
      ClassUtil.methodSignature(method) shouldBe signature
  }

  "isGetter" should "properly identify a getter" in {
    case class Foo(i: Int)
    val methods = classOf[Foo].getMethods.filter(_.getName == "i")
    methods should have length 1
    ClassUtil.isGetter(methods.head) shouldBe true
  }

  it should "properly identify a non-getter" in {
    case class Foo(i: Int)
    val methods = classOf[Foo].getMethods.filter(_.getName == "toString")
    methods should have length 1
    ClassUtil.isGetter(methods.head) shouldBe false
  }

  it should "not confuse a setter with a getter" in {
    class Foo(var i: Int)
    val methods = classOf[Foo].getMethods.filter(_.getName == "i_$eq")
    methods should have length 1
    ClassUtil.isGetter(methods.head) shouldBe false
  }

  "isSetter" should "properly identify a setter" in {
    class Foo(var i: Int)
    val methods = classOf[Foo].getMethods.filter(_.getName == "i_$eq")
    methods should have length 1
    ClassUtil.isSetter(methods.head) shouldBe true
  }

  it should "properly identify a non-setter" in {
    case class Foo(i: Int)
    val methods = classOf[Foo].getMethods.filter(_.getName == "toString")
    methods should have length 1
    ClassUtil.isSetter(methods.head) shouldBe false
  }

  it should "not confuse a getter with a setter" in {
    class Foo(var i: Int)
    val methods = classOf[Foo].getMethods.filter(_.getName == "i")
    methods should have length 1
    ClassUtil.isSetter(methods.head) shouldBe false
  }

  "beanName" should "produce valid getter and setter names" in {
    case class Foo(fieldA: String, fieldB: Int)

    val methods = ClassUtil.scalaAccessorMethods(classOf[Foo])
    methods.length should be > 0
    for (m <- methods) {
      if (ClassUtil.isGetter(m))
        ClassUtil.beanName(m) should startWith ("get")
      else
        ClassUtil.beanName(m) should startWith ("set")
    }
  }

  "scalaAccessorMethods" should "find all getters" in {
    case class Foo(fieldA: String, fieldB: Int, i: BigInt, s: String)
    val methodNames = ClassUtil.scalaAccessorMethods(classOf[Foo]).map(_.getName)
    methodNames should have length 4
    methodNames.toSet should be (Set("fieldA", "fieldB", "i", "s"))
  }

  it should "find all setters" in {
    class Foo(fieldA: String, var fieldB: Int, var i: BigInt, var s: String)
    val methodNames = ClassUtil
      .scalaAccessorMethods(classOf[Foo])
      .filter(ClassUtil.isSetter)
      .map(_.getName)
    methodNames should have length 3
    methodNames.toSet should be (Set("fieldB_$eq", "i_$eq", "s_$eq"))
  }

  it should "handle a trait with a getter" in {
    trait Foo { def foo: Int = 10 }
    val methodNames = ClassUtil
      .scalaAccessorMethods(classOf[Foo])
      .map(_.getName)
    methodNames should have length 1
    methodNames.head shouldBe "foo"
  }

  it should "handle a trait with a setter" in {
    trait Foo { var i: Int }
    val methodNames = ClassUtil
      .scalaAccessorMethods(classOf[Foo])
      .filter(ClassUtil.isSetter)
      .map(_.getName)
    methodNames should have length 1
    methodNames.head shouldBe "i_$eq"
  }

  it should "handle an empty trait" in {
    trait Foo
    val methodNames = ClassUtil.scalaAccessorMethods(classOf[Foo])
    methodNames should have length 0
  }

  it should "handle an abstract class" in {
    abstract class Bar
    val methodNames = ClassUtil.scalaAccessorMethods(classOf[Bar])
    methodNames should have length 0
  }

  it should "handle an empty class" in {
    class Bar
    val methodNames = ClassUtil.scalaAccessorMethods(classOf[Bar])
    methodNames should have length 0
  }

  "nonFinalPublicMethods" should "skip non-public and final methods" in {
    class Baz(val name: String, val age: Int) {
      final val upperClassName = name.toUpperCase
    }

    val cls = classOf[Baz]
    val nonPublicOrFinalMethods = cls
      .getMethods
      .filter { m =>
        import java.lang.reflect.{Modifier => JModifier}
        val modifiers = m.getModifiers

        ((modifiers & JModifier.PUBLIC) == 0) ||
         ((modifiers & JModifier.FINAL) != 0)
      }
      .map(_.getName)
      .toSet

    val publicNonFinalMethods = ClassUtil
      .nonFinalPublicMethods(cls)
      .map(_.getName)
      .toSet

    (nonPublicOrFinalMethods & publicNonFinalMethods) shouldBe Set.empty[String]
  }
}
