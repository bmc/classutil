---
title: ClassUtil—A Scala-friendly, fast class-finder library (with extras)
layout: withTOC
---

## Introduction

The *org.clapper.classutil* (ClassUtil) library is a Scala package that
provides methods to locate and filter classes quickly, at runtime—more
quickly, in fact, than can be done with the JVM's runtime reflection
capabilities. Under the covers, ClassUtil uses the [ASM][] bytecode
library, though it can easily be extended to use a different byte code
library.

ClassUtil is fast for several reasons:

* A bytecode library like [ASM][] loads compiled bytecode without using
  a JVM class loader. So, it avoids all the class loader's overhead.

* ClassUtil loads and returns information about classes using an efficient
  lazy iterator, which offers minimal startup penalty and the ability to
  cut the traversal short.

## Requirements

* ClassUtil requires a Scala 2.8 environment, because it uses the
  [continuations][] plug-in available in 2.8.

## Installation

The easiest way to install the ClassUtil library is to download a
pre-compiled jar from the [*clapper.org* Maven repository][]. However, you
can also get certain build tools to download it for you.

### Installing with Maven

If you're using [Maven][], you can get ClassUtil from the
[*clapper.org* Maven Repository][]. The relevant pieces of information are:

* Group ID: `org.clapper`
* Artifact ID: `classutil_2.8.0.RC1`
* Version: `0.1`
* Type: `jar`
* Repository: `http://maven.clapper.org/`

Here's a sample Maven POM "dependency" snippet:

    <dependency>
      <groupId>org.clapper</groupId>
      <artifactId>classutil_2.8.0.RC1</artifactId>
      <version>0.1</version>
    </dependency>

### Using with SBT

If you're using [SBT][] (the Simple Build Tool) to compile your code, you
can place the following lines in your project file (i.e., the Scala file in
your `project/build/` directory):

    val orgClapperRepo = "clapper.org Maven Repository" at
        "http://maven.clapper.org"
    val grizzled = "org.clapper" %% "classutil" % "0.1"

**NOTE:** The first doubled percent is *not* a typo. It tells SBT to treat
ClassUtil as a cross-built library and automatically inserts the Scala
version you're using into the artifact ID. Currently, it will *only* work
if you are building with Scala 2.8.0.RC1. See the [SBT cross-building][]
page for details.

## Building from Source

You can also build ClassUtil from source. There are two ways to get the
source:

### Downloading a snapshot of the source

You can download a tarball or zip file of the source from the
[downloads page][].

### Source Code Repository

The source code for ClassUtil is maintained on [GitHub][]. To clone
the repository, run this command:

    git clone git://github.com/bmc/classutil.git

### Building

Building the library requires [SBT][]. Install SBT, as described at the SBT
web site. Then, assuming you have an `sbt` shell script (or .BAT file, for
Windows), run:

    sbt update

That command will pull down the external jars on which the Grizzled SLF4J
Library depends. After that step, build the library with:

    sbt +compile +package

The resulting jar files will be under the top-level `target` directory, in
subdirectories specific to each Scala version.

## Using ClassUtil

ClassUtil is simple to use. The primary class in the library is the
`ClassFinder` class, which has a corresponding companion object that
contains utility methods. The following examples illustrate use of the
library.

### Getting information on all classes in the current class path

    import org.clapper.classutil

    val finder = ClassFinder()
    val classes = finder.getClasses // classes is an Iterator[ClassInfo]
    classes.foreach(println(_))

### Getting all concrete classes in a custom class path

    import org.clapper.classutil
    import java.io.File

    val classpath = List("foo.jar", "bar.jar", "baz.zip").map(new File(_))
    val finder = ClassFinder(classpath)
    val classes = finder.getClasses.filter(_.isConcrete)
    classes.foreach(println(_))

### Getting all interfaces in a custom class path

    import org.clapper.classutil
    import java.io.File

    val classpath = List("foo.jar", "bar.jar", "baz.zip").map(new File(_))
    val finder = ClassFinder(classpath)
    val classes = finder.getClasses.filter(_.isConcrete)
    classes.foreach(println(_))

### Finding all classes that implement an interface, directly or indirectly

