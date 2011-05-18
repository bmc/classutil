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

/**
 * This library provides methods for locating and filtering classes
 * quickly--faster, in fact, than can be done with Java or Scala runtime
 * reflection. Under the covers, it uses the ASM bytecode library, though
 * it can easily be extended to use other bytecode libraries. ClassUtil
 * loads and returns information about classes using an efficient lazy
 * iterator approach, which offers minimal startup penalty and the ability
 * to cut the traversal short.
 */
package org.clapper.classutil

import scala.collection.mutable.{Set => MutableSet}
import scala.util.continuations.cps
import scala.annotation.tailrec

import grizzled.slf4j._
import grizzled.generator._

import java.util.jar.{JarFile, Manifest => JarManifest}
import java.util.zip.{ZipFile, ZipEntry}
import java.io.{File, InputStream, IOException}

/**
 * An enumerated high-level view of the modifiers that can be attached
 * to a method, class or field.
 */
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

/**
 * Information about a method, as read from a class file.
 */
trait MethodInfo
{
    /**
     * The name of the method.
     */
    val name: String

    /**
     * The method's JVM signature.
     */
    val signature: String

    /**
     * A list of the checked exceptions (as class names) that the method
     * throws, or an empty list if it throws no known checked exceptions.
     */
    val exceptions: List[String]

    /**
     * The method's modifiers.
     */
    val modifiers: Set[Modifier.Modifier]

    /**
     * A printable version of the method. Currently, the string version is
     * the signature.
     */
    override def toString = signature

    override def hashCode = signature.hashCode

    override def equals(o: Any) = o match
    {
        case m: MethodInfo => m.signature == signature
        case _             => false
    }
}

/**
 * Information about a field, as read from a class file.
 */
trait FieldInfo
{
    /**
     * The field's name.
     */
    val name: String

    /**
     * The field's JVM signature.
     */
    val signature: String

    /**
     * The field's modifiers.
     */
    val modifiers: Set[Modifier.Modifier]

    /**
     * A printable version of the field. Currently, the string version is
     * the signature.
     */
    override def toString = signature

    override def hashCode = signature.hashCode

    override def equals(o: Any) = o match
    {
        case m: FieldInfo => m.signature == signature
        case _            => false
    }
}

/**
 * Information about a class, as read from a class file.
 */
trait ClassInfo
{
    /**
     * The class's fully qualified name.
     */
    def name: String

    /**
     * The parent class's fully qualified name.
     */
    def superClassName: String

    /**
     * A list of the interfaces, as class names, that the class implements;
     * or, an empty list if it implements no interfaces.
     */
    def interfaces: List[String]

    /**
     * The class's JVM signature.
     */
    def signature: String

    /**
     * The class's modifiers.
     */
    def modifiers: Set[Modifier.Modifier]

    /**
     * Where the class was found (directory, jar file, or zip file).
     */
    def location: File

    /**
     * A set of the methods in the class.
     */
    def methods: Set[MethodInfo]

    /**
     * A set of the fields in the class.
     */
    def fields: Set[FieldInfo]

    /**
     * Convenience method that determines whether the class implements an
     * interface. This method is just shorthand for:
     * {{{
     * modifiers contains Modifier.Interface
     * }}}
     */
    def isInterface = modifiers contains Modifier.Interface

    /**
     * Convenience method that determines whether the class is abstract
     * This method is just shorthand for:
     * {{{
     * modifiers contains Modifier.Abstract
     * }}}
     */
    def isAbstract = modifiers contains Modifier.Abstract

    /**
     * Convenience method that determines whether the class is private.
     * This method is just shorthand for:
     * {{{
     * modifiers contains Modifier.Private
     * }}}
     */
    def isPrivate = modifiers contains Modifier.Private

    /**
     * Convenience method that determines whether the class is protected.
     * This method is just shorthand for:
     * {{{
     * modifiers contains Modifier.Protected
     * }}}
     */
    def isProtected = modifiers contains Modifier.Protected

