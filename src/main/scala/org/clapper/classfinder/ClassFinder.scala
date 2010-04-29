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

import grizzled.slf4j.Logger

import java.io.IOException

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.util.jar.JarFile;
import java.util.zip.ZipFile;
import java.io.File

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

trait MethodInfo
{
    val name: String
    val signature: String
    val exceptions: List[String]
    val access: Set[Modifier]
}

trait FieldInfo
{
    val name: String
    val signature: String
    val access: Set[Modifier]
}

trait ClassInfo
{
    def name: String
    def superClassName: String
    def interfaces: List[String]
    def signature: String
    def modifiers: Set[Modifier]
    def location: File
    def methods: Set[MethodInfo]
    def fields: Set[FieldInfo]
}

class ClassFinder(path: List[String])
{
    def classes: Iterator[ClassInfo]

    
}

object ClassFinder
{
    import java.io.File

    private val log = Logger("org.clapper.classfinder.ClassFinder")

    def classpath = 
        System.getProperty("java.class.path").
        split(File.pathSeparator).
        map(s => if (s.trim.length == 0) "." else s)

    def find(path: List[File]) =
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

    private def handle(f: File)
    {
        if (name.endsWith(".jar"))
            processJarOrZip(f, new JarFile(f))
        else if (name.endsWith(".zip"))
            processZip(f, new ZipFile(f))
        else if (f.isDirectory)
            processDirectory(f)
    }

    private def processJarOrZip(file: File, open: File => ZipFile)
    {
        try
        {
            val opened = open(file)
            try
            {
                processOpenZip(file.getPath, opened)
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
        }
    }

    private def processOpenZip(file: File, zipFile: ZipFile)
    {
    }

    private def processDirectory(dir: File)
    {
    }
}

private[classfinder] class ClassInfoImpl(val name: String,
                                         val superClassName: String,
                                         val interfaces: List[String],
                                         val signature: String,
                                         access: Int,
                                         val location: File)
extends ClassInfo
{
    import java.lang.reflect.Modifier

    private val ModifierMap = new Map(
        Modifier.ABSTRACT     -> Modifier.Abstract,
        Modifier.FINAL        -> Modifier.Final,
        Modifier.INTERFACE    -> Modifier.Interface,
        Modifier.NATIVE       -> Modifier.Native,
        Modifier.PRIVATE      -> Modifier.Private,
        Modifier.PROTECTED    -> Modifier.Protected,
        Modifier.PUBLIC       -> Modifier.Public,
        Modifier.STATIC       -> Modifier.Static,
        Modifier.STRICT       -> Modifier.Strict,
        Modifier.SYNCHRONIZED -> Modifier.Synchronized,
        Modifier.TRANSIENT    -> Modifier.Transient,
        Modifier.VOLATILE     -> Modifier.Volatile
    )

    val modifiers = ModifierMap.filter(t => (t._1 & access) != 0).map(_._2)

    val methods = Set.empty[MethodInfo]
    val fields = Set.empty[FieldInfo]
}

private[classfinder] class ClassVisitor(location: File) extends EmptyVisitor
{
    import scala.collection.mutable.{Map => MutableMap}

    val classes = MutableMap.empty[String, ClassInfo]

    override def visit(version: Int, 
                       access: Int, 
                       name: String,
                       signature: String, 
                       superName: String,
                       interfaces: Array[String])
    {
        classes = name -> new ClassInfoImpl(name,
                                            superName,
                                            interfaces.toList,
                                            signature,
                                            access,
                                            location)
    }
}
