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

/** This library provides methods for locating and filtering classes
  * quickly--faster, in fact, than can be done with Java or Scala runtime
  * reflection. Under the covers, it uses the ASM bytecode library, though
  * it can easily be extended to use other bytecode libraries. ClassUtil
  * loads and returns information about classes using an efficient lazy
  * iterator approach, which offers minimal startup penalty and the ability
  * to cut the traversal short.
  */
package org.clapper.classutil

import java.io.{File, InputStream}
import java.util.jar.{JarFile, Manifest => JarManifest}
import java.util.zip.{ZipEntry, ZipFile}

import scala.annotation.tailrec
import scala.language.reflectiveCalls

/** An enumerated high-level view of the modifiers that can be attached
  * to a method, class or field.
  */
object Modifier {

  abstract sealed class Modifier(val name: String, val id: Int)
    extends Product with Serializable {
    override def hashCode: Int = id.hashCode
  }

  case object Abstract extends Modifier(name = "abstract", id = 1)

  case object Final extends Modifier(name = "final", id = 2)

  case object Interface extends Modifier(name = "interface", id = 3)

  case object Native extends Modifier(name = "native", id = 4)

  case object Private extends Modifier(name = "private", id = 5)

  case object Protected extends Modifier(name = "protected", id = 6)

  case object Public extends Modifier(name = "public", id = 7)

  case object Static extends Modifier(name = "static", id = 8)

  case object Strict extends Modifier(name = "strict", id = 0)

  case object Synchronized extends Modifier(name = "synchronized", id = 10)

  case object Synthetic extends Modifier(name = "synthetic", id = 11)

  case object Transient extends Modifier(name = "transient", id = 12)

  case object Volatile extends Modifier(name = "volatile", id = 13)

}

/** Base trait for method, field and class info.
  */
private[classutil] trait BaseInfo {
  /** The name of the entity.
    */
  val name: String

  /** The entity's modifiers.
    */
  val modifiers: Set[Modifier.Modifier]

  /** A printable version of the field. Currently, the string version is
    * the entity name.
    */
  override def toString = name

  /** Convenience method that determines whether the class implements an
    * interface. This method is just shorthand for:
    * {{{
    * modifiers contains Modifier.Interface
    * }}}
    */
  def isInterface = modifiers contains Modifier.Interface

  /** Convenience method that determines whether the class is abstract
    * This method is just shorthand for:
    * {{{
    * modifiers contains Modifier.Abstract
    * }}}
    */
  def isAbstract = modifiers contains Modifier.Abstract

  /** Convenience method that determines whether the class is private.
    * This method is just shorthand for:
    * {{{
    * modifiers contains Modifier.Private
    * }}}
    */
  def isPrivate = modifiers contains Modifier.Private

  /** Convenience method that determines whether the class is protected.
    * This method is just shorthand for:
    * {{{
    * modifiers contains Modifier.Protected
    * }}}
    */
  def isProtected = modifiers contains Modifier.Protected

  /** Convenience methods that determines whether the class is public.
    * This method is just shorthand for:
    * {{{
    * modifiers contains Modifier.Public
    * }}}
    */
  def isPublic = modifiers contains Modifier.Public

  /** Convenience methods that determines whether the class is final.
    * This method is just shorthand for:
    * {{{
    * modifiers contains Modifier.Final
    * }}}
    */
  def isFinal = modifiers contains Modifier.Final

  /** Convenience methods that determines whether the class is static.
    * This method is just shorthand for:
    * {{{
    * modifiers contains Modifier.Static
    * }}}
    */
  def isStatic = modifiers contains Modifier.Static

  /** Convenience methods that determines whether the class is synchronized.
    * This method is just shorthand for:
    * {{{
    * modifiers contains Modifier.Synchronized
    * }}}
    */
  def isSynchronized = modifiers contains Modifier.Synchronized

  /** Convenience methods that determines whether the class is synthetic.
    * This method is just shorthand for:
    * {{{
    * modifiers contains Modifier.Synthetic
    * }}}
    */
  def isSynthetic = modifiers contains Modifier.Synthetic

  /** Convenience method to determine whether the class is concrete (i.e.,
    * isn't abstract and isn't an interface).
    */
  def isConcrete = !((modifiers contains Modifier.Abstract) ||
    (modifiers contains Modifier.Interface))

}

