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

import org.scalatest.{FlatSpec, Matchers}

import java.io.File
import grizzled.file.Implicits._
import grizzled.file.{util => fileutil}

class ClassFinderSpec extends FlatSpec with Matchers {
  "runtimeClassFinder.getClasses" should "find classes in a specified class path" in {

    // The number of returned classInfo objects should be the same number
    // as the number of class files.
    // Ensure that we got some.
    val totalExpected = runtimeClassFiles.length
    totalExpected should be > 0

    // First, verify that we get the right number if we convert the stream
    // directly to a collection.
    val classInfoList = runtimeClassFinder.getClasses.toVector
    classInfoList.size shouldBe runtimeClassFiles.size

    // Next, check the stream by looping over it.
    val classInfoStream = runtimeClassFinder.getClasses
    classInfoStream.size shouldBe totalExpected
  }

  "concreteSubclasses" should "find indirect implementations a trait/interface" in {
    val classes = runtimeClassFinder.getClasses
    val cs = ClassFinder.concreteSubclasses(classOf[BaseInfo], classes)

    val csColl = cs.toVector
    csColl.size should be > 0
    val mustBePresent = classOf[org.clapper.classutil.asm.ClassInfoImpl]
    csColl.map { _.name } should contain (mustBePresent.getName)
  }

  it should "find direct implementations of a trait/interface" in {
    val classes = runtimeClassFinder.getClasses
    val cs = ClassFinder.concreteSubclasses(classOf[ClassInfo], classes)

    val csColl = cs.toVector
    csColl.size should be > 0
    val mustBePresent = classOf[org.clapper.classutil.asm.ClassInfoImpl]
    csColl.map { _.name } should contain (mustBePresent.getName)
  }

  it should "find direct subclasses of a parent class" in {
    abstract class A
    class B extends A

    val classes = runtimeClassFinder.getClasses
    val cs = ClassFinder.concreteSubclasses(classOf[A], classes).toVector
    cs.size should be > 0
    cs.map { _.name } should contain (classOf[B].getName)
  }

  it should "find indirect subclasses of a parent class" in {
    abstract class A
    class B extends A
    class C extends B

    val classes = runtimeClassFinder.getClasses
    val cs = ClassFinder.concreteSubclasses(classOf[A], classes).toVector
    cs.size should be > 0
    cs.map { _.name } should contain (classOf[C].getName)
  }

  private val (runtimeClassFiles, runtimeClassFinder) = {
    import scala.util.Properties
    val version = Properties.releaseVersion.get
    val shortVersion = version.split("""\.""").take(2).mkString(".")

    val targetDirectory: Option[File] = Array(
      fileutil.joinPath("target", s"scala-$version"),
      fileutil.joinPath("target", s"scala-$shortVersion")
    )
    .map(new File(_))
    .find(_.exists)

    assert(targetDirectory.isDefined)
    val dir = targetDirectory.get

    println(s"dir=$dir, exists=${dir.exists}")

    // SBT-dependent paths
    val classDir = new File(fileutil.joinPath(dir.getPath, "classes"))
    val testClassDir = new File(fileutil.joinPath(dir.getPath, "test-classes"))

    // Get class files under the directory.
    val classFiles = classDir.listRecursively()
                             .filter(_.getName.endsWith(".class"))
                             .toVector
    val testClassFiles = testClassDir.listRecursively()
                                     .filter(_.getName.endsWith(".class"))
                                     .toVector

    // The number of returned classInfo objects should be the same number
    // as the number of class files.
    val allClassFiles = classFiles ++ testClassFiles
    val finder = ClassFinder(Seq(classDir, testClassDir))
    assert(finder.getClasses.size == allClassFiles.length)
    (allClassFiles, finder)
  }
}
