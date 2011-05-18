---
title: "ClassUtil: A Scala-friendly, fast class-finder library (with extras)"
layout: withTOC
---

# Introduction

The *org.clapper.classutil* (ClassUtil) library is a Scala package that
provides various class location and class generation capabilities, including:

* Methods to locate and filter classes quickly, at runtime--more quickly, in
  fact, than can be done with the JVM's runtime reflection capabilities.
* Methods for converting Scala maps into Java Beans, on the fly--which can be
  useful when generating data for use with APIs (e.g., template APIs) that
  accept Java Beans, but not maps.
* Methods that convert Scala objects into Java beans, without requiring
  the use of the `@JavaBean` annotation--especially useful when passing
  `case class` objects into Java Bean-aware APIs.

Under the covers, ClassUtil uses the [ASM][] bytecode library, though it
can easily be extended to use a different byte code library.

ClassUtil is fast for several reasons:

* A bytecode library, like [ASM][], loads compiled bytecode without using
  a JVM class loader. So, it avoids all the class loader's overhead.
* ClassUtil's class-finder methods load and return information about
  classes using an efficient lazy iterator, which offers minimal startup
  penalty and the ability to terminate the traversal before reading
  and loading all the elements.

# Requirements

## Compile-time

* ClassUtil requires a Scala 2.8 environment, because it uses the
  [continuations][] plug-in available in 2.8.
* Other compile-time requirements are automatically satisfied, if you
  use Maven or SBT. See below.

## Runtime requirements

ClassUtil uses the following libraries, which must be present in the
classpath at runtime:

* The main [ASM][] library (version 3), e.g., `asm-3.3.1.jar`
* The [ASM][] commons library (version 3), e.g., `asm-commons-3.3.1.jar`
* The [Grizzled Scala][] library
* The [Grizzled SLF4J][] library, for logging
* The [SLF4J][] API library, for logging (e.g., `slf4j-api-1.6.1.jar`)
* An SLF4J implementation, such as [Logback][] or [AVSL][], if you want
  logging.

# Installation

The easiest way to install the ClassUtil library is to download a
pre-compiled jar from the [Scala Tools Maven repository][]. However, you
can also get certain build tools to download it for you automatically.

## Installing with Maven

If you're using [Maven][], you can simply tell Maven to get ClassUtil
from the [Scala Tools Maven repository][]. The relevant pieces of
information are:

* Group ID: `org.clapper`
* Artifact ID: `classutil_2.9.0`
* Version: `0.3.6`
* Type: `jar`
* Repository: `http://www.scala-tools.org/repo-releases/`

Here's a sample Maven POM "dependency" snippet:


    <repositories>
      <repository>
        <id>scala-tools.org</id>
          <name>Scala-tools Maven2 Repository</name>
          <url>http://scala-tools.org/repo-releases</url>
      </repository>
    </repositories>

    <dependency>
      <groupId>org.clapper</groupId>
      <artifactId>classutil_2.9.0</artifactId>
      <version>0.3.6</version>
    </dependency>

Version 0.3.6 supports Scala 2.9.0, 2.8.1 and 2.8.0.

For more information on using Maven and Scala, see Josh Suereth's
[Scala Maven Guide][].

## Using with SBT

If you're using [SBT][] (the Simple Build Tool) to compile your code, you
can place the following lines in your project file (i.e., the Scala file in
your `project/build/` directory):

    val classutil = "org.clapper" %% "classutil" % "0.3.6"

**NOTE:**

1. The first doubled percent is *not* a typo. It tells SBT to treat
   ClassUtil as a cross-built library and automatically inserts the Scala
   version you're using into the artifact ID. Currently, it will *only*
   work if you are building with Scala 2.8.0 or Scala 2.8.1. See the
   [SBT cross-building][] page for details.
2. Prior to version 0.3, you also had to specify the location of the
   *clapper.org* custom Maven repository. As of version 0.3, however,
   work if you are building with Scala 2.8.1 or 2.8.0. See the
   [SBT cross-building][] page for details.

# Building from Source

You can also build ClassUtil from source. There are two ways to get the
source:

## Downloading a snapshot of the source

You can download a tarball or zip file of the source from the
[downloads page][].

## Source Code Repository

The source code for ClassUtil is maintained on [GitHub][]. To clone
the repository, run this command:

    git clone git://github.com/bmc/classutil.git

