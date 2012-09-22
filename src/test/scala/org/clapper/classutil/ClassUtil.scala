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

class ClassUtilTest extends FunSuite {
  test("isPrimitive, Scala values") {
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
      expect(expected, "isPrimitive(" + valueClass + ")=" + expected) {
        ClassUtil.isPrimitive(valueClass)
      }
    }
  }

  test("isPrimitive, Scala classes") {
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

    for ((cls, expected) <- data) {
      expect(expected, "isPrimitive(" + cls + ")=" + expected) {
        ClassUtil.isPrimitive(cls)
      }
    }
  }

  test("isPrimitive, Java classes") {
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
      expect(expected, "isPrimitive(" + valueClass + ")=" + expected) {
        ClassUtil.isPrimitive(valueClass)
      }
    }
  }

  test("classSignature") {
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

    val PrimitivesToTest = Primitives.map {
      cls => (cls, ClassUtil.PrimitiveSigMap(cls.getName))
    }.toSeq

    val ClassesToTest = Seq[(Class[_], String)](
      (getClass,        "Lorg/clapper/classutil/ClassUtilTest;"),
      (classOf[String], "Ljava/lang/String;")
    )

    for ((cls, signature) <- PrimitivesToTest) {
      expect(signature, cls.getCanonicalName + " -> " + signature) {
        ClassUtil.classSignature(cls)
      }
    }

    for ((cls: Class[_], signature: String) <- ClassesToTest) {
      expect(signature, cls.getCanonicalName + " -> " + signature) {
        ClassUtil.classSignature(cls)
      }
    }
  }

  test("methodSignature") {
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
      m.getName + "(" + 
      m.getParameterTypes.map(c => c.getCanonicalName).mkString(", ") +
      ")"
    }

    for ((method, signature) <- data) {
      expect(signature, methodToString(method) + " -> " + signature) {
        ClassUtil.methodSignature(method)
      }
    }
  }
}
