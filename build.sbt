// ---------------------------------------------------------------------------
// Basic settings

name := "classutil"

organization := "org.clapper"

version := "1.5.1"

licenses := Seq(
  "Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")
)

homepage := Some(url("http://software.clapper.org/classutil/"))

description := "A library for fast runtime class-querying, and more"

scalaVersion := "2.13.0"

crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0")

// For compatibility between 2.13.0 and prior versions, we need version-specific
// compatibility code.
unmanagedSourceDirectories in Compile += {
  val sourceDir = (sourceDirectory in Compile).value
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 13 => sourceDir / "scala-2.13"
    case _                       => sourceDir / "pre-scala-2.13"
  }
}

// --------------------------------------------------------------------------
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
// Dependendencies

val asmVersion = "9.2"

libraryDependencies ++= Seq(
  "org.ow2.asm"             % "asm"                     % asmVersion,
  "org.ow2.asm"             % "asm-commons"             % asmVersion,
  "org.ow2.asm"             % "asm-util"                % asmVersion,
  "org.clapper"            %% "grizzled-scala"          % "4.9.3",
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.0.0",
  "org.scalatest"          %% "scalatest"               % "3.0.8" % Test
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
