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

package org.clapper.classutil

import scala.collection.mutable.{Set => MutableSet}
import scala.util.continuations.cps

import grizzled.generator._

import java.util.jar.{JarFile, Manifest => JarManifest}
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

trait MethodInfo
{
    val name: String
    val signature: String
    val exceptions: List[String]
    val modifiers: Set[Modifier.Modifier]

    override def toString = signature
    override def hashCode = signature.hashCode

    override def equals(o: Any) = o match
    {
        case m: MethodInfo => m.signature == signature
        case _             => false
    }
}

trait FieldInfo
{
    val name: String
    val signature: String
    val modifiers: Set[Modifier.Modifier]

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

    def isInterface = modifiers contains Modifier.Interface
    def isAbstract = modifiers contains Modifier.Abstract
    def isPrivate = modifiers contains  Modifier.Private
    def isProtected = modifiers contains  Modifier.Protected
    def isPublic = modifiers contains  Modifier.Public
    def isFinal = modifiers contains  Modifier.Final
    def isStatic = modifiers contains  Modifier.Static
    def isSynchronized = modifiers contains  Modifier.Synchronized

    /**
     * Determine whether this class directly implements a specific interface.
     * Since a `ClassInfo` object contains information about a single class,
     * this method cannot determine whether a class indirectly implements
     * an interface. That capability is a higher-order operation.
     *
     * @param interface the name of the interface
     *
     * @return whether the class implements the interface
     */
    def implements(interface: String) = interfaces contains interface
}

object ClassFinder
{
    import java.io.File
    import grizzled.slf4j._

    private val log = Logger("org.clapper.classutil.ClassFinder")

    def classpath = 
        System.getProperty("java.class.path").
        split(File.pathSeparator).
        map(s => if (s.trim.length == 0) "." else s)

    def find(path: List[File]): Iterator[ClassInfo] =
    {
        path match
        {
            case Nil =>
                Iterator.empty

            case item :: Nil =>
                handle(item)

            case item :: tail =>
                handle(item) ++ find(tail)
        }
    }

    private def handle(f: File): Iterator[ClassInfo] =
    {
        val name = f.getPath.toLowerCase

        if (name.endsWith(".jar"))
            processJar(f)
        else if (name.endsWith(".zip"))
            processZip(f)
        else if (f.isDirectory)
            processDirectory(f)
        else
            Iterator.empty
    }

    private def processJar(file: File): Iterator[ClassInfo] =
    {
        val jar = new JarFile(file)
        val list1 = processOpenZip(file, jar)

        var manifest = jar.getManifest
        if (manifest == null)
            list1

        else
        {
            val path = loadManifestPath(jar, file, manifest)
            val list2 = find(path)
            list1 ++ list2
        }
    }

    private def loadManifestPath(jar: JarFile,
                                 jarFile: File,
                                 manifest: JarManifest): List[File] =
    {
        import scala.collection.JavaConversions._

        val attrs = manifest.getMainAttributes
        val value = attrs.get("Class-Path").asInstanceOf[String]

        if (value == null)
            Nil

        else
        {
            log.debug("Adding ClassPath from jar " + jar.getName)
            val parent = jarFile.getParent
            val tokens = value.split("""\s+""").toList

            if (parent == null)
                tokens.map(new File(_))
            else
                tokens.map(s => new File(parent + File.separator + s))
        }
    }

    private def processZip(file: File): Iterator[ClassInfo] =
        processOpenZip(file, new ZipFile(file))

    private def processOpenZip(file: File, zipFile: ZipFile) =
    {
        import scala.collection.JavaConversions._

        val zipFileName = file.getPath
        val classInfoIterators =
            zipFile.entries.
            filter((e: ZipEntry) => isClass(e)).
            map((e: ZipEntry) => classData(zipFile.getInputStream(e), file))

        generateFromIterators(classInfoIterators)
    }

    // Matches both ZipEntry and File
    type FileEntry =
    {
        def isDirectory(): Boolean
        def getName(): String
    }

    private def isClass(e: FileEntry): Boolean =
        (! e.isDirectory) && (e.getName.toLowerCase.endsWith(".class"))

    private def processDirectory(dir: File): Iterator[ClassInfo] =
    {
        import grizzled.file.implicits._
        import java.io.FileInputStream

        val classInfoIterators =
            dir.listRecursively.
            filter((f: File) => isClass(f)).
            map((f: File) => classData(new FileInputStream(f), dir))

        generateFromIterators(classInfoIterators)
    }

    private def classData(is: InputStream, 
                          location: File): Iterator[ClassInfo] =
    {
        import org.clapper.classutil.asm.ClassFile

        ClassFile.load(is, location)
    }

    private def generateFromIterators(iterators: Iterator[Iterator[ClassInfo]])=
    generator[ClassInfo]
    {
        def doIterator(iterator: Iterator[ClassInfo]):
        Unit @cps[Iteration[ClassInfo]] =
        {
            if (iterator.hasNext)
            {
                generate(iterator.next)
                doIterator(iterator)
            }
        }

        def doIterators(iterators: Iterator[Iterator[ClassInfo]]):
        Unit @cps[Iteration[ClassInfo]] =
        {
            if (iterators.hasNext)
            {
                doIterator(iterators.next)
                doIterators(iterators)
            }
        }

        doIterators(iterators)
    }
}
