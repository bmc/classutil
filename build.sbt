// ---------------------------------------------------------------------------
// Basic settings

name := "classutil"

organization := "org.clapper"

version := "1.0.6"

licenses := Seq(
  "BSD New" -> url("http://software.clapper.org/classutil/license.html")
)

homepage := Some(url("http://software.clapper.org/classutil/"))

description := "A library for fast runtime class-querying, and more"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.7")

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

autoCompilerPlugins := true

// ---------------------------------------------------------------------------
// ScalaTest dependendency

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies <<= (scalaVersion, libraryDependencies) { (sv, deps) =>
  // ScalaTest still uses the (deprecated) scala.actors API.
  deps :+ "org.scala-lang" % "scala-actors" % sv % "test"
}

// ---------------------------------------------------------------------------
// Other dependendencies

libraryDependencies ++= Seq(
    "org.ow2.asm" % "asm" % "5.0.2",
    "org.ow2.asm" % "asm-commons" % "5.0.2",
    "org.ow2.asm" % "asm-util" % "5.0.2",
    "org.clapper" %% "grizzled-scala" % "1.4.0",
    "org.clapper" %% "grizzled-slf4j" % "1.0.2"
)

// ---------------------------------------------------------------------------
// Publishing criteria

// Don't set publishTo. The Bintray plugin does that automatically.

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>git@github.com:bmc/classutil.git/</url>
    <connection>scm:git:git@github.com:bmc/classutil.git</connection>
  </scm>
  <developers>
    <developer>
      <id>bmc</id>
      <name>Brian Clapper</name>
      <url>http://www.clapper.org/bmc</url>
    </developer>
  </developers>
)
