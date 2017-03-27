
package org.clapper.classutil

import java.security.SecureRandom
import java.lang.reflect.{InvocationHandler, Method, Proxy}

import scala.language.existentials
import scala.util.Random

/** Contains the actual logic that maps a Scala object to a Java bean.
  *
  * See [[org.clapper.classutil.ScalaObjectToBean]] for documentation.
  */
private[classutil] class ScalaObjectToBeanMapper {
  private val NameGenerator = new ClassNameGenerator {
    val ClassNamePrefix = "org.clapper.classutil.ScalaObjectBean"
  }

  /** Wrap a Scala object in a bean.
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

  /** Wrap a Scala object in a bean.
    *
    * @param obj       the Scala object
    * @param className the name to give the class
    * @param recurse   `true` to recursively map nested Scala objects,
    *                  `false` otherwise
    *
    * @return an instantiated bean representing the augmented Scala object,
    *         subject to the restrictions listed in the class documentation.
    */
  def wrapInBean(obj: Any, className: String, recurse: Boolean): AnyRef = {

    // Get the set of bean methods, and create a map of names to methods.

    val cls = obj.asInstanceOf[AnyRef].getClass
    val candidateMethods = ClassUtil.scalaAccessorMethods(cls)
    val beanMethodMap = methodsForBean(cls)
    if (beanMethodMap.isEmpty) {
      // No mappable methods. Just use the original object.
      obj.asInstanceOf[AnyRef]
    }

    else {
      // methodMap is now a map of bean method names to Method
      // objects. Convert it to a sequence of (bean-name,
      // return-value) tuples, to generate the interface.

      val methodMap = beanMethodMap ++
                      candidateMethods.map(m => m.getName -> m).toMap
      val classLoader = this.getClass.getClassLoader
      generateBean(obj, methodMap, className, classLoader, recurse)
    }
  }

  /** Get a list of methods that should be generated for a bean.
    *
    * @param cls  the class to query
    *
    * @return a map of `(methodName -> method)` pairs
    */
  def methodsForBean(cls: Class[_]): Map[String, Method] = {
    val settersGetters = ClassUtil.scalaAccessorMethods(cls)
    val pubNotFinal = ClassUtil.nonFinalPublicMethods(cls)
    ((settersGetters ++ pubNotFinal).map { m => m.getName -> m } ++
     settersGetters.map { m => ClassUtil.beanName(m) -> m }).toMap
  }

  /** Generate a Java Bean-compliant interface for a class. All Scala getters
    * and setters are represented in the interface as both their Scala methods
    * and the Java Bean equivalents. All other methods in the class appear,
    * as is, in the interface.
    *
    * Generating an implementation for the interface (e.g., by using a
    * `java.lang.reflect.Proxy`) is the responsibility of the caller.
    *
    * @param cls         the class for which to generate the interface
    * @param ifaceName   the name to give the interface. If not specified,
    *                    one is generated.
    * @param methodMap   A map of (name -> method) pairs describing the
    *                    methods to generate/wrap. If empty (the default),
    *                    all existing public methods are represented in the
    *                    interface, and new Java getters are setters are added
    *                    as necessary.
    * @param classLoader The class loader to use. Defaults to the class
    *                    loader for this class.
    * @param recurse     Whether or not method results should also be wrapped
    *                    as beans. Defaults to true.
    *
    * @return the generated interface, which is loaded using the specified
    *         class loader.
    *
    * @see [[generateBean]]
    */
  def generateBeanInterface(
    cls:         Class[_],
    ifaceName:   String = "",
    methodMap:   Map[String, Method] = Map.empty[String, Method],
    classLoader: ClassLoader = getClass.getClassLoader,
    recurse:     Boolean = true
  ): Class[_] = {

    import asm.InterfaceMaker

    def returnTypeFor(method: Method) = {
      if (recurse && shouldWrapReturnType(method.getReturnType))
        classOf[Any]
      else
        method.getReturnType
    }

    val className = cls.getName

    val newName = if (ifaceName.isEmpty)
      NameGenerator.newGeneratedClassName
    else
      ifaceName

    val methods = if (methodMap.isEmpty) {
      val allMethods = ClassUtil
        .nonFinalPublicMethods(cls)
        .map { m => m.getName -> m }
      val beanMethods = ClassUtil
        .scalaAccessorMethods(cls)
        .map { m => ClassUtil.beanName(m) -> m }
      (allMethods ++ beanMethods).toMap
    }
    else
      methodMap

    val methodSeq = methods.map { case (name, method) =>
      (name, method.getParameterTypes, returnTypeFor(method))
    }.toSeq

    val interfaceBytes = InterfaceMaker.makeInterface(methodSeq, newName)

    // Load the class we just generated.

    ClassUtil.loadClass(classLoader, newName, interfaceBytes)
  }

  /** Generate a bean from an object. This method creates an on-the-fly
    * interface containing Java Bean methods and uses that interface to
    * create a `java.lang.reflect.Proxy` to handle the calls. If you just
    * want the interface, call [[generateBeanInterface]].
    *
    * @param obj         the object to wrap
    * @param methodMap   A map of (name -> method) pairs describing the
    *                    methods to generate/wrap. If empty (the default),
    *                    all existing public methods are represented in the
    *                    interface, and new Java getters are setters are added
    *                    as necessary.
    * @param className   The class name to use. If not specified, a random
    *                    one is generated.
    * @param classLoader The class loader to use. If not specified, this class's
    *                    class loader is used.
    * @param recurse     Whether or not method results should also be wrapped
    *                    as beans. Defaults to `true`.
    *
    * @return the generated bean
    */
  def generateBean(obj:         Any,
                   methodMap:   Map[String, Method],
                   className:   String = "",
                   classLoader: ClassLoader = getClass.getClassLoader,
                   recurse:     Boolean = true): AnyRef = {


    def functionFor(method: Method) = {
      val o = obj.asInstanceOf[AnyRef]
      if (recurse && shouldWrapReturnType(method.getReturnType)) {
        // Return a partial function which, when invoked with an
        // Option of arguments, will call the method, wrapping the
        // result in a bean wrapper.
        (a: Option[Array[Object]]) =>
          wrapInBean(call(o, method, a), recurse = true)
      }
      else {
        // Return a partial function which, when invoked with an
        // Option of arguments, will call the method, returning the
        // result directly.
        (a: Option[Array[Object]]) => call(o, method, a)
      }
    }

    // Create a proxy that satisfies its calls from the original
    // object's set of methods.

    makeProxy(methodMap.map { case (name, meth) => name -> functionFor(meth) },
              obj,
              generateBeanInterface(cls         = obj.getClass,
                                    ifaceName   = className,
                                    methodMap   = methodMap,
                                    classLoader = classLoader,
                                    recurse     = recurse),
              classLoader)
  }

  //--------------------------------------------------------------------------
  // Private methods
  //--------------------------------------------------------------------------

  /** Determine whether a return type should be wrapped as a bean.
    *
    * @param returnType the return type
    *
    * @return `true` or `false`
    */
  private def shouldWrapReturnType(returnType: Class[_]): Boolean = {
    (!ClassUtil.isPrimitive(returnType)) &&
      (returnType ne classOf[String])
  }

  /** Invoke a method on an object, optionally passing it arguments.
    */
  private def call(obj: AnyRef,
                   method: Method,
                   args: Option[Array[Object]]): AnyRef = {
    args match {
      case Some(a) => method.invoke(obj, a: _*)
      case None    => method.invoke(obj)
    }
  }

  /** Create the proxy object.
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
                        classLoader: ClassLoader): AnyRef = {
    val handler = new InvocationHandler {
      def invoke(proxy: Object,
                 method: Method,
                 args: Array[Object]): Object =  {
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
  * `ScalaObjectToBean` contains functions that allow you to map a Scala object
  * into a read-only Java bean or merely generate an interface for such a bean.
  * The functions take a Scala object or class (depending), locate the Scala
  * accessors (using simple heuristics defined in [[ClassUtil]]), and generate
  * a new interface or object with additional Java Bean get` and `set` methods
  * for the accessors.
  *
  * This kind of wrapping is an alternative to using the `@BeanProperty`
  * annotation on classes, so it is useful for mapping case classes into Java
  * Beans, or for mapping classes from other APIs into Java Beans without having
  * to extend them.
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
  * public void y_\$eq(int newY) { _y = newY; }
  * }}}
 *
  * So, the mapper looks for Scala getter methods that take no parameters
  * and return some non-void (i.e., non-`Unit`) value, and it looks for
  * Scala setter methods that take one parameter, return void (`Unit`) and
  * have names ending in "_\$eq". Then, from that set of methods, the mapper
  * discards:
  *
  * <ul>
  * -  Methods starting with "get"
  * -  Methods that have a corresponding "get" method. In the above example,
  *        if there's a `getX()` method that returns an `int`, the mapper will
  *        assume that it's the bean version of `x()`, and it will ignore `x()`.
  * -  Methods that aren't public.
  * -  Any method in `java.lang.Object`.
  * -  Any method in `scala.Product`.
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
object ScalaObjectToBean {
  private val mapper = new ScalaObjectToBeanMapper

  /** Transform an object into an object. The class name will be generated,
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

  /** Transform an object into an object. The class name will be generated,
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


  /** Generate a Java Bean-compliant interface for a class.w
    *
    * Generating an implementation for the interface (e.g., by using a
    * `java.lang.reflect.Proxy`) is the responsibility of the caller.
    *
    * @param cls         the class for which to generate the interface
    * @param ifaceName   the name to give the interface. If not specified,
    *                    one is generated.
    * @param classLoader The class loader to use. Defaults to the class
    *                    loader for this class.
    * @param recurse     Whether or not method results should also be wrapped
    *                    as beans. Defaults to true.
    *
    * @return the generated interface, which is loaded using the specified
    *         class loader.
    */
  def generateBeanInterface(
    cls:         Class[_],
    ifaceName:   String = "",
    classLoader: ClassLoader = getClass.getClassLoader,
    recurse:     Boolean = true
  ): Class[_] = {
    val mapper = new ScalaObjectToBeanMapper()
    mapper.generateBeanInterface(
      cls         = cls,
      ifaceName   = ifaceName,
      methodMap   = mapper.methodsForBean(cls),
      classLoader = classLoader,
      recurse     = recurse
    )
  }

}

