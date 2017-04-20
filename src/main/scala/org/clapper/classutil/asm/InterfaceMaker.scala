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

package org.clapper.classutil.asm

import org.clapper.classutil._

import org.objectweb.asm.{ClassWriter, Type}
import org.objectweb.asm.Opcodes._

/** Uses ASM to create an interface from a map. The map is keyed by method
  * names, each of which maps to a return type.
  */
private[classutil] object InterfaceMaker {
  /** Convenience constant for "no parameters"
    */
  final val NoParams = List[Class[_]]().toArray

  // ----------------------------------------------------------------------
  // Public Methods
  // ----------------------------------------------------------------------

  /** Transform a sequence of (method-name, param-types, return-type)
    * tuples into an interface of methods. An empty param-types array
    * generates a method without parameters.
    *
    * @param methods   a sequence of (method-name, param-types, return-type)
    *                  tuples representing the methods to generate
    * @param className the name to give the interface
    *
    * @return an array of bytes representing the compiled interface
    */
  def makeInterface(methods: Seq[(String, Array[Class[_]], Class[_])],
                    className: String): Array[Byte] = {
    val cw = new ClassWriter(0)
    cw.visit(V1_6,
             ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
             ClassUtil.binaryClassName(className),
             null,
             "java/lang/Object",
             null)

    for ((methodName, paramClasses, returnClass) <- methods) {
      val asmType = Type.getType(returnClass)
      val returnType = asmType.getDescriptor

      cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT,
                     methodName,
                     ClassUtil.methodSignature(returnClass, paramClasses),
                     null,
                     null).
      visitEnd()
    }

    cw.visitEnd()
    cw.toByteArray
  }

  // ----------------------------------------------------------------------
  // Private Methods
  // ----------------------------------------------------------------------
}
