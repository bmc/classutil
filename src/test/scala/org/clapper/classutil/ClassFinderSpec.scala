package org.clapper.classutil

import java.io.File
import grizzled.file.Implicits._
import grizzled.file.{util => fileutil}

class ClassFinderSpec extends BaseSpec {
  "runtimeClassFinder.getClasses" should "find classes in a specified class path" in {

    // The number of returned classInfo objects should be the same number
    // as the number of class files.
    // Ensure that we got some.
    val totalExpected = runtimeClassFiles.length
    totalExpected should be > 0

    // First, verify that we get the right number if we convert the stream
    // directly to a collection.
    val classInfoList = runtimeClassFinder.getClasses().toVector
    classInfoList.size shouldBe runtimeClassFiles.size

    // Next, check the stream by looping over it.
    val classInfoStream = runtimeClassFinder.getClasses()
    classInfoStream.size shouldBe totalExpected
  }

  "concreteSubclasses" should "find indirect implementations a trait/interface" in {
    val classes = runtimeClassFinder.getClasses()
    val cs = ClassFinder.concreteSubclasses(classOf[BaseInfo], classes)

    val csColl = cs.toVector
    csColl.size should be > 0
    val mustBePresent = classOf[org.clapper.classutil.asm.ClassInfoImpl]
    csColl.map { _.name } should contain (mustBePresent.getName)
  }

  it should "find direct implementations of a trait/interface" in {
    val classes = runtimeClassFinder.getClasses()
    val cs = ClassFinder.concreteSubclasses(classOf[ClassInfo], classes)

    val csColl = cs.toVector
    csColl.size should be > 0
    val mustBePresent = classOf[org.clapper.classutil.asm.ClassInfoImpl]
    csColl.map { _.name } should contain (mustBePresent.getName)
  }

  it should "find direct subclasses of a parent class" in {
    abstract class A
    class B extends A

    val classes = runtimeClassFinder.getClasses()
    val cs = ClassFinder.concreteSubclasses(classOf[A], classes).toVector
    cs.size should be > 0
    cs.map { _.name } should contain (classOf[B].getName)
  }

  it should "find indirect subclasses of a parent class" in {
    abstract class A
    class B extends A
    class C extends B

    val classes = runtimeClassFinder.getClasses()
    val cs = ClassFinder.concreteSubclasses(classOf[A], classes).toVector
    cs.size should be > 0
    cs.map { _.name } should contain (classOf[C].getName)
  }

  private val (runtimeClassFiles, runtimeClassFinder) = {
    import scala.util.Properties
    val version = Properties.releaseVersion.get
    val shortVersion = version.split("""\.""").take(2).mkString(".")

    val targetDirectory: Option[File] = Array(
      fileutil.joinPath("target", s"scala-$version"),
      fileutil.joinPath("target", s"scala-$shortVersion")
    )
    .map(new File(_))
    .find(_.exists)

    assert(targetDirectory.isDefined)
    val dir = targetDirectory.get

    // SBT-dependent paths
    val classDir = new File(fileutil.joinPath(dir.getPath, "classes"))
    val testClassDir = new File(fileutil.joinPath(dir.getPath, "test-classes"))

    // Get class files under the directory.
    val classFiles = classDir.listRecursively()
                             .filter(_.getName.endsWith(".class"))
                             .toVector
    val testClassFiles = testClassDir.listRecursively()
                                     .filter(_.getName.endsWith(".class"))
                                     .toVector

    // The number of returned classInfo objects should be the same number
    // as the number of class files.
    val allClassFiles = classFiles ++ testClassFiles
    val finder = ClassFinder(Seq(classDir, testClassDir))
    assert(finder.getClasses().size == allClassFiles.length)
    (allClassFiles, finder)
  }
}
