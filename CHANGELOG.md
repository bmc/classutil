# Change log for ClassUtil library

Version 1.0.11

* Updated to Grizzled Scala version 2.4.0

Version 1.0.10

* Minor tweaks to permit Travis CI builds.
* Updated version of Lightbend Activator.

Version 1.0.9

* Updated to ASM version 5.1
* Bug fix: `ClassFinder.concreteSubclasses` did not properly handle traits and
  interfaces. Specifically, when looking for all implementations of an
  interface or trait, it did not properly find classes that were indirect
  implementers; it only found immediate descendents.
* Fixed some inconsistencies: `ClassFinder.getClasses` returns a `Stream`,
  but `ClassFinder.concreteSubclasses` only accepted an `Iterator` or a
  `Map`. A `Stream` version now exists.

Version 1.0.8

* Built against Scala 2.12.0-M4, instead of -M1

Version 1.0.7

* Now compiles against Scala 2.12, as well as 2.11 and 2.10.
* Updated to SBT 0.13.11.
* Added `activator`, for a self-bootstrapping build.
* Updated license to BSD 3-Clause license.

Version 1.0.6

* Add ability to retrieve runtime annotations. Patch courtesy of
  [github.com/ruippeixotog](https://github.com/ruippeixotog).
* Updated various dependencies.
* Removed `ls` SBT plugin.
* Updated to SBT 0.13.8.

Version 1.0.5

* Removed use of [Grizzled Scala][] generators, replacing them with Scala
  streams. (Generators were based on continuation passing, which is
  unsupported and unmaintained.)
* Now builds for Scala 2.11 and 2.10.
* Updated to [ASM][] 5.0.2

Version 1.0.4

* Merged change from [Patrick Berryhill](https://github.com/pjberry) providing
  support for ASM 4.x.
* Now publishes to Bintray, instead of oss.sonatype.org.
* Updated test code to latest version of ScalaTest.

Version 1.0.3:

Fixes and enhancements from [Jon Crussell](http://github.com/jcrussell):

* Added a new (private) `BaseInfo` trait as a super trait for `MethodInfo`,
  `FieldInfo` and `ClassInfo`, allowing the various modifier convenience
  methods to be shared among the three public traits.
* Added descriptors and final values to `FieldInfo`.
* Removed spurious `hashCode()` and `toString()` methods from `FieldInfoImpl`.
* Added a `Synthetic` modifier.
* Fixed a "missing method" bug caused by hash code collisions.

Version 1.0.2:

`ClassFinder` now records method descriptors, not just method signatures.
Change is courtesy [John Crussell](http://github.com/jcrussell).

Version 1.0.1:

* Cross-compiled and published for Scala 2.10.0-RC1.
* Converted to use ScalaTest 2.0, which changes `expect` to `expectResult`.

Version 1.0:

* Converted to build against Scala 2.10.0-M7. (Builds for Scala 2.9.x and
  earlier are maintained through the 0.x.x releases.)
* Converted to use the Scala 2.10 reflection API.
* Fixed Scala 2.10 feature warnings (`-feature`).
* Updated to use Scala 2.10 versions of [Grizzled Scala][]
  and [Grizzled SLF4J][].
* The project now explicitly specifies the SBT [ls][] plugin, instead of
  assuming that it's globally specified, to permit others to build the
  project more easily.

[Grizzled Scala]: http://software.clapper.org/grizzled-scala/
[Grizzled SLF4J]: http://software.clapper.org/grizzled-slf4j/
[ls]: https://github.com/softprops/ls

Version 0.4.6:

* Added Scala 2.9.2 to the set of crossbuilds.

Version 0.4.5:

* Added Scala 2.9.1-1 to the set of crossbuilds.

Version 0.4.4:

* Converted to build with SBT 0.11.2.
* Added support for `ls.implicit.ly` metadata.
* Now publishes to `oss.sonatype.org` (and, thence, to the Maven central repo).
* Bumped [Grizzled Scala][] version.
* Now builds for Scala 2.8.2, in addition to 2.8.0, 2.8.1, 2.9.0, 2.9.0-1 and
  2.9.1.

[SLF4J]: http://slf4j.org

Version 0.4.3:

* Fixed [issue #8][]: `ClassFinder` only finding one class. Problem was in
  the generator/iterator in the private `processDirectory()` method used by
  `ClassFinder.getClasses()`.
* Added a specific unit test for `ClassFinder.getClasses()`.

[issue #8]: https://github.com/bmc/classutil/issues/8

Version 0.4.2:

* Now builds for [Scala][] 2.9.1, as well as 2.9.0-1, 2.9.0, 2.8.1, and 2.8.0.

[Scala]: http://www.scala-lang.org/

Version 0.4.1:

* Converted code to conform with standard Scala coding style.

Version 0.4:

* Added the following methods to the `ClassFinder` object:
  - `classSignature`: Generates the runtime signature for a class name.
    e.g.: "Array[String]" -> "Ljava/lang/String;"
  - `methodSignature`: Generates the runtime signature for a method. e.g.:
    "def foo(s: String): Unit" -> "(Ljava/lang/String;)V"
* `ScalaObjectToBean` now creates setter methods, as well as getter methods
  (i.e., it produces true beans). See the documentation for `ScalaObjectToBean`
  for details.
* The [ASM][]-specific `InterfaceMaker` class now supports creating methods
  with parameter types. (Previously, it just created getters.)
* Upgraded to [ASM][] version 3.3.
* The generated [POM][] now uses a "scope" of "provided" for the ASM libraries.
  (The [Grizzled Scala][] and [Grizzled SLF4J][] libraries are still marked
  with scope "compile", however.)

[ASM]: http://asm.ow2.org/
[POM]: http://maven.apache.org/guides/introduction/introduction-to-the-pom.html
[Grizzled Scala]: http://software.clapper.org/grizzled-scala/
[Grizzled SLF4J]: http://software.clapper.org/grizzled-slf4j/

Version 0.3.7:

* Merged changes from Alex Cruise to close input streams in `ClassFinder`.
  Streams were (inadvertently) left open and, therefore, cluttering up memory
  until the garbage collector ran.
* Now builds against Scala 2.9.0.1, as well as Scala 2.9.0, 2.8.1 and 2.8.0.
* Converted to build with [SBT][] 0.10.1
* Updated dependencies for [Grizzled Scala][] and [Grizzled SLF4J][].

[Grizzled Scala]: http://software.clapper.org/grizzled-scala/
[Grizzled SLF4J]: http://software.clapper.org/grizzled-slf4j/

Version 0.3.6:

* Some fixes to `ClassUtil.isOfType` so that it works properly with 2.9.0,
  as well as 2.8.x.

[SBT]: http://code.google.com/p/simple-build-tool/
[ASM]: http://asm.ow2.org/

Version 0.3.5:

* Now builds against Scala 2.9.0, as well as Scala 2.8.0 and 2.8.1.
* Updated to version 1.4.1 of [ScalaTest][] for Scala 2.9.0. (Still uses
  ScalaTest 1.3, for Scala 2.8).
* Updated to use [SBT][] 0.7.7.
* Updated to version 3.3.1 of the [ASM][] byte code library.

[ScalaTest]: http://www.scalatest.org/
[SBT]: http://code.google.com/p/simple-build-tool/
[ASM]: http://asm.ow2.org/

Version 0.3.4:

* Folded in a [patch][] from [Aemon Cannon][], which tunes `ClassFinder`
  performance by substituting mutable collections for the immutable ones.

[patch]: https://github.com/aemoncannon/classutil/commit/cdb1ac7987bd7d011e108dc5f63730a93db582de
[Aemon Cannon]: https://github.com/aemoncannon/

Version 0.3.3:

* Folded in a [patch][] from [Aemon Cannon][], to protect against null
  class names returned from [ASM][].

[patch]: https://github.com/aemoncannon/classutil/commit/37d740dcd7ceb18615bde1715131a4cb81b43567
[Aemon Cannon]: https://github.com/aemoncannon/
[ASM]: http://asm.ow2.org/

Version 0.3.2:

* Fixed title in generated Scaladocs.
* Now builds against [Scala][] 2.8.1, as well as 2.8.0.

[Scala]: http://www.scala-lang.org/

Version 0.3.1:

* Fixed [Issue #1][]: `MapToBean` can generate class names that conflict
  with previously generated class names.
* Now compiles against [Scala][] 2.8.1 RC1, as well as 2.8.0
* Now depends on version 1.0.1 of [Grizzled Scala][].

[Scala]: http://www.scala-lang.org/
[Issue #1]: http://github.com/bmc/classutil/issues/issue/1
[Grizzled Scala]: http://bmc.github.com/grizzled-scala/

Version 0.3:

* Now published to the [Scala Tools Maven repository][], which [SBT][]
  includes by default. Thus, if you're using SBT, it's longer necessary to
  specify a custom repository to find this artifact.

[Scala Tools Maven repository]: http://www.scala-tools.org/repo-releases/
[SBT]: http://code.google.com/p/simple-build-tool/

Version 0.2.2:

* Added `ClassUtil` utility module, with some useful utility methods,
  including: `isPrimitive()` methods that test objects and classes to see
  if they represent primitives; an `isOfType` method that uses a Scala
  `Manifest` to allow runtime tests against generic types; and a
  `loadClass` method that simplifies loading a class from its bytes.
* Added `ScalaObjectToBean`, which takes a Scala object and creates a Java
  bean that wraps it, at runtime.
* Added `ClassNameGenerator`, which generates class names automatically.
* Refactored various internal implementation details, for ease of re-use.
* Updated to released 1.2 version of [ScalaTest][] and converted its
  dependency in [SBT][] to a test-time, not run-time, dependency.

[ScalaTest]: http://scalatest.org/
[SBT]: http://code.google.com/p/simple-build-tool/

Version 0.2.1:

* Updated to build with Scala 2.8.0.final *only*.

Version 0.2:

* Added new `MapToBean` module, which takes a Scala map of type
  `Map[String, Any]` and converts it, on the fly, to a Java Bean. By
  default, it also recursively converts any nested maps of type
  `Map[String, Any]` that it finds. The transformation results in an object
  that can only really be used via reflection; however, that fits fine with
  some APIs that want to receive Java Beans as parameters. For a complete
  description, see
  [the appropriate section on the web site](http://bmc.github.com/classutil/#generating_java_beans_from_scala_maps).
* Updated to version 0.7.1 of [Grizzled Scala][].
* Updated to version 0.2.2 of [Grizzled SLF4J][].
* Removed unnecessary dependency on old version of [Grizzled Scala][] in
  build file.
*
* Now compiles under Scala 2.8.0.RC5. Now builds against RC3 and RC5 only.

[ASM]: http://asm.ow2.org/
[SBT]: http://code.google.com/p/simple-build-tool
[Grizzled Scala]: http://bmc.github.com/grizzled-scala/
[Grizzled SLF4J]: http://bmc.github.com/grizzled-slf4j/

Version 0.1.2:

* Updated to version 0.7 of [Grizzled Scala][].
* Updated to version 0.2.2 of [Grizzled SLF4J][].
* Bumped to [SBT][] version 0.7.4.
* Tightened access restrictions on some ASM-specific internal classes.
* Now compiles under Scala 2.8.0.RC3 and RC2. Dropped support for RC1.

[SBT]: http://code.google.com/p/simple-build-tool
[Grizzled Scala]: http://bmc.github.com/grizzled-scala/
[Grizzled SLF4J]: http://bmc.github.com/grizzled-slf4j/

Version 0.1.1:

* Now compiles against Scala 2.8.0.RC2, as well as 2.8.0.RC1.


Version 0.1:

* Initial release.
