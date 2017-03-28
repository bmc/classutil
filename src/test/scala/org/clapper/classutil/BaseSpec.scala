package org.clapper.classutil

import java.lang.reflect.Method

import org.scalatest.{FlatSpec, Matchers}

/** Base trait for all testers.
  */
trait BaseSpec extends FlatSpec with Matchers {
  implicit class EnrichedClass(cls: Class[_]) {
    def methodForName(name: String): Option[Method] = {
      cls.getMethods.find(_.getName == name)
    }

    def hasMethod(name: String): Boolean = methodForName(name).isDefined

    def invokeOn(obj: AnyRef, method: String, args: AnyRef*): Option[AnyRef] = {
      methodForName(method).map { _.invoke(obj, args: _*) }
    }
  }
}