## Building

Building the library requires [SBT][]. Install SBT, as described at the SBT
web site. Then, assuming you have an `sbt` shell script (or .BAT file, for
Windows), run:

    sbt update

That command will pull down the external jars on which the ClassUtil
Library depends. After that step, build the library with:

    sbt +compile +package

The resulting jar files will be under the top-level `target` directory, in
subdirectories specific to each Scala version.

# Using ClassUtil

## Finding classes at runtime

ClassUtil is simple to use. The primary class in the library is the
`ClassFinder` class, which has a corresponding companion object that
contains utility methods. The following examples illustrate use of the
library.

### Getting information on all classes in the current class path

    import org.clapper.classutil._

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
abstract class is sometimes useful. For example, if you're implementing a
plugin capability, you may need to discover all concrete classes that
implement your plugin interface. The `ClassFinder` companion object
provides a special utility function for that:

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

**WARNING**: `concreteSubclasses` can chew up a lot of heap space
temporarily, if called with a large classpath. Either use a "focused"
classpath, or make sure you run with a large enough maximum heap.

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

## Generating Java Beans from Scala objects

### Overview

ClassUtil also supports two ways to generate beans, on the fly, from Scala
objects; these capabilities are useful when you have to interact with APIs
that require Java Beans, but you don't have the option or desire to mark
all the bean fields with Scala's `@BeanProperty` annotation.
([Case classes][] and final classes are two good examples.)

To this end, ClassUtil provides two solutions:

- `MapToBean`, which traverses a map and convert each name/value pair into a
  Java Beans `get` method.
- `ScalaObjectToBean`, which takes a Scala object, looks for methods that
  take no parameters and return a value, and generates an object with
  `get` methods for those methods.

Both approaches will, by default, recursively convert objects. (See below
for more details.)

### `MapToBean`

`MapToBean` takes, as input, a `Map` object and generates a Java Bean with
`get` methods for each key/value pair in the map. By default, `MapToBean`
recursively converts values that are, themselves, maps. That is, if the
value for a map key is, itself, a map, `MapToBean` will convert that map to
a bean, too. Recursive generation can be disabled, if desired.

The `MapToBean` Scala object contains the method that performs the
transformation.

    def apply(map: Map[String, Any], recurse: Boolean = true): AnyRef
    
The first parameter is the map that is to be converted to a Java Bean. The
second parameter (`recurse`) indicates whether or not nested maps should be
automatically converted; it defaults to `true`. The bean's class name is
automatically generated, though there's a version of the `apply` method
that allows you to specify your own class name. The method returns an
instance of the newly generated bean class.

There are a few restrictions imposed on any map that is to be converted.

* Only maps with string keys can be converted.
* The string keys must be valid Java identifiers.

#### An example

An example will help clarify this part of the API:

    import org.clapper.classutil.MapToBean

    val charList = List('a', 'b', 'c')

    val subMap = Map("sub1" -> 1, "sub2" -> 2)
    val map =  Map("int" -> 1,
                   "float" -> 2f,
                   "someString" -> "three",
                   "intClass" -> classOf[Int],
                   "subMap" -> subMap,
                   "list" -> charList)
    val obj = MapToBean(map)

    obj.getClass.getMethods.filter(_.getName startsWith "get").foreach(println _)

    def call(methodName: String) =
    {
        val method = obj.getClass.getMethod(methodName)
        method.invoke(obj)
    }

    println()
    println("getSubMap returns " + call("getSubMap"))

This example takes a map:

    val map =  Map("int" -> 1,
                   "float" -> 2f,
                   "someString" -> "three",
                   "intClass" -> classOf[Int],
                   "subMap" -> subMap,
                   "list" -> charList)

and produces a Java Bean that behaves like an instance of the following class:

    public class Bean1
    {
        public Integer getInt()
        {
            return 1;
        }

        public Float getFloat()
        {
            return 2f;
        }

        public Class getIntClass()
        {
            return Integer.class;
        }

        public Object getSubMap()
        {
            return MapToBean(subMap);
        }

        public scala.collection.immutable.$colon$colon getList()
        { 
            return charList;
        }
    }

