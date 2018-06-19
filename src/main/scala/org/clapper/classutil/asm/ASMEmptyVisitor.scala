package org.clapper.classutil.asm

import org.objectweb.asm._

private[classutil] class ASMEmptyVisitor(apiVersion: Int)
  extends org.objectweb.asm.ClassVisitor(apiVersion) {

  import scala.language.existentials

  def this() = this(Opcodes.ASM6)

  val annotationVisitor =
    new AnnotationVisitor(apiVersion) {

      override def visitAnnotation(name: String, desc: String) = this

      override def visitArray(name: String) = this

      override def visit(name: String, value: scala.Any)= {}

      override def visitEnum(name: String, desc: String, value: String) = {}

      override def visitEnd()= {}
    }

  override def visit(version: Int, access: Int, name: String, signature: String, superName: String, interfaces: Array[String])= {}

  override def visitSource(source: String, debug: String)= {}

  override def visitOuterClass(owner: String, name: String, desc: String)= {}

  override def visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor = annotationVisitor

  override def visitAttribute(attr: Attribute)= {}

  override def visitInnerClass(name: String, outerName: String, innerName: String, accesss: Int)= {}

  override def visitEnd()= {}

  override def visitField(access: Int, name: String, desc: String, signature: String, value: Object): FieldVisitor =
    new FieldVisitor(apiVersion) {

      override def visitAnnotation(p1: String, p2: Boolean): AnnotationVisitor = annotationVisitor

      override def visitAttribute(p1: Attribute)= {}

      override def visitEnd()= {}
    }

  override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor =
    new MethodVisitor(apiVersion) {

      override def visitAnnotationDefault(): AnnotationVisitor = annotationVisitor

      override def visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor = annotationVisitor

      override def visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor = annotationVisitor

      override def visitAttribute(attr: Attribute)= {}

      override def visitCode()= {}

      override def visitFrame(theType: Int, nLocal: Int, local: Array[Object], nStack: Int, stack: Array[Object])= {}

      override def visitInsn(opcode: Int)= {}

      override def visitIntInsn(opcode: Int, operand: Int)= {}

      override def visitVarInsn(opcdode: Int, theVar: Int)= {}

      override def visitTypeInsn(opcode: Int, theType: String)= {}

      override def visitFieldInsn(opcode: Int, owner: String, name: String, desc: String)= {}

      override def visitMethodInsn(opcode: Int, owner: String, name: String, desc: String)= {}

      override def visitJumpInsn(opcode: Int, label: Label)= {}

      override def visitLabel(label: Label)= {}

      override def visitLdcInsn(cst: Object)= {}

      override def visitIincInsn(theVar: Int, increment: Int)= {}

      override def visitTableSwitchInsn(min: Int, max: Int, dflt: Label, labels: Label*)= {}

      override def visitLookupSwitchInsn(dflt: Label, keys: Array[Int], labels: Array[Label])= {}

      override def visitMultiANewArrayInsn(desc: String, dims: Int)= {}

      override def visitTryCatchBlock(start: Label, end: Label, handler: Label, theType: String)= {}

      override def visitLocalVariable(name: String, desc: String, signature: String, start: Label, end: Label, index: Int)= {}

      override def visitLineNumber(line: Int, start: Label)= {}

      override def visitMaxs(maxStack: Int, maxLocals: Int)= {}

      override def visitEnd()= {}
    }


}
