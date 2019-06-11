package org.clapper.classutil

import scala.collection.convert.{AsJavaExtensions, AsScalaExtensions}

/** Compatibility definitions for Scala 2.13+ vs. Scala 2.12 and lesser.
  * This object is conceptually similar to `scala.collection.compat`.
  *
  * - For Scala 2.12 and earlier, it provides a type alias and compatibility
  *   functions for `LazyList`. For Scala 2.13 and greater, it's empty. Thus,
  *   all code can use `LazyList` throughout.
  * - It also provides the implicit objects `Ordering` objects for floats and
  *   doubles. For instance, it provides
  *   `grizzled.ScalaCompat.math.Ordering.Double.IeeeOrdering` and
  *   `grizzled.ScalaCompat.math.Ordering.Double.IeeeOrdering`. For Scala 2.12
  *   and earlier, these values are aliases for `scala.math.Ordering.Double`.
  *   For Scala 2.13 and greater, they map to their 2.13 counterparts (e.g.,
  *   `scala.math.Ordering.Double.IeeeOrdering`).
  */
package object ScalaCompat {

  val CollectionConverters: AsJavaExtensions with AsScalaExtensions =
    scala.jdk.CollectionConverters

  object math {
    object Ordering {
      object Double {
        implicit val IeeeOrdering: Ordering[Double] =
          scala.math.Ordering.Double.IeeeOrdering
        implicit val TotalOrdering: Ordering[Double] =
          scala.math.Ordering.Double.TotalOrdering
      }
      object Float {
        implicit val IeeeOrdering: Ordering[Float] =
          scala.math.Ordering.Float.IeeeOrdering
        implicit val TotalOrdering: Ordering[Float] =
          scala.math.Ordering.Float.TotalOrdering
      }
    }
  }}
