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

package org.clapper.classfinder

import scala.collection.mutable.{Set => MutableSet}

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.util.jar.JarFile
import java.util.zip.{ZipFile, ZipEntry}
import java.io.{File, InputStream, IOException}

object Modifier extends Enumeration
{
    type Modifier = Value

    val Abstract     = Value("abstract")
    val Final        = Value("final")
    val Interface    = Value("interface")
    val Native       = Value("native")
    val Private      = Value("private")
    val Protected    = Value("protected")
    val Public       = Value("public")
    val Static       = Value("static")
    val Strict       = Value("strict")
    val Synchronized = Value("synchronized")
    val Transient    = Value("transient")
    val Volatile     = Value("volatile")
}

class MethodInfo(val name: String,
                 val signature: String,
                 val exceptions: List[String],
                 val access: Set[Modifier.Modifier])
{
    override def toString = signature
    override def hashCode = signature.hashCode

    override def equals(o: Any) = o match
    {
        case m: MethodInfo => m.signature == signature
        case _             => false
    }
}

class FieldInfo(val name: String,
                val signature: String,
                val access: Set[Modifier.Modifier])
{
    override def toString = signature
    override def hashCode = signature.hashCode

    override def equals(o: Any) = o match
    {
        case m: FieldInfo => m.signature == signature
        case _            => false
    }
}

trait ClassInfo
{
    def name: String
    def superClassName: String
    def interfaces: List[String]
    def signature: String
    def modifiers: Set[Modifier.Modifier]
    def location: File
    def methods: Set[MethodInfo]
    def fields: Set[FieldInfo]
}

object ClassFinder
{
    import java.io.File
    import grizzled.slf4j._

    private val log = Logger("org.clapper.classfinder.ClassFinder")

    def classpath = 
        System.getProperty("java.class.path").
        split(File.pathSeparator).
        map(s => if (s.trim.length == 0) "." else s)

    def find(path: List[File]): List[ClassInfo] =
    {
        path match
        {
            case Nil =>
                Nil

            case item :: Nil =>
                handle(item)

            case item :: tail =>
                handle(item) ::: find(tail)
        }
    }

    private def handle(f: File): List[ClassInfo] =
    {
        val name = f.getPath

        if (name.endsWith(".jar"))
            processJarOrZip(f, new JarFile(f))
        else if (name.endsWith(".zip"))
            processJarOrZip(f, new ZipFile(f))
        else if (f.isDirectory)
            processDirectory(f)
        else
            Nil
    }

    private def processJarOrZip(file: File, open: => ZipFile): List[ClassInfo] =
    {
        try
        {
            val opened = open
            try
            {
                processOpenZip(file, opened)
            }
            finally
            {
                opened.close
            }
        }

        catch
        {
            case e: IOException =>
                log.error("Cannot open file \"" + file.getPath + "\"", e)
                Nil
        }
    }

    private def processOpenZip(file: File, zipFile: ZipFile): List[ClassInfo] =
    {
        import scala.collection.JavaConversions._

        val zipFileName = file.getPath
        zipFile.entries.
            filter((e: ZipEntry) => isClass(e)).
            map((e: ZipEntry) => classData(zipFile.getInputStream(e), file)).
            toList.
            flatten
    }

    private def isClass(e: ZipEntry): Boolean =
        (! e.isDirectory) && (e.getName.toLowerCase.endsWith(".class"))

    private def processDirectory(dir: File): List[ClassInfo] =
    {
        Nil
    }

    private def classData(is: InputStream, location: File): List[ClassInfo] =
    {
        val cr = new ClassReader(is)
        val ASMAcceptCriteria = 0
        val visitor = new ClassVisitor(location)
        cr.accept(visitor, ASMAcceptCriteria)
        visitor.classes.toList
    }
}

private[classfinder] class ClassInfoImpl(val name: String,
                                         val superClassName: String,
                                         val interfaces: List[String],
                                         val signature: String,
                                         val modifiers: Set[Modifier.Modifier],
                                         val location: File)
extends ClassInfo
{
    import java.lang.reflect.{Modifier => JModifier}

    override def toString = name

    def methods = Set.empty[MethodInfo] ++ methodSet
    def fields  = Set.empty[FieldInfo] ++ fieldSet

    var methodSet = MutableSet.empty[MethodInfo]
    var fieldSet = MutableSet.empty[FieldInfo]
}

private[classfinder] class ClassVisitor(location: File) extends EmptyVisitor
{
    import java.lang.reflect.{Modifier => JModifier}

    private val ModifierMap = Map(
        JModifier.ABSTRACT     -> Modifier.Abstract,
        JModifier.FINAL        -> Modifier.Final,
        JModifier.INTERFACE    -> Modifier.Interface,
        JModifier.NATIVE       -> Modifier.Native,
        JModifier.PRIVATE      -> Modifier.Private,
        JModifier.PROTECTED    -> Modifier.Protected,
        JModifier.PUBLIC       -> Modifier.Public,
        JModifier.STATIC       -> Modifier.Static,
        JModifier.STRICT       -> Modifier.Strict,
        JModifier.SYNCHRONIZED -> Modifier.Synchronized,
        JModifier.TRANSIENT    -> Modifier.Transient,
        JModifier.VOLATILE     -> Modifier.Volatile
    )

    private val AccessMap = Map(
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

    var classes = MutableSet.empty[ClassInfo]
    var currentClass: Option[ClassInfoImpl] = None

    override def visit(version: Int, 
                       access: Int, 
                       name: String,
                       signature: String, 
                       superName: String,
                       interfaces: Array[String])
    {
        val classInfo = new ClassInfoImpl(
            mapClassName(name),
            mapClassName(superName),
            interfaces.toList.map(mapClassName(_)),
            signature,
            mapModifiers(access, AccessMap),
            location
        )
        classes += classInfo
        currentClass = Some(classInfo)
    }

    override def visitMethod(access: Int,
                             name: String,
                             description: String,
                             signature: String,
                             exceptions: Array[String]): MethodVisitor =
    {
        assert(currentClass != None)
        val sig = if (signature != null) signature else name
        val excList = if (exceptions == null) Nil else exceptions.toList
        currentClass.get.methodSet += new MethodInfo(
            name, sig, excList, mapModifiers(access, ModifierMap)
        )
        null
    }

    override def visitField(access: Int,
                            name: String,
                            description: String,
                            signature: String,
                            value: java.lang.Object): FieldVisitor =
    {
        assert(currentClass != None)
        val sig = if (signature != null) signature else name
        currentClass.get.fieldSet += new FieldInfo(
            name, sig, mapModifiers(access, ModifierMap)
        )
        null
    }

    private def mapModifiers(bitmap: Int, 
                             map: Map[Int, Modifier.Modifier]):
        Set[Modifier.Modifier] =
    {
        // Map the class's modifiers integer bitmap into a set of Modifier
        // enumeration values by filtering and keeping only the ones that
        // match the masks, extracting the corresponding map value, and
        // converting the whole thing to a set.
        map.filterKeys(k => (k & bitmap) != 0).values.toSet
    }

    private def mapClassName(name: String): String = name.replaceAll("/", ".")
}
