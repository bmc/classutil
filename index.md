---
title: ClassUtil—A Scala-friendly, fast class-finder library (with extras)
layout: withTOC
---

## NOTICE

---

**This software is still under development. Links in this document may point
to web pages that are not yet written. The library, itself, still lacks a
few features. Documentation is sparse. Goblins run freely within, unhindered
by checkpoints or armies. Check this out at your own risk.**

**This notice will be removed when the software is finished and the beasties
are vanquished.**

---

## Introduction

The *org.clapper.classutil* (ClassUtil) library is a Scala package that
provides methods for locating and filter classes quickly, at runtime—more
quickly, in fact, than can be done with the JVM's runtime reflection
capabilities. Under the covers, ClassUtil uses the [ASM][] bytecode
library, though it can easily be extended to use a different byte code
library. ClassUtil loads and returns information about classes using an
efficient lazy iterator approach, which offers minimal startup penalty and
the ability to cut the traversal short.

*more coming*

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
[GitHub repository]: http://github.com/bmc/PROJECT
[GitHub]: http://github.com/bmc/
[downloads area]: http://github.com/bmc/PROJECT/downloads
[*clapper.org* Maven repository]: http://maven.clapper.org/org/clapper/
[Maven]: http://maven.apache.org/
[ASM]: http://asm.ow2.org/
[bmc@clapper.org]: mailto:bmc@clapper.org
