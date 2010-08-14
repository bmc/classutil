/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the names "clapper.org", "ClassUtil", nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
*/

package org.clapper.classutil

/**
 * Takes a Scala object, locates the Scala accessors (using simple heuristics),
 * and generates a new object with additional Java Bean `get` methods for the
 * accessors. A `ScalaObjectToBeanMapper` is an alternative to using
 * the `@BeanProperty` annotation on classes, so it is useful for mapping
 * case classes into Java Beans, or for mapping classes from other APIs into
 * Java Beans without having to extend them.
 *
 * The mapper uses the following heuristics to determine which fields to
 * map.
 *
 * First, it recognizes that any Scala `val` or `var` is really a getter method
 * returning some type. That is:
 *
 * {{{
 * val x: Int = 0
 * var y: Int = 10
 * }}}
 *
 * is compiled down to the equivalent of the following Java code:
 *
 * {{{
 * private int _x = 0;
 * private int _y = 10;
 *
 * public int x() { return _x; }
 * public int y() { return _y; }
 * public void y_$eq(int newY) { _y = newY; }
 * }}}
 *
 * So, the mapper looks for methods that take no parameters and return some
 * non-void (i.e., non-`Unit`) value. Then, from that set of methods, the
 * mapper discards:
 *
 * <ul>
 *   <li> Methods starting with `get`.
 *   <li> Methods that have a corresponsing `get` method. In the above example,
 *        if there's a `getX()` method that returns an `int`, the mapper will
 *        assume that it's the bean version of `x()`, and it will ignore `x()`.
 *   <li> Any method in `java.lang.Object`.
 *   <li> Any method in `scala.Product`.
 *   <li> Optionally, any method whose name matches regular expressions
 *        specified by the caller.
 * </ul>
 *
 * If there are any methods in the remaining set, then the mapper returns a
 * new wrapper object that contains Java Bean versions of those methods;
 * otherwise, the mapper returns the original Scala object. The resulting
 * bean delegates its calls to the original object, instead of capturing the
 * object's method values at the time the bean is called. That way, if the
 * underlying Scala object's methods return different values for each call,
 * the bean will reflect those changes.
 */
private[classutil] class ScalaObjectToBeanMapper
{
    import java.lang.reflect.{InvocationHandler, Modifier, Method, Proxy}

    // In addition to skipping methods with non-beanable signatures, the
    // bean-generation logic will also skip any methods that match these
    // regular expressions.
    private val SkipMethods = List("""^toString$""".r,
                                   """^productArity$""".r,
                                   """^productIterator$""".r,
                                   """^productElements$""".r,
                                   """^productPrefix$""".r,
                                   """^hashCode$""".r,
                                   """^get""".r,
                                   """^readResolve$""".r)
    private val NameGenerator = new ClassNameGenerator
    {
        val ClassNamePrefix = "ScalaBean"
    }

    /**
     * Transform a Scala object into a bean.
     *
     * @param obj       the Scala object
     * @param className the name to give the class
     * @param recurse   `true` to recursively map nested Scala objects,
     *                  `false` otherwise
     *
     * @return an instantiated bean representing the augmented Scala object,
     *         subject to the restrictions listed in the class documentation.
     */
    def makeBean(obj: Any, className: String, recurse: Boolean = true): AnyRef =
    {
        import asm.InterfaceMaker

        def skip(name: String) = SkipMethods.exists(_.findFirstIn(name) != None)

        def isBeanable(m: Method) =
        {
            ((m.getModifiers & Modifier.PUBLIC) != 0) &&
             (m.getReturnType.getName != "void") &&
             (m.getParameterTypes.length == 0) &&
             (! skip(m.getName))
        }

        def beanName(m: Method) =
        {
            val name = m.getName
            "get" + name.take(1).toUpperCase + name.drop(1)
        }

        // Get the set of bean methods, and create a map of names to methods.

        val methodMap = obj.asInstanceOf[AnyRef].
                            getClass.
                            getMethods.
                            filter(isBeanable _).
                            map(m => beanName(m) -> m).
                            toMap

        if (methodMap.size == 0)
        {
            // No mappable methods. Just use the original object.

            obj.asInstanceOf[AnyRef]
        }

        else
        {
            // methodMap is now a map of bean method names to Method
            // objects. Convert it to a sequence of (bean-name,
            // return-value) tuples, to generate the interface.

            val className = NameGenerator.newGeneratedClassName

            // NEED to handle recursion.

            val interfaceBytes = InterfaceMaker.makeInterface(
                methodMap.map(kv => (kv._1, kv._2.getReturnType)).toSeq,
                className
            ).toArray

            // Load the class we just generated.
            val classLoader = obj.asInstanceOf[AnyRef].getClass.getClassLoader
            val interface = ClassUtil.loadClass(classLoader,
                                                className,
                                                interfaceBytes)

            // Create a proxy that satisfies its calls from the original
            // object's set of methods.

            makeProxy(methodMap, obj, interface, classLoader)
        }
    }

    /* ---------------------------------------------------------------------- *\
                             * Private Methods
    \* ---------------------------------------------------------------------- */

    private def makeProxy(methodMap: Map[String, Method],
                          obj: Any,
                          interface: Class[_],
                          classLoader: ClassLoader): AnyRef =
    {
        val handler = new InvocationHandler
        {
            def invoke(proxy: Object,
                       method: Method,
                       args: Array[Object]): Object = 
            {
                // It could be an invocation of a method that isn't one we
                // generated. In that case, just delegate the call to the
                // original object.

                methodMap.getOrElse(method.getName, method).
                          invoke(obj, args: _*)
            }
        }

        Proxy.newProxyInstance(classLoader, List(interface).toArray, handler)
    }

}

/**
 * Takes a Scala object, locates the Scala accessors (using simple heuristics),
 * and generates a new object with additional Java Bean `get` methods for the
 * accessors. A `ScalaObjectToBeanMapper` is an alternative to using
 * the `@BeanProperty` annotation on classes, so it is useful for mapping
 * case classes into Java Beans, or for mapping classes from other APIs into
 * Java Beans without having to extend them.
 *
 * See the documentation for the `ScalaObjectToBeanMapper` trait for full
 * details.
 *
 * @see ScalaObjectToBeanMapper
 */
object ScalaObjectToBean extends ClassNameGenerator
{
    val ClassNamePrefix = "org.clapper.classutil.ScalaObjectBean"

    private val mapper = new ScalaObjectToBeanMapper

    /**
     * Transform a map into an object. The class name will be generated,
     * will be in the `org.clapper.classutil` package, and will have
     * a class name prefix of `ScalaObjectBean_`.
     *
     * @param obj       the Scala object
     * @param recurse   `true` to recursively map nested maps, `false` otherwise
     *
     * @return an instantiated object representing the map
     */
    def apply(obj: Any, recurse: Boolean = true): AnyRef =
        mapper.makeBean(obj, newGeneratedClassName, recurse)

    /**
     * Transform a map into an object. The class name will be generated,
     * will be in the `org.clapper.classutil` package, and will have
     * a class name prefix of `ScalaObjectToBean_`.
     *
     * @param obj       the Scala object
     * @param className the desired class name
     * @param recurse   `true` to recursively map nested maps, `false`
     *                  otherwise. Recursively mapped maps will have generated
     *                  class names.
     *
     * @return an instantiated object representing the map
     */
    def apply(obj: Any, className: String, recurse: Boolean): AnyRef =
        mapper.makeBean(obj, className, recurse)
}
