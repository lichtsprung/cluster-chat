import sbt._
import Keys._

object OpenplexusChat extends Build {

  lazy val projectSettings = Defaults.defaultSettings ++ Seq(
    name := "Openplexus Chat",
    version := "0.1",
    organization := "net.openplexus",
    scalaVersion := "2.10.0-RC2",
    fork in run := true,
    libraryDependencies += Dependency.libAkkaActor,
    libraryDependencies += Dependency.libAkkaCamel,
    libraryDependencies += Dependency.libAkkaRemote,
    libraryDependencies += Dependency.libAkkaTestkit,
    libraryDependencies += Dependency.libAkkaCluster,
    resolvers += Resolvers.sonatypeSnapshotRepo,
    resolvers += Resolvers.typesafeReleaseRepo,
    resolvers += Resolvers.typesafeSnapshotRepo
  )

  lazy val root = Project(id = "root", base = file("."), settings = projectSettings)
}


object Resolvers {
  val typesafeReleaseRepo = "Typesafe Snapshot Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
  val typesafeSnapshotRepo = "Typesafe Release Repository" at "http://repo.typesafe.com/typesafe/releases/"
  val sonatypeSnapshotRepo = "Scala Tools Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
}


object Dependency {
  val libAkkaActor = "com.typesafe.akka" %% "akka-actor" % "2.2-SNAPSHOT" cross CrossVersion.full withSources() withJavadoc()
  val libAkkaRemote = "com.typesafe.akka" %% "akka-remote" % "2.2-SNAPSHOT" cross CrossVersion.full withSources() withJavadoc()
  val libAkkaCluster = "com.typesafe.akka" %% "akka-cluster-experimental" % "2.2-SNAPSHOT" cross CrossVersion.full withSources() withJavadoc()
  val libAkkaTestkit = "com.typesafe.akka" %% "akka-testkit" % "2.2-SNAPSHOT" cross CrossVersion.full withSources() withJavadoc()
  val libAkkaCamel = "com.typesafe.akka" %% "akka-camel" % "2.2-SNAPSHOT" cross CrossVersion.full withSources() withJavadoc()
}
