package org.clapper.classutil

import java.lang.reflect.Method

import org.scalatest.{FlatSpec, Matchers}

/** Base trait for all testers.
  */
trait BaseSpec extends FlatSpec with Matchers {

  /** Enrichment for java.lang.Class to make certain things a wee bit easier.
    *
    * @param cls  the class to wrap
    */
  implicit class EnrichedClass(cls: Class[_]) {

    /** Load the method for a given method name. If there are multiple methods
      * with the same name, the first one is returned. (In these tests, it's
      * not likely the testers will be trying to tests overloaded methods.)
      *
      * @param name the method name
      *
      * @return `Some(Method)` if found, `None` if not.
      */
    def methodForName(name: String): Option[Method] = {
      cls.getMethods.find(_.getName == name)
    }

    /** Determine whether the class has a particular named method.
      *
      * @param name  the name of the method
      *
      * @return `true` if at least one method exists with the specified name,
      *        `false`
      */
    def hasMethod(name: String): Boolean = methodForName(name).isDefined

    /** Invoke a method by name. If there are multiple methods with the same
      * name, the first one is used.
      *
      * @param obj     the object on which to invoke the method
      * @param method  the method name
      * @param args    the arguments, if any, to pass to the method
      *
      * @return whatever the method returns
      */
    def invokeOn(obj: AnyRef, method: String, args: AnyRef*): Option[AnyRef] = {
      methodForName(method).map { _.invoke(obj, args: _*) }
    }
  }
}
