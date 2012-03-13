// ---------------------------------------------------------------------------
// Basic settings

name := "classutil"

organization := "org.clapper"

version := "0.4.5"

licenses := Seq(
  "BSD" -> url("http://software.clapper.org/classutil/license.html")
)

homepage := Some(url("http://software.clapper.org/classutil/"))

description := "A library for fast runtime class-querying, and more"

scalaVersion := "2.9.1"

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-P:continuations:enable", "-deprecation", "-unchecked")

autoCompilerPlugins := true

libraryDependencies <<= (scalaVersion, libraryDependencies) { (ver, deps) =>
    deps :+ compilerPlugin("org.scala-lang.plugins" % "continuations" % ver)
}

crossScalaVersions := Seq(
  "2.9.1-1", "2.9.1", "2.9.0-1", "2.9.0", "2.8.2", "2.8.1", "2.8.0"
)

seq(lsSettings :_*)

(LsKeys.tags in LsKeys.lsync) := Seq("classes", "byte code")

(description in LsKeys.lsync) <<= description(d => d)

// ---------------------------------------------------------------------------
// ScalaTest dependendency

libraryDependencies <<= (scalaVersion, libraryDependencies) { (sv, deps) =>
    // Select ScalaTest version based on Scala version
    val scalatestVersionMap = Map("2.8.0"   -> ("scalatest_2.8.0", "1.3.1.RC2"),
                                  "2.8.1"   -> ("scalatest_2.8.1", "1.7.1"),
                                  "2.8.2"   -> ("scalatest_2.8.2", "1.7.1"),
                                  "2.9.0"   -> ("scalatest_2.9.0", "1.7.1"),
                                  "2.9.0-1" -> ("scalatest_2.9.0-1", "1.7.1"),
                                  "2.9.1"   -> ("scalatest_2.9.1", "1.7.1"),
                                  "2.9.1-1"   -> ("scalatest_2.9.1", "1.7.1"))
    val (scalatestArtifact, scalatestVersion) = scalatestVersionMap.getOrElse(
        sv, error("Unsupported Scala version: " + scalaVersion)
    )
    deps :+ "org.scalatest" % scalatestArtifact % scalatestVersion % "test"
}

// ---------------------------------------------------------------------------
// Other dependendencies

libraryDependencies ++= Seq(
    "asm" % "asm" % "3.3",
    "asm" % "asm-commons" % "3.3",
    "asm" % "asm-util" % "3.3",
    "org.clapper" %% "grizzled-scala" % "1.0.12",
    "org.clapper" %% "grizzled-slf4j" % "0.6.8"
)

// ---------------------------------------------------------------------------
// Publishing criteria

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

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
