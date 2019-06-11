package org.clapper.classutil

/**
 * Takes a Scala `Map`, with `String` keys and object values, and generates
 * an object, with fields for each map value. Field that are, themselves,
 * `Map[String,Any]` objects can be recursively mapped, as well.
 */
trait MapToBeanMapper {
  /** Transform a map into a bean.
    *
    * @param map       the map
    * @param className name of the generated interface used to create a proxy
    * @param recurse   `true` to recursively map nested maps, `false` otherwise
    *
    * @return an instantiated object representing the map
    *
    * @deprecated Use makeBean() instead.
    */
  def makeObject(map: Map[String, Any],
                 className: String,
                 recurse: Boolean = true): AnyRef = {
    makeBean(map, className, recurse)
  }

  /** Transform a map into a bean.
    *
    * @param map       the map
    * @param className name of the generated interface used to create a proxy
    * @param recurse   `true` to recursively map nested maps, `false` otherwise
    *
    * @return an instantiated object representing the map
    */
  def makeBean(map: Map[String, Any],
               className: String,
               recurse: Boolean = true): AnyRef
}

/** Takes a Scala `Map`, with `String` keys and object values, and generates
  * a Java Bean object, with fields for each map value. Field that are,
  * themselves, `Map[String,Any]` objects can be recursively mapped, as
  * well. The map's keys are mapped to Java Bean `get` accessors. For
  * instance, a scalaGetter name "foo" is mapped to a method called `getFoo()`.
  *
  * The transformation results in an object that can only really be used
  * via reflection; however, that fits fine with some APIs that want to receive
  * Java Beans as parameters.
  *
  * There are some restrictions imposed on any map that is to be converted.
  *
  * - Only maps with string keys can be converted.
  * - The string keys must be valid Java identifiers.
  *
  * Here's a simple example:
  *
  * {{{
  * import org.clapper.classutil._
  *
  * val charList = List('1', '2', '3')
  *
  * val subMap = Map("sub1" -> 1, "sub2" -> 2)
  * val m =  Map("oneInt" -> 1,
  *              "twoFloat" -> 2f,
  *              "threeString" -> "three",
  *              "fourIntClass" -> classOf[Int],
  *              "fiveMap" -> subMap,
  *              "sixList" -> charList)
  * val obj = MapToBean(m)
  *
  * def call(methodName: String) = {
  *   val method = obj.getClass.getMethod(methodName)
  *   method.invoke(obj)
  * }
  *
  * val five = obj.getClass.getMethod("getFiveMap").invoke(obj)
  * println("getFiveMap returns " + five)
  * }}}
  *
  * That code will produce output like the following:
  * {{{
  * getFiveMap returns Map(getSub1 -> 1, getSub2 -> 2, sub1 -> 1, sub2 -> 2)
  * }}}
  */
object MapToBean extends ClassNameGenerator {
  val ClassNamePrefix = "org.clapper.classutil.MapBean"

  private val mapper = new org.clapper.classutil.asm.MapToBeanMapperImpl

  /**
   * Transform a map into an object. The class name will be generated,
   * will be in the `org.clapper.classutil` package, and will have
   * a class name prefix of `MapBean_`.
   *
   * @param map       the map
   * @param recurse   `true` to recursively map nested maps, `false` otherwise
   *
   * @return an instantiated object representing the map
   */
  def apply(map: Map[String, Any], recurse: Boolean = true): AnyRef =
    mapper.makeBean(map, newGeneratedClassName, recurse)

  /**
   * Transform a map into an object.
   *
   * @param map       the map
   * @param className the desired class name
   * @param recurse   `true` to recursively map nested maps, `false`
   *                  otherwise. Recursively mapped maps will have generated
   *                  class names.
   *
   * @return an instantiated object representing the map
   */
  def apply(map: Map[String, Any], className: String, recurse: Boolean): AnyRef =
    mapper.makeBean(map, className, recurse)
}
