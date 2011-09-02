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

import java.io.File
import grizzled.file.GrizzledFile._
import scala.annotation.tailrec

class ClassFinderTest extends FunSuite {
  test("find classes in specified class path") {
    val directory = new File("target")
    assert(directory.exists)
    val scalaDirs = directory.listFiles.filter(_.getName.startsWith("scala"))
    assert(scalaDirs.length > 0)

    // Even though the take() returns an Array[File], we must use apply(0) to
    // get the first element, not just (0), because there's an implicit
    // parameter in the way...
    val classDir = scalaDirs.take(1).map(new File(_, "classes")).apply(0)

    // Get class files under the directory.
    val classFiles = classDir.listRecursively().
                              filter(_.getName.endsWith(".class")).toList

    // Ensure that we got some.
    val totalExpected = classFiles.length
    assert(totalExpected > 0)

    // The number of returned classInfo objects should be the same number
    // as the number of class files.
    val classFinder = ClassFinder(Seq(classDir))

    // First, verify that we get the right number if we convert the iterator
    // directly to a list.
    val classInfoList = classFinder.getClasses.toList
    assert(classInfoList.length === classFiles.length)

    // Next, check the iterator by looping over it.
    val classInfoIterator = classFinder.getClasses
    assert(countIterator(classInfoIterator) === totalExpected)
  }

  private def countIterator(classInfos: Iterator[ClassInfo]): Int = {
    @tailrec def countNext(cur: Int, i: Iterator[ClassInfo]): Int = {
      if (i.hasNext) {
        i.next
        countNext(cur + 1, i)
      }
      else
        cur
    }

    countNext(0, classInfos)
  }
}