    /**
     * Convenience methods that determines whether the class is public.
     * This method is just shorthand for:
     * {{{
     * modifiers contains Modifier.Public
     * }}}
     */
    def isPublic = modifiers contains Modifier.Public

    /**
     * Convenience methods that determines whether the class is final.
     * This method is just shorthand for:
     * {{{
     * modifiers contains Modifier.Final
     * }}}
     */
    def isFinal = modifiers contains Modifier.Final

    /**
     * Convenience methods that determines whether the class is static.
     * This method is just shorthand for:
     * {{{
     * modifiers contains Modifier.Static
     * }}}
     */
    def isStatic = modifiers contains Modifier.Static

    /**
     * Convenience methods that determines whether the class is synchronized.
     * This method is just shorthand for:
     * {{{
     * modifiers contains Modifier.Synchronized
     * }}}
     */
    def isSynchronized = modifiers contains Modifier.Synchronized

    /**
     * Convenience method to determine whether the class is concrete (i.e.,
     * isn't abstract and isn't an interface).
     */
    def isConcrete = ! ( (modifiers contains Modifier.Abstract) ||
                         (modifiers contains Modifier.Interface) )
    /**
     * Convenience method to determine whether this class directly
     * implements a specific interface. Since a `ClassInfo` object contains
     * information about a single class, this method cannot determine
     * whether a class indirectly implements an interface. That capability
     * is a higher-order operation.
     *
     * @param interface the name of the interface
     *
     * @return whether the class implements the interface
     */
    def implements(interface: String) = interfaces contains interface
}

/**
 * A `ClassFinder` finds classes in a class path, returning the result in a
 * lazy iterator. The iterator can then be filtered, mapped, or passed to
 * the utility methods in the `ClassFinder` companion object.
 *
 * @param path  a sequence of directories, jars and zips to search
 */
class ClassFinder(path: Seq[File])
{
    val classpath = path.toList

    private val log = Logger(this.getClass)

    /**
     * Find all classes in the specified path, which can contain directories,
     * zip files and jar files. Returns metadata about each class in a
     * `ClassInfo` object. The `ClassInfo` objects are returned lazily,
     * rather than loaded all up-front.
     *
     * @param path  the class path
     *
     * @return an iterator over `ClassInfo` objects
     */
    def getClasses(): Iterator[ClassInfo] = find(classpath)

    /* ---------------------------------------------------------------------- *\
                              Private Methods
    \* ---------------------------------------------------------------------- */

    private def find(path: Seq[File]): Iterator[ClassInfo] =
    {
        path match
        {
            case Nil          => Iterator.empty
            case item :: Nil  => findClassesIn(item)
            case item :: tail => findClassesIn(item) ++ find(tail)
        }
    }

    private def findClassesIn(f: File): Iterator[ClassInfo] =
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
        import grizzled.file.GrizzledFile._
        import java.io.FileInputStream

        val classInfoIterators =
            dir.listRecursively().
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

    /**
     * Generate classes from an iterator of iterators.
     */
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

/**
 * The entrance to the factory floor, providing methods for finding and
 * filtering classes.
 */
object ClassFinder
{
    /**
     * Convenient method for getting the standard JVM classpath, into a
     * variable suitable for use with the `find()` method.
     *
     * @return the classpath, as a list of `File` objects
     */
    def classpath = 
        System.getProperty("java.class.path").
        split(File.pathSeparator).
        map(s => if (s.trim.length == 0) "." else s).
        map(new File(_)).
        toList

    /**
     * Instantiate a new `ClassFinder` that will search the specified
     * classpath, or the default classpath, if no classpath is defined.
     *
     * @param path  the classpath, which is a sequence of `File`
     *               objects representing directories, jars and zip files
     *               to search. Defaults to `classpath`.
     *
     * @return a new `ClassFinder` object
     */
    def apply(path: Seq[File] = Seq.empty[File]) =
        new ClassFinder(if (path.length > 0) path else classpath)