/** Information about a method, as read from a class file.
  */
trait MethodInfo extends BaseInfo {
  /** The method's JVM signature (only available with generics).
    * Ex: java.util.List.iterator ()Ljava/util/Iterator<TE;>;
    */
  val signature: String

  /** The method's descriptor which describes it's arg types
    * and return type.
    * Ex: (ILjava/lang/String;)[I
    */
  val descriptor: String

  /** A list of the checked exceptions (as class names) that the method
    * throws, or an empty list if it throws no known checked exceptions.
    */
  val exceptions: List[String]

  /** A printable version of the method. Currently, the string is
    * the method name plus descriptor.
    */
  override def toString = name + descriptor

  override def hashCode = toString.hashCode

  override def equals(o: Any) = o match {
    case m: MethodInfo => m.toString == toString
    case _ => false
  }
}

/** Information about a field, as read from a class file.
  */
trait FieldInfo extends BaseInfo {
  /** The field's JVM signature (only available with generics).
    */
  val signature: String

  /** The field's descriptor which describes it's type
    * Ex: Ljava/lang/String;
    */
  val descriptor: String

  /** The field's default value, only available when the field
    * is a static field that is a primitive or a String type.
    */
  val value: Option[java.lang.Object]

  override def hashCode = name.hashCode

  override def equals(o: Any) = o match {
    case m: FieldInfo => m.name == name
    case _ => false
  }
}

/** Information about a field, as read from a class file.
  */
trait AnnotationInfo {

  /** The annotations's descriptor which describes it's type
    * Ex: Lscala/reflect/ScalaSignature;
    */
  val descriptor: String

  val visible: Boolean

  def params: Map[String, Any]

  override def hashCode = (descriptor, params).hashCode

  override def equals(o: Any) = o match {
    case m: AnnotationInfo => m.descriptor == descriptor && m.params == params
    case _ => false
  }
}

/** Information about a class, as read from a class file.
  */
trait ClassInfo extends BaseInfo {
  /** The parent class's fully qualified name.
    */
  def superClassName: String

  /** A list of the interfaces, as class names, that the class implements;
    * or, an empty list if it implements no interfaces.
    */
  def interfaces: List[String]

  /** The class's JVM signature.
    */
  def signature: String

  /** Where the class was found (directory, jar file, or zip file).
    */
  def location: File

  /** A set of the methods in the class.
    */
  def methods: Set[MethodInfo]

  /** A set of the fields in the class.
    */
  def fields: Set[FieldInfo]

  /** A set of the runtime-retained annotations in the class.
    */
  def annotations: Set[AnnotationInfo]

  /** Convenience method to determine whether this class directly
    * implements a specific interface. Since a `ClassInfo` object contains
    * information about a single class, this method cannot determine
    * whether a class indirectly implements an interface. That capability
    * is a higher-order operation.
    *
    * @param interface the name of the interface
    * @return whether the class implements the interface
    */
  def implements(interface: String): Boolean = interfaces contains interface
}

/** A `ClassFinder` finds classes in a class path, returning the result in a
  * lazy iterator. The iterator can then be filtered, mapped, or passed to
  * the utility methods in the `ClassFinder` companion object.
  *
  * @param path                    a sequence of directories, jars and zips to search
  * @param maybeOverrideAsmVersion the version of asm to be used. Defaults to v6.
  *                                To override use one of the ASM-fields in [[org.objectweb.asm.Opcodes]].
  *                                (e.g. [[org.objectweb.asm.Opcodes.ASM5]])
  */
class ClassFinder(path: Seq[File], maybeOverrideAsmVersion: Option[Int]) {
  val classpath: List[File] = path.toList

  /** Find all classes in the specified path, which can contain directories,
    * zip files and jar files. Returns metadata about each class in a
    * `ClassInfo` object. The `ClassInfo` objects are returned lazily,
    * rather than loaded all up-front.
    *
    * @return a `Stream` of `ClassInfo` objects
    */
  def getClasses(): Stream[ClassInfo] = find(classpath)

  /* ---------------------------------------------------------------------- *\
   Private Methods
   \* ---------------------------------------------------------------------- */

  private def find(path: Seq[File]): Stream[ClassInfo] = {
    path match {
      case Nil => Stream.empty[ClassInfo]
      case item :: Nil => findClassesIn(item)
      case item :: tail => findClassesIn(item) ++ find(tail)
    }
  }

