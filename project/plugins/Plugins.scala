import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info)
{
    val t_repo = "t_repo" at "http://tristanhunt.com:8081/content/groups/public"
    val snug_repo = "uk-releases" at "http://www2.ph.ed.ac.uk/maven2/"

    val posterous = "net.databinder" % "posterous-sbt" % "0.1.6"
    val markdown = "org.clapper" % "sbt-markdown-plugin" % "0.3"
}
