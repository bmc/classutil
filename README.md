classutil: Fast class finder utilities, plus some extras
========================================================

## Introduction

The *org.clapper.classutil* (ClassUtil) library is a Scala package that
provides various class location and class generation capabilities, including:

* Methods to locate and filter classes quickly, at runtime—more quickly, in
  fact, than can be done with the JVM's runtime reflection capabilities.
* Methods for converting Scala maps into Java Beans, on the fly—which can be
  useful when generating data for use with APIs (e.g., template APIs) that
  accept Java Beans, but not maps.

Under the covers, ClassUtil uses the [ASM][] bytecode library, though it
can be extended to use a different byte code library.

ClassUtil is fast for several reasons:

* A bytecode library like [ASM][] loads compiled bytecode without using
  a JVM class loader. So, it avoids all the class loader's overhead.
* ClassUtil's class-finder methods load and return information about
  classes using an efficient lazy iterator, which offers minimal startup
  penalty and the ability to cut the traversal short.

ClassUtil is copyright &copy; 2010-2011 [Brian M. Clapper][] and is released
under a new BSD license.

[library's home page]: http://bmc.github.com/classutil
[ASM]: http://asm.ow2.org/
[Brian M. Clapper]: mailto:bmc@clapper.org
