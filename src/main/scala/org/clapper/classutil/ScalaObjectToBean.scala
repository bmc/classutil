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
 * Contains the actual logic that maps a Scala object to a Java bean.
 *
 * See `ScalaObjecToBean` for documentation.
 */
private[classutil] class ScalaObjectToBeanMapper
{
    import java.lang.reflect.{InvocationHandler,
                              InvocationTargetException,
                              Modifier,
                              Method,
                              Proxy}

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
                                   """^set""".r,
                                   """^readResolve$""".r)
    // Matches setter methods.
    private val SetterPattern = """_\$eq$""".r
    private val SetterRemove  = SetterPattern

    private val NameGenerator = new ClassNameGenerator
    {
        val ClassNamePrefix = "org.clapper.classutil.ScalaObjectBean"
    }

    /**
     * Wrap a Scala object in a bean.
     *
     * @param obj       the Scala object
     * @param recurse   `true` to recursively map nested Scala objects,
     *                  `false` otherwise
     *
     * @return an instantiated bean representing the augmented Scala object,
     *         subject to the restrictions listed in the class documentation.
     */
    def wrapInBean(obj: Any, recurse: Boolean): AnyRef =
        wrapInBean(obj, NameGenerator.newGeneratedClassName, recurse)

    /**
     * Wrap a Scala object in a bean.
     *
     * @param obj       the Scala object
     * @param className the name to give the class
     * @param recurse   `true` to recursively map nested Scala objects,
     *                  `false` otherwise
     *
     * @return an instantiated bean representing the augmented Scala object,
     *         subject to the restrictions listed in the class documentation.
     */
    def wrapInBean(obj: Any, className: String, recurse: Boolean): AnyRef =
    {
        def skip(name: String) = SkipMethods.exists(_.findFirstIn(name) != None)

        def isGetter(m: Method) =
        {
             (m.getReturnType.getName != "void") &&
             (m.getParameterTypes.length == 0)
        }

        def isSetter(m: Method) =
        {
            (m.getReturnType.getName == "void") &&
            (m.getParameterTypes.length == 1) &&
            (SetterPattern.findFirstIn(m.getName) != None)
        }

        def isBeanable(m: Method) =
        {
            ((m.getModifiers & Modifier.PUBLIC) != 0) &&
            (! skip(m.getName)) &&
            (isSetter(m) || isGetter(m))
        }

        def canCopyMethod(m: Method) =
        {
            val modifiers = m.getModifiers
            ((modifiers & Modifier.PUBLIC) != 0) &&
            ((modifiers & Modifier.FINAL) == 0)
        }

        def beanName(m: Method) =
        {
            val name = m.getName

            def prefix(s: String) = s.take(1).toUpperCase + s.drop(1)

            if (isGetter(m))
                "get" + prefix(name)

            else
                "set" + prefix(SetterRemove.replaceFirstIn(name, ""))
        }

        // Get the set of bean methods, and create a map of names to methods.

        val candidateMethods = obj.asInstanceOf[AnyRef].
                               getClass.
                               getMethods.
                               filter(canCopyMethod _)
        val beanMethodMap = candidateMethods.
                            filter(isBeanable _).
                            map(m => beanName(m) -> m).
                            toMap

        if (beanMethodMap.size == 0)
        {
            // No mappable methods. Just use the original object.

            obj.asInstanceOf[AnyRef]
        }

        else
        {
            // methodMap is now a map of bean method names to Method
            // objects. Convert it to a sequence of (bean-name,
            // return-value) tuples, to generate the interface.

            val methodMap = beanMethodMap ++
                            candidateMethods.map(m => m.getName -> m).toMap

            val classLoader = this.getClass.getClassLoader
            generateBean(obj, methodMap, className, classLoader, recurse)
        }
    }

    /* ---------------------------------------------------------------------- *\
                             * Private Methods
    \* ---------------------------------------------------------------------- */

    /**
     * Generate the bean.
     *
     * @param obj         the object to wrap
     * @param methodMap   A map of (name -> method) pairs describing the
     *                    methods to generate/wrap
     * @param className   The class name to use
     * @param classLoader The class loader to use
     * @param recurse     Whether or not method results should also be wrapped
     *                    as beans.
     *
     * @return the generated bean
     */
    private def generateBean(obj: Any,
                             methodMap: Map[String, Method],
                             className: String,
                             classLoader: ClassLoader,
                             recurse: Boolean): AnyRef =
    {
        import asm.InterfaceMaker

        def beanWrappableClass(returnType: Class[_]): Boolean =
        {
            (!ClassUtil.isPrimitive(returnType)) &&
            (returnType ne classOf[String])
        }

        def functionFor(method: Method) =
        {
            val o = obj.asInstanceOf[AnyRef]
            if (recurse && beanWrappableClass(method.getReturnType))
            {
                // Return a partial function which, when invoked with an
                // Option of arguments, will call the method, wrapping the
                // result in a bean wrapper.
                (a: Option[Array[Object]]) =>
                    wrapInBean(call(o, method, a), true)
            }
            else
            {
                // Return a partial function which, when invoked with an
                // Option of arguments, will call the method, returning the
                // result directly.
                (a: Option[Array[Object]]) => call(o, method, a)
            }
        }

        def returnTypeFor(method: Method) =
        {
            if (recurse && beanWrappableClass(method.getReturnType))
                classOf[Any]
            else
                method.getReturnType
        }

        val methodSeq = methodMap.map
        {
            kv =>

            (kv._1, kv._2.getParameterTypes, returnTypeFor(kv._2))
        }.toSeq

        val interfaceBytes = InterfaceMaker.makeInterface(methodSeq,
                                                          className).toArray

        // Load the class we just generated.

        val interface = ClassUtil.loadClass(classLoader,
                                            className,
                                            interfaceBytes)

        // Create a proxy that satisfies its calls from the original
        // object's set of methods.

        makeProxy(methodMap.map(kv => (kv._1 -> functionFor(kv._2))).toMap,
                                obj,
                                interface,
                                classLoader)
    }

    /**
     * Invoke a method on an object, optionally passing it arguments.
     */
    private def call(obj: AnyRef,
                     method: Method,
                     args: Option[Array[Object]]): AnyRef =
    {
        args match
        {
            case Some(a) => method.invoke(obj, a: _*)
            case None    => method.invoke(obj)
        }
    }

    /**
     * Create the proxy object.
     *
     * @param methods     A map of method names to parameter types. If there
     *                    are no parameter types for a method, the value
     *                    associated with the method should be None.
     * @param obj         The object to which to delegate the method calls
     * @param interface   The generated interface class to be implemented
     * @param classLoader The class loader to use
     *
     * @return the proxy instance
     */
    private def makeProxy(methods: Map[String, Option[Array[Object]] => AnyRef],
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

                def delegate(args: Option[Array[Object]]): AnyRef =
                    call(obj.asInstanceOf[AnyRef], method, args)

                val func = methods.getOrElse(method.getName, delegate _)
                if (method.getParameterTypes.length == 0)
                    func(None)
                else
                    func(Some(args))
            }
        }

        Proxy.newProxyInstance(classLoader, List(interface).toArray, handler)
    }
}