The above Scala script produces the following output:

    public final java.lang.Integer $Proxy1.getInt()
    public final java.lang.Float $Proxy1.getFloat()
    public final $Proxy0 $Proxy1.getSubMap()
    public final java.lang.Class $Proxy1.getIntClass()
    public final java.lang.String $Proxy1.getSomeString()
    public final scala.collection.immutable.$colon$colon $Proxy1.getList()
    public static java.lang.Class java.lang.reflect.Proxy.getProxyClass(java.lang.ClassLoader,java.lang.Class[]) throws java.lang.IllegalArgumentException
    public static java.lang.reflect.InvocationHandler java.lang.reflect.Proxy.getInvocationHandler(java.lang.Object) throws java.lang.IllegalArgumentException
    public final native java.lang.Class java.lang.Object.getClass()

    getSubMap returns Map(getSub1 -> 1, getSub2 -> 2)
    keys=Set(intClass, float, someString, int, subMap, list)

Nested maps are automatically converted via `MapToBean`, unless `recurse` is
`false`.

### `ScalaObjectToBean`

`ScalaObjectToBean` takes, as input, a Scala object and generates a Java
Bean with `get` methods for each Scala accessor. `ScalaObjectToBean` is an
alternative to using the `@BeanProperty` annotation on classes, so it is
useful for mapping case classes into Java Beans, or for mapping classes
from other APIs into Java Beans without having to extend them.

`ScalaObjectToBean` uses the following heuristics to determine which fields
to map.

First, it recognizes that any Scala `val` or `var` is really a getter method
returning some type. That is, it knows that Scala compiles the following

    val x: Int = 0
    var y: Int = 10

down to the equivalent of the this Java code:

    private int _x = 0;
    private int _y = 10;

    public int x() { return _x; }
    public int y() { return _y; }
    public void y_$eq(int newY) { _y = newY; }

So, the mapper looks for methods that take no parameters and return some
non-void (i.e., non-`Unit`) value. Then, from that set of methods, the
mapper discards:

* Methods starting with `get`.
* Methods that have a corresponding `get` method. In the above example,
  if there's a `getX()` method that returns an `int`, the mapper will
  assume that it's the bean version of `x()`, and it will ignore `x()`.
* Any method in `java.lang.Object`.
* Any method in `scala.Product`.
* Methods that aren't public.

If there are any methods in the remaining set, then the mapper returns a
new wrapper object that contains Java Bean versions of those methods;
otherwise, the mapper returns the original Scala object. The resulting bean
delegates its calls to the original object, instead of capturing the
object's method values at the time the bean is called. That way, if the
underlying Scala object's methods return different values for each call,
the bean will reflect those changes. Also, the mapped class delegates
any methods it didn't convert back to the original object. For instance,
calling `toString` on the newly generated bean results in a call to the
original object's `toString` method.

By default, `ScalaObjectToBean` recursively converts methods that return
non-primitive, non-String values that. That is, if the value for a getter
method is a non-primitive, non-String object, `ScalaObjectToBean` will
generate a bean for that object, too. Recursive generation can be disabled,
if desired.

The `ScalaObjectToBean` Scala object contains the method that performs the
transformation.

    def apply(obj: Any, recurse: Boolean = true): AnyRef

The first parameter is the map that is to be converted to a Java Bean. The
second parameter (`recurse`) indicates whether or not nested objects should
be automatically converted; it defaults to `true`. The bean's class name is
automatically generated, though there's a version of the `apply` method
that allows you to specify your own class name. The method returns an
instance of the newly generated bean class.

#### An example

An example will help clarify this part of the API:

    import org.clapper.classutil.ScalaObjectToBean

    case class Foo(name: String, value: Int)
    case class Bar(name: String, foo: Foo)

    val foo = Foo("foo100", 100)
    val bar = Bar("bar1", foo)
    val beanFoo = ScalaObjectToBean(foo)
    val beanBar = ScalaObjectToBean(bar)

    println("beanFoo:")
    println("-" * 30)
    beanFoo.getClass.getMethods.filter(_.getName startsWith "get").foreach(println _)

    println("beanBar:")
    println("-" * 30)
    beanBar.getClass.getMethods.filter(_.getName startsWith "get").foreach(println _)

    def call(obj: AnyRef, methodName: String) =
    {
        val method = obj.getClass.getMethod(methodName)
        method.invoke(obj)
    }

    println()
    println("beanFoo.getName returns " + call(beanFoo, "getName"))
    println("beanFoo.getValue returns " + call(beanFoo, "getValue"))
    println("beanBar.getName returns " + call(beanBar, "getName"))
    val beanFoo2 = call(beanBar, "getFoo")
    println("beanBar.getFoo returns " + beanFoo2)
    println("beanBar.getFoo.getValue returns " + call(beanFoo2, "getValue"))
    

