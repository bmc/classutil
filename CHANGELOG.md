---
title: "ClassUtil: Change Log"
layout: default
---

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