  private def findClassesIn(f: File): Stream[ClassInfo] = {
    val name = f.getPath.toLowerCase

    if (name.endsWith(".jar"))
      processJar(f)
    else if (name.endsWith(".zip"))
      processZip(f)
    else if (f.isDirectory)
      processDirectory(f)
    else
      Stream.empty[ClassInfo]
  }

  private def processJar(file: File): Stream[ClassInfo] = {
    val jar = new JarFile(file)
    val list1 = processOpenZip(file, jar)

    Option(jar.getManifest)
      .map { manifest =>
        val path = loadManifestPath(jar, file, manifest)
        val list2 = find(path)
        list1 ++ list2
      }
      .getOrElse(list1)
  }

  private def loadManifestPath(jar: JarFile,
                               jarFile: File,
                               manifest: JarManifest): List[File] = {
    val attrs = manifest.getMainAttributes
    val value = attrs.get("Class-Path").asInstanceOf[String]

    if (value == null)
      Nil

    else {
      val parent = jarFile.getParent
      val tokens = value.split("""\s+""").toList

      if (parent == null)
        tokens.map(new File(_))
      else
        tokens.map(s => new File(parent + File.separator + s))
    }
  }

  private def processZip(file: File): Stream[ClassInfo] =
    processOpenZip(file, new ZipFile(file))

  private def processOpenZip(file: File, zipFile: ZipFile) = {
    import scala.collection.JavaConverters._

    val classInfoIterators =
      zipFile.entries
        .asScala
        .toStream
        .filter((e: ZipEntry) => isClass(e))
        .map((e: ZipEntry) => classData(zipFile.getInputStream(e), file))

    for {it <- classInfoIterators
         data <- it}
      yield data
  }

  // Structural type that matches both ZipEntry and File
  private type FileEntry = {
    def isDirectory(): Boolean
    def getName(): String
  }

  private def isClass(e: FileEntry): Boolean =
    (!e.isDirectory) && e.getName.toLowerCase.endsWith(".class")

  private def processDirectory(dir: File): Stream[ClassInfo] = {
    import java.io.FileInputStream

    import grizzled.file.Implicits._

    val inputStreams = dir.listRecursively().filter(isClass).
      map(f => new FileInputStream(f))

    val iterators =
      for (fis <- inputStreams) yield {
        try {
          classData(fis, dir)
        }
        finally {
          fis.close()
        }
      }

    for {it <- iterators
         data <- it}
      yield data
  }

  private def classData(is: InputStream,
                        location: File): Iterator[ClassInfo] = {
    import org.clapper.classutil.asm.ClassFile

    ClassFile.load(is, location, maybeOverrideAsmVersion.getOrElse(asm.DefaultAsmVersion))
  }
}

/** The entrance to the factory floor, providing methods for finding and
  * filtering classes.
  */
object ClassFinder {
  /** Convenient method for getting the standard JVM classpath, into a
    * variable suitable for use with the `find()` method.
    *
    * @return the classpath, as a list of `File` objects
    */
  def classpath: List[File] = System
    .getProperty("java.class.path")
    .split(File.pathSeparator)
    .map(s => if (s.trim.length == 0) "." else s)
    .map(new File(_))
    .toList

  /** Instantiate a new `ClassFinder` that will search the specified
    * classpath, or the default classpath, if no classpath is defined.
    *
    * @param path                    the classpath, which is a sequence of `File`
    *                                objects representing directories, jars and zip files
    *                                to search. Defaults to `classpath` if empty.
    * @param maybeOverrideAsmVersion the version of asm to be used. Defaults to v6.
    *                                To override use one of the ASM-fields in [[org.objectweb.asm.Opcodes]].
    *                                (e.g. [[org.objectweb.asm.Opcodes.ASM5]])
    * @return a new `ClassFinder` object
    */
  def apply(path: Seq[File] = Seq.empty[File], maybeOverrideAsmVersion: Option[Int] = None): ClassFinder =
    new ClassFinder(if (path.nonEmpty) path else classpath, maybeOverrideAsmVersion)

  /** Create a map from an Iterator of ClassInfo objects. The resulting
    * map is indexed by class name.
    *
    * @return a map of (classname, `ClassInfo`) pairs
    */
  def classInfoMap(iterator: Iterator[ClassInfo]): Map[String, ClassInfo] =
    iterator.map(c => c.name -> c).toMap