This example takes instances of two cases classes and maps them to beans.
Running it produces the following output:

    beanFoo:
    ------------------------------
    public final java.lang.String $Proxy3.getName()
    public final int $Proxy3.getValue()
    public final java.lang.String $Proxy3.getCopy$default$1()
    public final int $Proxy3.getCopy$default$2()
    public static java.lang.Class java.lang.reflect.Proxy.getProxyClass(java.lang.ClassLoader,java.lang.Class[]) throws java.lang.IllegalArgumentException
    public static java.lang.reflect.InvocationHandler java.lang.reflect.Proxy.getInvocationHandler(java.lang.Object) throws java.lang.IllegalArgumentException
    public final native java.lang.Class java.lang.Object.getClass()

    beanBar:
    ------------------------------
    public final java.lang.String $Proxy4.getName()
    public final java.lang.String $Proxy4.getCopy$default$1()
    public final java.lang.Object $Proxy4.getCopy$default$2()
    public final java.lang.Object $Proxy4.getFoo()
    public static java.lang.Class java.lang.reflect.Proxy.getProxyClass(java.lang.ClassLoader,java.lang.Class[]) throws java.lang.IllegalArgumentException
    public static java.lang.reflect.InvocationHandler java.lang.reflect.Proxy.getInvocationHandler(java.lang.Object) throws java.lang.IllegalArgumentException
    public final native java.lang.Class java.lang.Object.getClass()


### Caveats

The beans generated by `MapToBean` and `ScalaObjectToBean` can really only
be used via reflection. Their types (classes) are created on the fly, so
they cannot be imported ahead of time. However, a reflected bean works well
with APIs that expect such things.

### Under the covers

Internally, `MapToBean` and `ScalaObjectToBean` use [ASM][] to create a
Java interface from the map or object. They then use
[`java.lang.reflect.Proxy`](http://java.sun.com/javase/6/docs/api/java/lang/reflect/Proxy.html)
to create a dynamic implementation of the interface that satisfies the
`get` method calls directly from the original object.

This approach turns out to be simpler to implement (and, therefore, simpler
to reason about) than directly generating a class that serves up the map's
values.

## API Docs

The full Scaladoc API documentation is available [here][API documentation].

# Change log

The change log for all releases is [here][changelog].

# Author

Brian M. Clapper, [bmc@clapper.org][]

# Copyright and License

The ClassUtil Library is copyright &copy; 2010-2011 Brian M. Clapper and is
released under a [BSD License][].

# Patches

I gladly accept patches from their original authors. Feel free to email
patches to me or to fork the [GitHub repository][] and send me a pull
request. Along with any patch you send:

* Please state that the patch is your original work.
* Please indicate that you license the work to the ClassUtil project
  under a [BSD License][].

[BSD License]: license.html
[GitHub repository]: http://github.com/bmc/classutil
[GitHub]: http://github.com/bmc/
[downloads page]: http://github.com/bmc/classutil/downloads
[Scala Tools Maven repository]: http://www.scala-tools.org/repo-releases/
[Scala Maven Guide]: http://www.scala-lang.org/node/345
[Maven]: http://maven.apache.org/
[ASM]: http://asm.ow2.org/
[SBT]: http://code.google.com/p/simple-build-tool
[SBT cross-building]: http://code.google.com/p/simple-build-tool/wiki/CrossBuild
[bmc@clapper.org]: mailto:bmc@clapper.org
[continuations]: http://www.scala-lang.org/node/2096
[Grizzled Scala]: http://software.clapper.org/grizzled-scala/
[Grizzled SLF4J]: http://software.clapper.org/grizzled-slf4j/
[SLF4J]: http://slf4j.org/
[Logback]: http://logback.qos.ch/
[AVSL]: http://software.clapper.org/avsl/
[API documentation]: api/index.html
[Case classes]: http://www.scala-lang.org/node/107
[changelog]: CHANGELOG.html
