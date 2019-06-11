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
  val NoParams = List[Class[_]]().toArray

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
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
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
