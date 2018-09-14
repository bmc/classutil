/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010-2018, Brian M. Clapper
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

import scala.collection.mutable.{Set => MutableSet, ArrayBuilder}

import org.objectweb.asm._

import java.io.{File, InputStream}

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
private[classutil] object ASMBitmapMapper {
  import java.lang.reflect.{Modifier => JModifier}

  val AccessMap = Map(
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
    Opcodes.ACC_SYNTHETIC    -> Modifier.Synthetic,
    Opcodes.ACC_TRANSIENT    -> Modifier.Transient,
    Opcodes.ACC_VOLATILE     -> Modifier.Volatile
  )
}

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
private[classutil] trait ASMBitmapMapper {
  def mapModifiers(bitmap: Int, map: Map[Int, Modifier.Modifier]):
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

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures",
                        "org.wartremover.warts.Var"))
private[classutil] class ClassInfoImpl(val name: String,
                                       val superClassName: String,
                                       val interfaces: List[String],
                                       val signature: String,
                                       val access: Int,
                                       val location: File)
extends ClassInfo with ASMBitmapMapper {
  def methods     = Set.empty[MethodInfo] ++ methodSet
  def fields      = Set.empty[FieldInfo] ++ fieldSet
  def annotations = Set.empty[AnnotationInfo] ++ annotationSet

  var methodSet = MutableSet.empty[MethodInfo]
  var fieldSet = MutableSet.empty[FieldInfo]
  var annotationSet = MutableSet.empty[AnnotationInfo]
  val modifiers = mapModifiers(access, ASMBitmapMapper.AccessMap)
}

private[classutil] class MethodInfoImpl(val name: String,
                                        val signature: String,
                                        val descriptor: String,
                                        val exceptions: List[String],
                                        val access: Int)
extends MethodInfo with ASMBitmapMapper {
  val modifiers = mapModifiers(access, ASMBitmapMapper.AccessMap)
}

private[classutil] class FieldInfoImpl(val name: String,
                                       val signature: String,
                                       val descriptor: String,
                                       val value: Option[java.lang.Object],
                                       val access: Int)
extends FieldInfo with ASMBitmapMapper {
  val modifiers = mapModifiers(access, ASMBitmapMapper.AccessMap)
}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
private[classutil] class AnnotationInfoImpl(val descriptor: String,
                                            val visible: Boolean)
extends AnnotationInfo {
  def params = Map.empty[String, Any] ++ paramMap

  var paramMap = Map.empty[String, Any]
}

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures",
                        "org.wartremover.warts.Var"))
private[classutil] class ClassVisitor(location: File, apiVersion: Int)
extends ASMEmptyVisitor(apiVersion) with ASMBitmapMapper {

  var classes = MutableSet.empty[ClassInfo]
  var currentClass: Option[ClassInfoImpl] = None

  override def visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String,
                     superName: String,
                     interfaces: Array[String]): Unit = {
    val sig = if (signature != null) signature else ""
    val classInfo = new ClassInfoImpl(mapClassName(name),
                                      mapClassName(superName),
                                      interfaces.toList.map(mapClassName(_)),
                                      sig,
                                      access,
                                      location)
    classes += classInfo
    currentClass = Some(classInfo)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  override def visitMethod(access: Int,
                           name: String,
                           descriptor: String,
                           signature: String,
                           exceptions: Array[String]): MethodVisitor = {
    assert(currentClass.isDefined)
    val sig = if (signature != null) signature else ""
    val excList = Option(exceptions).map(_.toList).getOrElse(Nil)
    currentClass.foreach { c =>
      c.methodSet += new MethodInfoImpl(name, sig, descriptor, excList, access)
    }

    null
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  override def visitField(access: Int,
                          name: String,
                          descriptor: String,
                          signature: String,
                          value: java.lang.Object): FieldVisitor = {
    assert(currentClass.isDefined)
    val sig = if (signature != null) signature else ""
    val initialVal = Option(value)
    currentClass.foreach { c =>
      c.fieldSet += new FieldInfoImpl(name, sig, descriptor, initialVal, access)
    }

    null
  }

  class AnnotationVisitor(annotationInfo: AnnotationInfoImpl)
    extends org.objectweb.asm.AnnotationVisitor(api) {

    override def visit(name: String, value: Any): Unit =
      annotationInfo.paramMap += (name -> value)

    override def visitEnum(name: String, desc: String, value: String): Unit =
      annotationInfo.paramMap += (name -> value)

    override def visitAnnotation(name: String, desc: String): AnnotationVisitor = {
      val innerAnnInfo = new AnnotationInfoImpl(desc, annotationInfo.visible)
      annotationInfo.paramMap += (name -> innerAnnInfo)
      new AnnotationVisitor(innerAnnInfo)
    }

    override def visitArray(name: String): AnnotationArrayVisitor = {
      new AnnotationArrayVisitor {
        override def visitEnd(): Unit = {
          annotationInfo.paramMap += (name -> arrBuilder.result)
        }
      }
    }
  }

  class AnnotationArrayVisitor extends org.objectweb.asm.AnnotationVisitor(api) {
    protected val arrBuilder: ArrayBuilder[Any] = ArrayBuilder.make[Any]

    override def visit(name: String, value: Any): Unit = arrBuilder += value

    override def visitEnum(name: String, desc: String, value: String): Unit = {
      arrBuilder += value
    }

    override def visitAnnotation(name: String, desc: String) = {
      val innerAnnInfo = new AnnotationInfoImpl(desc, false)
      arrBuilder += innerAnnInfo
      new AnnotationVisitor(innerAnnInfo)
    }

    override def visitArray(name: String) = {
      val outer = this
      new AnnotationArrayVisitor {
        override def visitEnd(): Unit = outer.arrBuilder += arrBuilder.result
      }
    }
  }

  override def visitAnnotation(descriptor: String,
                               visible: Boolean): AnnotationVisitor = {
    assert(currentClass.isDefined)
    val annotationInfo = new AnnotationInfoImpl(descriptor, visible)
    currentClass.foreach { c => c.annotationSet += annotationInfo }

    new AnnotationVisitor(annotationInfo)
  }

  private def mapClassName(name: String): String = {
    if (name == null) ""
    else name.replaceAll("/", ".")
  }
}

private[classutil] object ClassFile {
  val ASMAcceptCriteria = 0

  def load(is: InputStream, location: File, asmVersion: Int): Iterator[ClassInfo] = {
    val cr = new ClassReader(is)
    val visitor = new ClassVisitor(location, asmVersion)
    cr.accept(visitor, ASMAcceptCriteria)
    visitor.classes.toIterator
  }
}

