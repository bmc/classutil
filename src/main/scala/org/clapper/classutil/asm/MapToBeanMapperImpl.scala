package org.clapper.classutil.asm

import org.clapper.classutil._

import java.lang.reflect.{Method, Proxy, InvocationHandler}

/** Takes a Scala `Map`, with `String` keys and object values, and generates
  * a Java Bean object, with fields for each map value. Field that are,
  * themselves, `Map[String,Any]` objects can be recursively mapped, as
  * well.
  *
  * The transformation results in an object that can only really be used
  * via reflection; however, that fits fine with some APIs that want to receive
  * Java Beans as parameters.
  *
  * There are some restrictions imposed on the map. First, each scalaGetter must be
  * a valid Java identifier. Second, the keys are mapped to Java Bean
  * `get` accessors. For instance, a scalaGetter name "foo" is mapped to a method
  * called `getFoo()`.
  */
private[classutil] class MapToBeanMapperImpl extends MapToBeanMapper {
  // ----------------------------------------------------------------------
  // Public Methods
  // ----------------------------------------------------------------------

  /**
   * Transform a map into a bean.
   *
   * @param map       the map
   * @param className name of the generated interface used to create a proxy
   * @param recurse   `true` to recursively map nested maps, `false` otherwise
   *
   * @return an instantiated object representing the map
   */
  def makeBean(map:       Map[String, Any],
               className: String,
               recurse:   Boolean = true): AnyRef = {
    // Strategy: Create an interface, load it, and generate a proxy that
    // implements the interface dynamically. The proxy handler resolves
    // references from the map.

    // Methods for each field, including bean methods.
    def transformValueIfMap(value: Any) = {
      if (recurse && ClassUtil.isOfType[Map[String,Any]](value))
        makeObject(value.asInstanceOf[Map[String,Any]],
                   MapToBean.newGeneratedClassName,
                   recurse)
      else
        value
    }

    // If we're recursing, then first map any value that is, itself, a
    // Map[String,Any].

    val tuples1 = map.map { case (k, v) => k -> transformValueIfMap(v) }
    val newMap = Map(tuples1.toList: _*)

    // Map the keys to method names, with the same values as the existing
    // map.
    val beanGetters = newMap.keys.map { k =>
      Util.scalaGetterNameToBeanName(k) -> newMap(k)
    }

    // Generate both bean-style getters and Scala-style getters.
    val methodNameMap = Map((beanGetters ++ newMap).toArray: _*)

    // Create the interface bytes. We need a map of names to return types
    // here.
    val methodSpecs = methodNameMap.map { case (k, v) =>
      (k, InterfaceMaker.NoParams, v.asInstanceOf[AnyRef].getClass)
    }

    val interfaceBytes = InterfaceMaker.makeInterface(
      methodSpecs.toSeq, className
    )

    // Load the class we just generated.

    val classLoader = map.getClass.getClassLoader
    val interface = ClassUtil.loadClass(
      classLoader, className, interfaceBytes
    )

    makeProxy(methodNameMap, map, interface, classLoader)
  }

  // ----------------------------------------------------------------------
  // Private Methods
  // ----------------------------------------------------------------------

  private def makeProxy(methodNameMap: Map[String, Any],
                        originalMap:   Map[String, Any],
                        interface:     Class[_],
                        classLoader:   ClassLoader): AnyRef = {
    val handler = new InvocationHandler {
      def invoke(proxy: Object,
                 method: Method,
                 args: Array[Object]): Object = {
        // It could be an invocation of a method that isn't one of the
        // ones we created from the map. In that case, just delegate
        // the method call to the original map.

        methodNameMap.get(method.getName) match {
          case None    => method.invoke(methodNameMap, args: _*)
          case Some(v) => v.asInstanceOf[AnyRef]
        }
      }
    }

    Proxy.newProxyInstance(classLoader, Array(interface), handler)
  }
}
