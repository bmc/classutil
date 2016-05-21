// ---------------------------------------------------------------------------
// Basic settings

name := "classutil"

organization := "org.clapper"

version := "1.0.10"

licenses := Seq(
  "BSD New" -> url("http://software.clapper.org/classutil/license.html")
)

homepage := Some(url("http://software.clapper.org/classutil/"))

description := "A library for fast runtime class-querying, and more"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0-M4")

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

autoCompilerPlugins := true

// ---------------------------------------------------------------------------
// Helpers

// Take a dependency and map its cross-compiled version, creating a new
// dependency. Temporary, until Scala 2.12 is for real.
/*
def mappedDep(dep: sbt.ModuleID): sbt.ModuleID = {
  dep cross CrossVersion.binaryMapped {
    case v if v startsWith "2.12" => "2.11"
    case v => v.split("""\.""").take(2).mkString(".")
  }
}
*/

// ---------------------------------------------------------------------------
// ScalaTest dependendency

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

// ---------------------------------------------------------------------------
// Other dependendencies

libraryDependencies ++= Seq(
    "org.ow2.asm" % "asm" % "5.1",
    "org.ow2.asm" % "asm-commons" % "5.1",
    "org.ow2.asm" % "asm-util" % "5.1",
    "org.clapper" %% "grizzled-scala" % "1.6.1"
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
