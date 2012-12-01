import sbt._
import Keys._

object GremlinScalaBuild extends Build {
  val tinkerpopVersion = "2.1.0" //SettingKey[String]("tinkerpop-version", "Version for the tinkerpop dependencies")
  
  lazy val root = Project(id = "gremlin-scala", base = file("."))
}
