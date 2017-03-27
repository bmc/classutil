package org.clapper.classutil

/**
  * Class name generator. Mix in, to get class name generation capabilities.
  */
private[classutil] trait ClassNameGenerator {
  val ClassNamePrefix: String

  private[classutil] def newGeneratedClassName =
    ClassNamePrefix + "$" + uniqueSuffix

  private def uniqueSuffix =
    java.util.UUID.randomUUID.toString.replace('-', '_')
}