  /** Create a map from a Stream of ClassInfo objects. The resulting map is
    * indexed by class name.
    *
    * @return a map of (classname, `ClassInfo`) pairs
    */
  def classInfoMap(stream: Stream[ClassInfo]): Map[String, ClassInfo] =
    classInfoMap(stream.toIterator)

  /** Convenience method that scans the specified classes for all concrete
    * classes that are subclasses of a superclass or trait. A subclass, in this
    * definition, is a class that directly or indirectly (a) implements an
    * interface (if the named class is an interface) or (b) extends a
    * subclass (if the named class is a class). The class must be
    * concrete, so intermediate abstract classes are not returned, though
    * any children of such abstract classes will be.
    *
    * '''WARNINGS'''
    *
    * This method converts the stream to a map of classes, for easier
    * lookup. Thus, upon its return, the stream will be empty. You can
    * certainly recreate the stream, but at a cost. If you need to make
    * multiple calls to this method with the same classpath, consider
    * converting the stream to a map first, as shown below:
    * {{{
    * val finder = ClassFinder(myPath)
    * val classes = finder.getClasses  // classes is an Stream[ClassInfo]
    * val classMap = ClassFinder.classInfoMap // runs the stream out, once
    * val foos = ClassFinder.concreteSubclasses(classOf[org.example.Foo], classMap)
    * val bars = ClassFinder.concreteSubclasses(classOf[Bar], classMap)
    * }}}
    *
    * This method can chew up a lot of temporary heap space, if called
    * with a large classpath.
    *
    * @param ancestor the `Class` object of the superclass or trait for which
    *                 to find descendent concrete subclasses
    * @param classes  the stream of `ClassInfo` objects to search
    * @return an iterator of `ClassInfo` objects that are concrete subclasses
    *         of `ancestor`. The iterator will be empty if no matching classes
    *         could be found.
    */
  def concreteSubclasses(ancestor: Class[_], classes: Stream[ClassInfo]):
  Iterator[ClassInfo] = {
    findConcreteSubclasses(ancestor.getName, ClassFinder.classInfoMap(classes))
  }

  /** Variant of `concreteSubclasses()` that takes a string class name and
    * a `Stream` of `ClassInfo` objects, rather than a `Class` and a `Stream`.
    *
    * @param ancestor the name of the class for which to find descendent
    *                 concrete subclasses
    * @param classes  the stream of `ClassInfo` objects to search
    * @return an iterator of `ClassInfo` objects that are concrete subclasses
    *         of `ancestor`. The iterator will be empty if no matching classes
    *         could be found.
    * @see `concreteSubclasses(Class[_], Stream[ClassInfo])`
    */
  def concreteSubclasses(ancestor: String, classes: Stream[ClassInfo]):
  Iterator[ClassInfo] = {
    findConcreteSubclasses(ancestor, ClassFinder.classInfoMap(classes))
  }

  /** Variant of `concreteSubclasses()` that takes a class and an `Iterator`
    * of `ClassInfo` objects, rather than a `Class` and a `Stream`.
    *
    * @example
    * {{{
    *   val finder = ClassFinder(myPath)
    *   val classes = finder.getClasses  // classes is an Stream[ClassInfo]
    *   // Of course, it's easier just to call the version that takes a
    *   // Stream...
    *   ClassFinder.concreteSubclasses(classOf[Baz], classes.toIterator)
    * }}}
    * @param ancestor the `Class` object of the superclass or trait for which
    *                 to find descendent concrete subclasses
    * @param classes  the iterator of `ClassInfo` objects to search
    * @return an iterator of `ClassInfo` objects that are concrete subclasses
    *         of `ancestor`. The iterator will be empty if no matching classes
    *         could be found.
    * @see `concreteSubclasses(Class[_], Stream[ClassInfo])`
    */
  def concreteSubclasses(ancestor: Class[_], classes: Iterator[ClassInfo]):
  Iterator[ClassInfo] = {
    findConcreteSubclasses(ancestor.getName, ClassFinder.classInfoMap(classes))
  }