/**

 * `ScalaObjectToBean` maps a Scala object into a read-only Java bean. It
 * takes a Scala object, locates the Scala accessors (using simple
 * heuristics), and generates a new object with additional Java Bean `get`
 * and `set` methods for the accessors. `ScalaObjectToBean` is an
 * alternative to using the `@BeanProperty` annotation on classes, so it is
 * useful for mapping case classes into Java Beans, or for mapping classes
 * from other APIs into Java Beans without having to extend them.
 *
 * `ScalaObjectToBean` uses the following heuristics to determine which fields
 * to map.
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
 * So, the mapper looks for Scala getter methods that take no parameters
 * and return some non-void (i.e., non-`Unit`) value, and it looks for
 * Scala setter methods that take one parameter, return void (`Unit`) and
 * have names ending in "_$eq". Then, from that set of methods, the mapper
 * discards:
 *
 * <ul>
 *   <li> Methods starting with "get"
 *   <li> Methods that have a corresponding "get" method. In the above example,
 *        if there's a `getX()` method that returns an `int`, the mapper will
 *        assume that it's the bean version of `x()`, and it will ignore `x()`.
 *   <li> Methods that aren't public.
 *   <li> Any method in `java.lang.Object`.
 *   <li> Any method in `scala.Product`.
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
object ScalaObjectToBean
{
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
        mapper.wrapInBean(obj, recurse)

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
        mapper.wrapInBean(obj, className, recurse)
}
