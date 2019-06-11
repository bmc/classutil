classutil: Fast class finder utilities, plus some extras
========================================================

[![Build Status](https://travis-ci.org/bmc/classutil.svg?branch=master)](https://travis-ci.org/bmc/classutil)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.clapper/classutil_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.clapper/classutil_2.11)

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

Please see the full documentation on the [library's home page][] for all the
gory details, including caveats.

ClassUtil is copyright &copy; 2010-2019 [Brian M. Clapper][].
 
- Versions prior to 1.5.0 were licensed under the
  [3-Clause BSD License](https://opensource.org/licenses/BSD-3-Clause).

- Version 1.5.0 and on are licensed under the
  [Apache License, version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

[library's home page]: http://software.clapper.org/classutil
[ASM]: http://asm.ow2.org/
[Brian M. Clapper]: mailto:bmc@clapper.org
