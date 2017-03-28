package org.clapper.classutil

/** Utility methods for internal use.
  */
private[classutil] object Util {

  // In addition to skipping methods with non-beanable signatures, the
  // bean-generation logic will also skip any methods that match these
  // regular expressions.
  val SkipMethods = Vector(
    """^toString$""".r,
    """^productArity$""".r,
    """^productIterator$""".r,
    """^productElements$""".r,
    """^productPrefix$""".r,
    """^hashCode$""".r,
    """^get""".r,
    """^set""".r,
    """^copy.*""".r,
    """\$outer$""".r,
    """^readResolve$""".r
  )

  // Methods to hide (i.e., not pass through to generated proxies).
  val HideMethods = Vector(
    """^.*\${2,}""".r
  )

  /** Take a Scala getter and map it to a Java Bean method name. Aborts if
    * called with a getter name that isn't a valid Java identifier.
    *
    * @param scalaGetter Scala getter name
    *
    * @return the Java Bean getter name
    */
  def scalaGetterNameToBeanName(scalaGetter: String): String = {
     require(
       scalaGetter.forall(Character.isJavaIdentifierPart),
       s"""Map scalaGetter "$scalaGetter" is not a valid Java identifier."""
     )

    "get" + scalaGetter.take(1).toUpperCase + scalaGetter.drop(1)
  }

  /** Determine whether a method should be skipped.
    *
    * @param name the method name
    *
    * @return `true` or `false`
    */
  def skipMethod(name: String): Boolean = {
    SkipMethods.exists(_.findFirstIn(name).isDefined)
  }

  /** Determine whether a method should be hidden (i.e., not exposed at all
    * in generated beans).
    *
    * @param name the method name
    *
    * @return `true` or `false`
    */
  def hideMethod(name: String): Boolean = {
    HideMethods.exists(_.findFirstIn(name).isDefined)
  }
}
