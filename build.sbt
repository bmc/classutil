// ---------------------------------------------------------------------------
// Basic settings

name := "classutil"

organization := "org.clapper"

version := "0.4.1"

scalaVersion := "2.8.1"

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-P:continuations:enable", "-deprecation", "-unchecked")

autoCompilerPlugins := true

libraryDependencies <<= (scalaVersion, libraryDependencies) { (ver, deps) =>
    deps :+ compilerPlugin("org.scala-lang.plugins" % "continuations" % ver)
}

crossScalaVersions := Seq("2.9.0-1", "2.9.0", "2.8.1", "2.8.0")

// ---------------------------------------------------------------------------
// ScalaTest dependendency

libraryDependencies <<= (scalaVersion, libraryDependencies) { (sv, deps) =>
    // Select ScalaTest version based on Scala version
    val scalatestVersionMap = Map("2.8.0"   -> ("scalatest", "1.3"),
                                  "2.8.1"   -> ("scalatest_2.8.1", "1.5.1"),
                                  "2.9.0"   -> ("scalatest_2.9.0", "1.6.1"),
                                  "2.9.0-1" -> ("scalatest_2.9.0-1", "1.6.1"))
    val (scalatestArtifact, scalatestVersion) = scalatestVersionMap.getOrElse(
        sv, error("Unsupported Scala version: " + scalaVersion)
    )
    deps :+ "org.scalatest" % scalatestArtifact % scalatestVersion % "test"
}

fork in Test := true

fork in Compile := true

// ---------------------------------------------------------------------------
// Posterous-SBT

libraryDependencies <<= (sbtVersion, scalaVersion, libraryDependencies) { (sbtv, scalav, deps) =>
    if (scalav == "2.8.1")
        deps :+ "net.databinder" %% "posterous-sbt" % ("0.3.0_sbt" + sbtv)
    else
        deps
}

(name in Posterous) := "ClassUtil"

// ---------------------------------------------------------------------------
// Other dependendencies

libraryDependencies ++= Seq(
    "asm" % "asm" % "3.3" % "provided",
    "asm" % "asm-commons" % "3.3" % "provided",
    "asm" % "asm-util" % "3.3" % "provided",
    "org.clapper" %% "grizzled-scala" % "1.0.7",
    "org.clapper" %% "grizzled-slf4j" % "0.6"
)

// ---------------------------------------------------------------------------
// Publishing criteria

publishTo <<= version {(v: String) =>
    val nexus = "http://nexus.scala-tools.org/content/repositories/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/") 
    else                             Some("releases"  at nexus + "releases/")
}

publishMavenStyle := true

credentials += Credentials(Path.userHome / "src" / "mystuff" / "scala" /
                           "nexus.scala-tools.org.properties")
