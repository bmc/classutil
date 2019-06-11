# Change log for ClassUtil library

Version 1.5.1:

* Updated license in `build.sbt`, so it'll be reflected in the POM.

Version 1.5.0:

* Now compiles against Scala 2.13.0, as well as 2.12.x and 2.11.x.
  Supporting 2.13.0 required adding a compatibility layer for previous versions.
  Some function type signatures now appear 2.13.0-biased. For instance, some
  methods now return `LazyList[T]` instead of `Stream[T]`. For versions of
  Scala prior to 2.13.0, there's an internal compatibility type alias that
  maps `LazyList` to `Stream`.
* Support for Scala 2.10 **has been dropped.**
* Upgraded to ASM version 7.1 (courtesy of @sullis).
* Now licensed under the [Apache License, version 2.0](https://www.apache.org/licenses/LICENSE-2.0),
  instead of the [3-Clause BSD License](https://opensource.org/licenses/BSD-3-Clause).
  The previous license still applies to older versions.

Version 1.4.0:

* Build changes to allow test suite to work on non-release versions of
  Scala, for the Scala Community Build (courtesy of @retronym).
* ASM version is now configurable at run-time (courtesy of @FloWi). This
  update changes the API in a backward-compatible way, but recompilation
  of clients might be necessary.

Version 1.3.0

* Merged [PR #25](https://github.com/bmc/classutil/pull/25), from @ElfoLiNk,
  which updates [ASM][] to version 6 and upgrades SBT to 1.x.
* Merged [PR #27](https://github.com/bmc/classutil/pull/27), from @xuwei-k,
  which fixes an old-style procedure syntax issue.
* Rolled in changes from @SethTisue to update the `sbt-bintray` version,
  as well as the Scala and ScalaTest versions.
* Converted `org.clapper.classutil.Modifier` from an `Enumeration` to a
  set of sealed case objects. The interface remains the same.
* Enabled [Wart Remover](http://www.wartremover.org) and many, but not all,
  of its errors. Cleaned up offending code.

Version 1.2.0

* Refactored some internal methods to the `ClassUtil` object, resulting in
  some new public methods, including `isSetter()`, `isGetter()`,
  `beanName()` and `scalaAccessorMethods()`.
* Updated list of methods that should be skipped (i.e., methods that look like
  getters or setters, but aren't).
* Added `ClassUtil.nonFinalPublicMethods()` convenience helper.
* Added `ScalaObjectToBean.withResultMapper()`, allowing caller to trap,
  examine, and manipulate any methods called in the generated bean.
* `MapToBean` also generates Scala-style getters, not just bean-style getters.
* Updated to ScalaTest 3.0.4 and Grizzled Scala 4.4.2.
* Update cross-compile versions to 2.10.7, 2.11.12 and 2.12.4.

Version 1.1.2

* Added more tests and converted all remaining tests to ScalaTest's `FlatSpec`.
* Made miscellaneous mostly cosmetic code cleanups.

Version 1.1.1

* Now builds against Scala 2.12.1.
* Updated to Grizzled Scala version 4.2.0

Version 1.1.0

* Now builds against Scala 2.12.0-final.
* Updated to Grizzled Scala version 3.1.0

Version 1.0.13

* Updated Scala 2.12 cross-build to use Scala 2.12.0-RC1.
* Updated to Grizzled Scala version 3.0.0
* Replaced uses of `scala.collection.JavaConversions` with
  `scala.collection.JavaConverters`

Version 1.0.12

* Updated to Grizzled Scala version 2.4.1

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
