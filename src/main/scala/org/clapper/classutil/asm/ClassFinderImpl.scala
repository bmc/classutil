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

   * Neither the names "clapper.org", "Grizzled ClassUtil", nor the names of
    its contributors may be used to endorse or promote products derived
    from this software without specific prior written permission.

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

/** The ASM-specific implementation of the Class Finder capabilities.
  */
package org.clapper.classutil.asm

import org.clapper.classutil._

import scala.collection.mutable.{Set => MutableSet}
import scala.collection.mutable.{HashMap, HashSet}

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.{File, InputStream, IOException}


private[classutil] object ASMBitmapMapper {
  import java.lang.reflect.{Modifier => JModifier}

  val AccessMap = HashMap(
    Opcodes.ACC_ABSTRACT     -> Modifier.Abstract,
    Opcodes.ACC_FINAL        -> Modifier.Final,
    Opcodes.ACC_INTERFACE    -> Modifier.Interface,
    Opcodes.ACC_NATIVE       -> Modifier.Native,
    Opcodes.ACC_PRIVATE      -> Modifier.Private,
    Opcodes.ACC_PROTECTED    -> Modifier.Protected,
    Opcodes.ACC_PUBLIC       -> Modifier.Public,
    Opcodes.ACC_STATIC       -> Modifier.Static,
    Opcodes.ACC_STRICT       -> Modifier.Strict,
    Opcodes.ACC_SYNCHRONIZED -> Modifier.Synchronized,
    Opcodes.ACC_TRANSIENT    -> Modifier.Transient,
    Opcodes.ACC_VOLATILE     -> Modifier.Volatile
  )
}

private[classutil] trait ASMBitmapMapper {
  def mapModifiers(bitmap: Int, map: HashMap[Int, Modifier.Modifier]):
  Set[Modifier.Modifier] = {
    // Map the class's modifiers integer bitmap into a set of Modifier
    // enumeration values by filtering and keeping only the ones that
    // match the masks, extracting the corresponding map value, and
    // converting the whole thing to a set.
    //
    // Mutable collections are used for speed.
    val result = MutableSet[Modifier.Modifier]()
    for(pair <- map) {
      if((pair._1 & bitmap) != 0)
	result += pair._2
    }
    result.toSet
  }
}

private[classutil] class ClassInfoImpl(val name: String,
                                       val superClassName: String,
                                       val interfaces: List[String],
                                       val signature: String,
                                       val access: Int,
                                       val location: File)
extends ClassInfo with ASMBitmapMapper {
  import java.lang.reflect.{Modifier => JModifier}

  override def toString = name

  def methods = Set.empty[MethodInfo] ++ methodSet
  def fields  = Set.empty[FieldInfo] ++ fieldSet

  var methodSet = MutableSet.empty[MethodInfo]
  var fieldSet = MutableSet.empty[FieldInfo]
  val modifiers = mapModifiers(access, ASMBitmapMapper.AccessMap)
}

private[classutil] class MethodInfoImpl(val name: String,
                                        val signature: String,
                                        val exceptions: List[String],
                                        val access: Int)
extends MethodInfo with ASMBitmapMapper {
  val modifiers = mapModifiers(access, ASMBitmapMapper.AccessMap)
}

private[classutil] class FieldInfoImpl(val name: String,
                                       val signature: String,
                                       val access: Int)
extends FieldInfo with ASMBitmapMapper {
  override def toString = signature
  override def hashCode = signature.hashCode

  override def equals(o: Any) = o match {
    case m: FieldInfo => m.signature == signature
    case _            => false
  }

  val modifiers = mapModifiers(access, ASMBitmapMapper.AccessMap)
}

private[classutil] class ClassVisitor(location: File)
extends EmptyVisitor with ASMBitmapMapper {

  var classes = MutableSet.empty[ClassInfo]
  var currentClass: Option[ClassInfoImpl] = None

  override def visit(version: Int, 
                     access: Int, 
                     name: String,
                     signature: String, 
                     superName: String,
                     interfaces: Array[String]) {
    val classInfo = new ClassInfoImpl(mapClassName(name),
                                      mapClassName(superName),
                                      interfaces.toList.map(mapClassName(_)),
                                      signature,
                                      access,
                                      location)
    classes += classInfo
    currentClass = Some(classInfo)
  }

  override def visitMethod(access: Int,
                           name: String,
                           description: String,
                           signature: String,
                           exceptions: Array[String]): MethodVisitor = {
    assert(currentClass != None)
    val sig = if (signature != null) signature else name
    val excList = if (exceptions == null) Nil else exceptions.toList
    currentClass.get.methodSet += new MethodInfoImpl(name, sig, excList, 
                                                     access)
    null
  }

  override def visitField(access: Int,
                          name: String,
                          description: String,
                          signature: String,
                          value: java.lang.Object): FieldVisitor = {
    assert(currentClass != None)
    val sig = if (signature != null) signature else name
    currentClass.get.fieldSet += new FieldInfoImpl(name, sig, access)
    null
  }

  private def mapClassName(name: String): String = {
    if (name == null) ""
    else name.replaceAll("/", ".")
  }
}

private[classutil] object ClassFile {
  val ASMAcceptCriteria = 0

  def load(is: InputStream, location: File): Iterator[ClassInfo] = {
    val cr = new ClassReader(is)
    val visitor = new ClassVisitor(location)
    cr.accept(visitor, ASMAcceptCriteria)
    visitor.classes.toIterator
  }
}