Being able to locate all classes that implement an interface or extend an
abstract class is sometimes useful. Perhaps you're implementing a plugin
capability, and you want to discover all concrete classes that implement
some plugin interface, for instance. ClassUtil provides a special utility
method for that:

    import org.clapper.classutil
    import java.io.File

    val classpath = List("foo.jar", "bar.jar", "baz.zip").map(new File(_))
    val finder = ClassFinder(classpath)
    val classes = finder.getClasses
    val plugins = ClassFinder.concreteSubclasses("org.example.plugin", classes)
    classes.foreach(println(_))

Note that the `concreteSubclasses()` method called above takes the iterator
of `ClassInfo` objects returned by `ClassFinder.getClasses`. This
`concreteSubclasses` method converts the iterator to a map of classes, for
easier lookup. Thus, upon its return, the iterator will be empty. You can
certainly recreate the iterator, but at a cost. If you need to make
multiple calls to `concreteSubclasses` with the same classpath, consider
converting the iterator to a map first, as shown below:

    val finder = ClassFinder(myPath)
    val classes = finder.getClasses  // classes is an Iterator[ClassInfo]
    val classMap = ClassFinder.classInfoMap // runs the iterator out, once
    val foos = ClassFinder.concreteSubclasses("org.example.Foo", classMap)
    val bars = ClassFinder.concreteSubclasses("org.example.Bar", classMap)

**WARNING**: `concreteSubclasses` can chew up a lot of temporary heap
space, if called with a large classpath. Either use a "focused" classpath,
or make sure you run with a large enough maximum heap.

### The ClassInfo classes

Metadata about classes is loaded into three types of objects:

`ClassInfo` contains metadata about a class, including:

* its name
* its modifiers (whether it is an interface, is private, is public, etc.)
* data about its methods, as `MethodInfo` objects
* data about its fields, as `FieldInfo` objects

`MethodInfo` contains metadata about a method, including:

* the names of all declared exceptions it throws
* its modifiers (whether it is private, public, synchronized, etc.)
* its runtime signature

`FieldInfo` contains metadata about a method, including:

* the names of all declared exceptions it throws
* its modifiers (whether it is private, public, synchronized, etc.)
* its runtime signature

The modifiers are an abstraction: a set of enumerated values that aren't
tied to the underlying ASM representation. This allows the ClassUtil
API to be ported to other bytecode libraries, if necessary.

Please see the [API documentation][] for additional information.

### API Docs

The full Scaladoc API documentation is available [here][API documentation].

## Runtime requirements

ClassUtil uses the following libraries, which must be present in the
classpath at runtime:

* The main [ASM][] library (version 3), e.g., `asm-3.2.jar`
* The [ASM][] commons library (version 3), e.g., `asm-commons3.2.jar`
* The [Grizzled Scala][] library
* The [Grizzled SLF4J][] library, for logging
* The [SLF4J][] API library, for logging (e.g., `slf4j-api-1.5.11.jar`)
* An SLF4J implementation, such as [Logback][] or [AVSL][], if you want
  logging.

## Author

Brian M. Clapper, [bmc@clapper.org][]

## Copyright and License

The ClassUtil Library is copyright &copy; 2010 Brian M. Clapper and is
released under a [BSD License][].

## Patches

I gladly accept patches from their original authors. Feel free to email
patches to me or to fork the [GitHub repository][] and send me a pull
request. Along with any patch you send:

* Please state that the patch is your original work.
* Please indicate that you license the work to the PROJECT project
  under a [BSD License][].

[BSD License]: license.html
[GitHub repository]: http://github.com/bmc/classutil
[GitHub]: http://github.com/bmc/
[downloads page]: http://github.com/bmc/classutil/downloads
[*clapper.org* Maven repository]: http://maven.clapper.org/org/clapper/
[Maven]: http://maven.apache.org/
[ASM]: http://asm.ow2.org/
[SBT]: http://code.google.com/p/simple-build-tool
[SBT cross-building]: http://code.google.com/p/simple-build-tool/wiki/CrossBuild
[bmc@clapper.org]: mailto:bmc@clapper.org
[continuations]: http://www.scala-lang.org/node/2096
[Grizzled Scala]: http://bmc.github.com/grizzled-scala/
[Grizzled SLF4J]: http://bmc.github.com/grizzled-slf4j/
[SLF4J]: http://slf4j.org/
[Logback]: http://logback.qos.ch/
[AVSL]: http://bmc.github.com/avsl/
[API documentation]: api/
