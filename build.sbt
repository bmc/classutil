// ---------------------------------------------------------------------------
// Basic settings

name := "classutil"

organization := "org.clapper"

version := "1.4.0"

licenses := Seq(
  "BSD New" -> url("http://software.clapper.org/classutil/license.html")
)

homepage := Some(url("http://software.clapper.org/classutil/"))

description := "A library for fast runtime class-querying, and more"

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.8")

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

autoCompilerPlugins := true

//tests need to be forked since the classpath of sbt test only contains a jar file of sbt
Test / fork := true

wartremoverErrors in (Compile, compile) ++= Seq(
  Wart.ArrayEquals,
  // Wart.Any,
  // Wart.AnyVal,
  // This library is loaded with casting, because of how it has to interface
  // with ASM. AsInstanceOf cannot be enabled.
  // Wart.AsInstanceOf
  Wart.EitherProjectionPartial,
  Wart.Enumeration,
  Wart.ExplicitImplicitTypes,
  Wart.FinalCaseClass,
  Wart.FinalVal,
  // Wart.IsInstanceOf,
  Wart.JavaConversions,
  Wart.LeakingSealed,
  Wart.MutableDataStructures,
  // Wart.NonUnitStatements,
  // Wart.Nothing,
  Wart.Null,
  Wart.Option2Iterable,
  Wart.OptionPartial,
  Wart.PublicInference,
  Wart.Return,
  Wart.StringPlusAny,
  Wart.Throw,
  Wart.TraversableOps,
  Wart.TryPartial,
  Wart.Var,
  Wart.While
)

// ---------------------------------------------------------------------------
// ScalaTest dependendency

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

// ---------------------------------------------------------------------------
// Other dependendencies

val asmVersion = "7.1"

libraryDependencies ++= Seq(
    "org.ow2.asm" % "asm" % asmVersion,
    "org.ow2.asm" % "asm-commons" % asmVersion,
    "org.ow2.asm" % "asm-util" % asmVersion,
    "org.clapper" %% "grizzled-scala" % "4.4.2"
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
