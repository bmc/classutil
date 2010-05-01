/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2010, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the names "clapper.org", "Novus", nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
*/

import sbt._

import scala.io.Source

import java.io.File
import grizzled.file.implicits._
import org.clapper.sbtplugins.MarkdownPlugin

/**
 * To build Novus via SBT.
 */
class Project(info: ProjectInfo)
extends DefaultProject(info)
with MarkdownPlugin
with posterous.Publish
{
    /* ---------------------------------------------------------------------- *\
                         Compiler and SBT Options
    \* ---------------------------------------------------------------------- */

    override def compileOptions = Unchecked :: super.compileOptions.toList
    override def parallelExecution = true // why not?

    // Disable cross-paths, since we're only building under one version.
    // This simplifies publishing and importing. See
    // http://groups.google.com/group/simple-build-tool/browse_thread/thread/973b5a2956b5ecbe

    override def disableCrossPaths = true


    /* ---------------------------------------------------------------------- *\
                             Various settings
    \* ---------------------------------------------------------------------- */

    val LocalLibDir = "local_lib"

    val sourceDocsDir = "src" / "docs"
    val targetDocsDir = "target" / "doc"
    val usersGuide = sourceDocsDir / "users-guide.md"
    val markdownFiles = (path(".") * "*.md") +++ usersGuide
    val markdownHtmlFiles = transformPaths(targetDocsDir,
                                           markdownFiles,
                                           {_.replaceAll("\\.md$", ".html")})
    val markdownSources = markdownFiles +++
                          (sourceDocsDir / "markdown.css") +++
                          (sourceDocsDir ** "*.js")

    val scalaVersionDir = "scala-" + buildScalaVersion

    /* ---------------------------------------------------------------------- *\
                       Managed External Dependencies

               NOTE: Additional dependencies are declared in
         project/plugins/Plugins.scala. (Declaring them there allows them
                       to be imported in this file.)
    \* ---------------------------------------------------------------------- */

    val scalaToolsRepo = "Scala-Tools Maven Repository" at 
        "http://scala-tools.org/repo-releases/"

    val newReleaseToolsRepository = "Scala Tools Repository" at
        "http://nexus.scala-tools.org/content/repositories/snapshots/"
    val scalatest = "org.scalatest" % "scalatest" %
        "1.0.1-for-scala-2.8.0.Beta1-with-test-interfaces-0.3-SNAPSHOT"

    val asm = "asm" % "asm" % "3.2"
    val asmCommons = "asm" % "asm-commons" % "3.2"

    val orgClapperRepo = "clapper.org Maven Repository" at
        "http://maven.clapper.org"
    val grizzled = "org.clapper" % "grizzled-scala" % "0.5.1"
    val grizzledSlf4j = "org.clapper" % "grizzled-slf4j" % "0.1"

    /* ---------------------------------------------------------------------- *\
                         Custom tasks and actions
    \* ---------------------------------------------------------------------- */

    // Create the target/docs directory
    lazy val makeTargetDocsDir = task 
    {
        FileUtilities.createDirectory(targetDocsDir, log)
    }

    // Generate HTML docs from Markdown sources
    lazy val htmlDocs = fileTask(markdownHtmlFiles from markdownSources)
    { 
        val markdownCSS = Some(sourceDocsDir / "markdown.css")
        def markdownWithTOC(src: Path, target: Path) =
            runMarkdown(src, target, true)
        def markdownWithoutTOC(src: Path, target: Path) =
            runMarkdown(src, target, false)

        markdownWithoutTOC("README.md", targetDocsDir / "README.html")
        markdownWithoutTOC("LICENSE.md", targetDocsDir / "LICENSE.html")
        None
    } 
    .dependsOn(makeTargetDocsDir)

    // Copy Markdown sources into target/docs
    lazy val markdownDocs = copyTask(markdownFiles, targetDocsDir)

    // Local doc production
    lazy val targetDocs = task {None} dependsOn(htmlDocs, markdownDocs)

    // Override the "doc" action to depend on additional doc targets
    override def docAction = super.docAction dependsOn(targetDocs)

    /* ---------------------------------------------------------------------- *\
                          Private Helper Methods
    \* ---------------------------------------------------------------------- */

    /**
     * Run Markdown to convert a source (Markdown) file to HTML.
     *
     * @param markdownSource  the path to the source file
     * @param targetHTML      the path to the output file
     * @param useToc          whether or not to include the table of contents
     */
    private def runMarkdown(markdownSource: Path, 
                            targetHTML: Path, 
                            useToc: Boolean) = 
    {

        import scala.xml.Comment

        val cssLines = fileLines(sourceDocsDir / "markdown.css")
        val css = <style type="text/css">{cssLines mkString ""}</style>
        val toc =
            if (useToc)
                <script text="text/javascript" src={"toc.js"}/>
            else
                new Comment("No table of contents Javascript")

        markdown(markdownSource, targetHTML, css :: toc :: Nil, log)
    }

    private def fileLines(path: Path): Iterator[String] =
        Source.fromFile(new File(path.absolutePath)).getLines

    private def transformPaths(targetDir: Path, 
                               paths: PathFinder,
                               transform: (String) => String): Iterable[Path] =
    {
        val justFileNames = paths.get.map(p => p.asFile.basename.getPath)
        val transformedNames = justFileNames.map(s => transform(s))
        transformedNames.map(s => targetDir / s)
    }
}
