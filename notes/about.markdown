The ClassUtil library is a Scala library that locates and filters classes
quickly, using the small and fast [ASM][] bytecode library. ClassUtil
returns metadata about the classes in a lazy iterator (via the
continuations plugin in Scala 2.8), for efficiency and fast startup. The
package's home page is at <http://bmc.github.com/classutil>; please see
that page for complete details.

[ASM]: http://asm.ow2.org/