    /**
     * Create a map from an Iterator of ClassInfo objects. The resulting
     * map is indexed by class name.
     */
    def classInfoMap(iterator: Iterator[ClassInfo]): Map[String, ClassInfo] =
        iterator.map(c => (c.name -> c)).toMap

    /**
     * Convenience method that scans the specified classes for all concrete
     * classes that are subclasses of the named class. A subclass, in this
     * definition, is a class that directly or indirectly (a) implements an
     * interface (if the named class is an interface) or (b) extends a
     * subclass (if the named class is a class). The class must be
     * concrete, so intermediate abstract classes are not returned, though
     * any children of such abstract classes will be.
     *
     * '''WARNINGS'''
     *
     * This method converts the iterator to a map of classes, for easier
     * lookup. Thus, upon its return, the iterator will be empty. You can
     * certainly recreate the iterator, but at a cost. If you need to make
     * multiple calls to this method with the same classpath, consider
     * converting the iterator to a map first, as shown below:
     * {{{
     * val finder = ClassFinder(myPath)
     * val classes = finder.getClasses  // classes is an Iterator[ClassInfo]
     * val classMap = ClassFinder.classInfoMap // runs the iterator out, once
     * val foos = ClassFinder.concreteSubclasses("org.example.Foo", classMap)
     * val bars = ClassFinder.concreteSubclasses("org.example.Bar", classMap)
     * }}}
     *
     * This method can chew up a lot of temporary heap space, if called
     * with a large classpath.
     *
     * @param ancestor the name of the class for which to find descendent
     *                 concrete subclasses
     * @param classes  the iterator of `ClassInfo` objects to search
     * 
     * @return an iterator of `ClassInfo` objects that are concrete subclasses
     *         of `ancestor`. The iterator will be empty if no matching classes
     *         could be found.
     */
    def concreteSubclasses(ancestor: String, classes: Iterator[ClassInfo]):
        Iterator[ClassInfo] =
    {
        concreteSubclasses(ancestor, ClassFinder.classInfoMap(classes))
    }

    /**
     * Convenience method that scans the specified classes for all concrete
     * classes that are subclasses of the named class. A subclass, in this
     * definition, is a class that directly or indirectly (a) implements an
     * interface (if the named class is an interface) or (b) extends a
     * subclass (if the named class is a class). The class must be
     * concrete, so intermediate abstract classes are not returned, though
     * any children of such abstract classes will be.
     *
     * WARNING: This method can chew up a lot of temporary heap space, if
     * called with a large classpath.
     *
     * @param ancestor the name of the class for which to find descendent
     *                 concrete subclasses
     * @param classes  the iterator of `ClassInfo` objects to search
     * 
     * @return an iterator of `ClassInfo` objects that are concrete subclasses
     *         of `ancestor`. The iterator will be empty if no matching classes
     *         could be found.
     */
    def concreteSubclasses(ancestor: String, classes: Map[String, ClassInfo]):
        Iterator[ClassInfo] =
    {
        // Convert the set of classes to search into a map of ClassInfo objects
        // indexed by class name.

        @tailrec def classMatches(ancestorClassInfo: ClassInfo, 
                                  classToCheck: ClassInfo): Boolean =
        {
            if (classToCheck.name == ancestorClassInfo.name)
                true
            else if ((classToCheck.superClassName == ancestorClassInfo.name) ||
                     (classToCheck implements ancestorClassInfo.name))
                true
            else
            {
                classes.get(classToCheck.superClassName) match
                {
                    case None            => false
                    case Some(classInfo) => classMatches(ancestorClassInfo,
                                                         classInfo)
                }
            }            
        }

        // Find the ancestor class
        classes.get(ancestor) match
        {
            case None     =>
                Iterator.empty
            case Some(ci) => 
                classes.values.toIterator.
                filter(_.isConcrete).
                filter(classMatches(ci, _))
        }
    }
}
