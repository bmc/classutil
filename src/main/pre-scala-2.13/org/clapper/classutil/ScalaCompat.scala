package org.clapper.classutil

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

  import scala.collection.convert.{DecorateAsJava, DecorateAsScala}

  val CollectionConverters: DecorateAsJava with DecorateAsScala =
    scala.collection.JavaConverters

  type LazyList[+T] = Stream[T]

  object LazyList {
    def empty[T]: LazyList[T] = Stream.empty[T]

    object #:: {
      @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
      def unapply[T](s: LazyList[T]): Option[(T, LazyList[T])] =
        if (s.nonEmpty) Some((s.head, s.tail)) else None
    }

    def from[T](coll: Iterator[T]): LazyList[T] = coll.toStream
  }

  object math {
    object Ordering {
      object Double {
        implicit val IeeeOrdering: Ordering[Double] =
          scala.math.Ordering.Double
        implicit val TotalOrdering: Ordering[Double] =
          scala.math.Ordering.Double
      }
      object Float {
        implicit val IeeeOrdering: Ordering[Float] =
          scala.math.Ordering.Float
        implicit val TotalOrdering: Ordering[Float] =
          scala.math.Ordering.Float
      }
    }
  }
}
