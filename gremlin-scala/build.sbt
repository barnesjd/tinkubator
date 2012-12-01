name := "gremlin-scala"

version := "2.2.0-SNAPSHOT"

scalaVersion := "2.9.1"

// Disabling checksums because three of the fortytwo.net sha1's don't match
checksums in update := Nil

resolvers ++= Seq(
    "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
    "Typesafe Plugins" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/",
    "TinkerPop Maven2 Repository" at "http://tinkerpop.com/maven2",
    "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Aduna" at "http://repo.aduna-software.org/maven2/releases/",
    "fortytwo.net Maven repository" at "http://fortytwo.net/maven2"
)

libraryDependencies ++= Seq(
    "com.tinkerpop.gremlin" % "gremlin-java" % tinkerpopVersion,
    "com.tinkerpop" % "pipes" % tinkerpopVersion,
    "org.scala-lang" % "jline" % "2.9.1",
    "org.scala-lang" % "scala-compiler" % "2.9.1",
    "com.tinkerpop.blueprints" % "blueprints-graph-jung" % tinkerpopVersion,
    "com.tinkerpop.blueprints" % "blueprints-graph-sail" % tinkerpopVersion,
    "com.tinkerpop.blueprints" % "blueprints-neo4j-graph" % tinkerpopVersion,
    "com.tinkerpop.blueprints" % "blueprints-neo4jbatch-graph" % tinkerpopVersion,
    "com.tinkerpop.blueprints" % "blueprints-orient-graph" % tinkerpopVersion,
    "com.tinkerpop.blueprints" % "blueprints-dex-graph" % tinkerpopVersion,
    "com.tinkerpop.blueprints" % "blueprints-sail-graph" % tinkerpopVersion,
    "com.tinkerpop.blueprints" % "blueprints-rexster-graph" % tinkerpopVersion,
    "com.tinkerpop.gremlin" % "gremlin-test" % tinkerpopVersion % "test",
    "org.scalatest" %% "scalatest" % "1.8" % "test",
    "junit" % "junit" % "4.10" % "test",
    "com.novocode" % "junit-interface" % "0.8" % "test->default"
)