  /** Variant of `concreteSubclasses()` that takes a string class name and
    * an `Iterator` of `ClassInfo` objects, rather than a `Class` and an
    * `Iterator`.
    *
    * @example
    * {{{
    *   val finder = ClassFinder(myPath)
    *   val classes = finder.getClasses  // classes is an Stream[ClassInfo]
    *   // Of course, it's easier just to call the version that takes a
    *   // Stream...
    *   ClassFinder.concreteSubclasses("org.example.Foo", classes.toIterator)
    * }}}
    * @param ancestor the name of the class for which to find descendent
    *                 concrete subclasses
    * @param classes  the stream of `ClassInfo` objects to search
    * @return an iterator of `ClassInfo` objects that are concrete subclasses
    *         of `ancestor`. The iterator will be empty if no matching classes
    *         could be found.
    * @see `concreteSubclasses(String, Stream[ClassInfo])`
    * @see `concreteSubclasses(Class[_], Iterator[ClassInfo])`
    */
  def concreteSubclasses(ancestor: String, classes: Iterator[ClassInfo]):
  Iterator[ClassInfo] = {
    findConcreteSubclasses(ancestor, ClassFinder.classInfoMap(classes))
  }

  /** Variant of `concreteSubclasses()` that takes a class and a `Map`
    * `ClassInfo` objects.
    *
    * @param ancestor the `Class` object of the superclass or trait for which
    *                 to find descendent concrete subclasses
    * @param classes  the iterator of `ClassInfo` objects to search
    * @return an iterator of `ClassInfo` objects that are concrete subclasses
    *         of `ancestor`. The iterator will be empty if no matching classes
    *         could be found.
    * @see `concreteSubclasses(Class[_], Stream[ClassInfo])`
    * @see `concreteSubclasses(Class[_], Iterator[ClassInfo])`
    */
  def concreteSubclasses(ancestor: Class[_], classes: Map[String, ClassInfo]):
  Iterator[ClassInfo] = {
    findConcreteSubclasses(ancestor.getName, classes)
  }

  /** Variant of `concreteSubclasses()` that takes a string class name and
    * a map, rather than a `Class` and a map.
    *
    * @param ancestor the name of the class for which to find descendent
    *                 concrete subclasses
    * @param classes  the iterator of `ClassInfo` objects to search
    * @return an iterator of `ClassInfo` objects that are concrete subclasses
    *         of `ancestor`. The iterator will be empty if no matching classes
    *         could be found.
    * @see `concreteSubclasses(Class[_], Map[String, ClassInfo])`
    * @see `concreteSubclasses(String, Stream[ClassInfo])`
    * @see `concreteSubclasses(String, Iterator[ClassInfo])`
    */
  def concreteSubclasses(ancestor: String, classes: Map[String, ClassInfo]):
  Iterator[ClassInfo] = {

    findConcreteSubclasses(ancestor, classes)
  }

  // -------------------------------------------------------------------------
  // Private methods
  // -------------------------------------------------------------------------

  private def findConcreteSubclasses(ancestor: String,
                                     classes: Map[String, ClassInfo]):
  Iterator[ClassInfo] = {

    // Convert the set of classes to search into a map of ClassInfo objects
    // indexed by class name.
    @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable"))
    @tailrec
    def classMatches(targetClassInfo: ClassInfo,
                     classesToCheck: Seq[ClassInfo]): Boolean = {
      val targetName = targetClassInfo.name // could use ancestor, but, yuck.
      val classNames = classesToCheck.map(_.name)
      val interfaceNamesToCheck = classesToCheck.flatMap(_.interfaces)
      if (classNames contains targetName) {
        // The current classes we're checking match the target class. Done.
        true
      }
      else if (interfaceNamesToCheck contains targetName) {
        // At least one of current classes implements an interface that is
        // the target class. Done.
        true
      }
      else {
        val superClasses = classesToCheck.flatMap { c =>
          classes.get(c.superClassName)
        }
        val interfaces = interfaceNamesToCheck.flatMap { i =>
          classes.get(i)
        }

        val newClassesToCheck = superClasses ++ interfaces
        if (newClassesToCheck.isEmpty) {
          // No matches. Done.
          false
        }
        else {
          // Dive deeper.
          classMatches(targetClassInfo, newClassesToCheck)
        }
      }
    }

    // Find the ancestor class
    classes.get(ancestor).map { classInfo =>
      classes.values
        .toIterator
        .filter(_.isConcrete)
        .filter(c => classMatches(classInfo, Seq(c)))
    }
      .getOrElse(Iterator.empty)
  }
}
