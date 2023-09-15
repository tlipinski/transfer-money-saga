import sbt.addCompilerPlugin

ThisBuild / scalaVersion := "2.13.8"

ThisBuild / scalacOptions ++= Seq(
  "-language:higherKinds",
  "-Ymacro-annotations",
  "-deprecation"
)

ThisBuild / version := "1.0-SNAPSHOT"

ThisBuild / Docker / version := git.gitHeadCommit.value.map(_.take(8)).getOrElse("latest")

ThisBuild / assemblyMergeStrategy := {
  case "logback.xml" => MergeStrategy.first
  case x             =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

val fs2Kafka       = "com.github.fd4s"               %% "fs2-kafka"                      % "2.5.0"
val couchbaseJava  = "com.couchbase.client"           % "java-client"                    % "3.3.3"
val couchbaseTrans = "com.couchbase.client"           % "couchbase-transactions"         % "1.2.4"
val quicklens      = "com.softwaremill.quicklens"    %% "quicklens"                      % "1.8.8"
val sttp           = "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.7.6"
val scalatest      = "org.scalatest"                 %% "scalatest"                      % "3.2.13" % Test

val circe =
  Seq("circe-core", "circe-generic", "circe-generic-extras", "circe-parser").map("io.circe" %% _ % "0.14.2")

val http4s =
  Seq("http4s-dsl", "http4s-blaze-server", "http4s-blaze-client", "http4s-circe").map("org.http4s" %% _ % "1.0.0-M35")

val commonDeps = Seq(
  "org.typelevel" %% "log4cats-slf4j"  % "2.4.0",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "org.typelevel" %% "cats-effect"     % "3.3.14"
)

val doobie = Seq(
  "doobie-core",
  "doobie-hikari",
  "doobie-postgres",
  "doobie-postgres-circe"
).map("org.tpolecat" %% _ % "1.0.0-RC4")

lazy val transfers = project
  .in(file("transfers"))
  .settings(
    name                        := "transfers",
    libraryDependencies ++= commonDeps ++ circe ++ http4s :+ sttp :+ quicklens,
    Docker / dockerBaseImage    := "openjdk:8",
    Docker / dockerExposedPorts := Seq(8080)
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(outbox, saga, util, `database-pg`, consumer)

lazy val bank = project
  .in(file("bank"))
  .settings(
    name                     := "bank",
    libraryDependencies ++= commonDeps ++ circe :+ scalatest :+ quicklens,
    Docker / dockerBaseImage := "openjdk:8"
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(consumer, outbox, database, util)

lazy val outbox = project
  .in(file("outbox"))
  .settings(
    name                     := "outbox",
    libraryDependencies ++= commonDeps :+ fs2Kafka,
    Docker / dockerBaseImage := "openjdk:8"
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(`database-pg`)

lazy val necromant = project
  .in(file("necromant"))
  .settings(
    name                     := "necromant",
    libraryDependencies ++= commonDeps,
    Docker / dockerBaseImage := "openjdk:8"
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(consumer)

lazy val database = project
  .in(file("database"))
  .settings(
    name := "database",
    libraryDependencies ++= Seq(couchbaseJava, couchbaseTrans)
  )
  .dependsOn(util)

lazy val `database-pg` = project
  .in(file("database-pg"))
  .settings(
    name := "database-pg",
    libraryDependencies ++= doobie
  )
  .dependsOn(util)

lazy val consumer = project
  .in(file("consumer"))
  .settings(
    name := "consumer",
    libraryDependencies ++= commonDeps :+ fs2Kafka
  )
  .dependsOn(util)

lazy val saga = project
  .in(file("saga"))
  .settings(
    name := "saga",
    libraryDependencies ++= commonDeps ++ circe :+ scalatest :+ quicklens
  )
  .dependsOn(util)

lazy val `test-runner` = project
  .in(file("test-runner"))
  .settings(
    name := "test-runner",
    libraryDependencies ++= commonDeps ++ http4s :+ sttp,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
  .dependsOn(outbox, saga, util, `database-pg`, consumer)

lazy val util = project
  .in(file("util"))
  .settings(
    name := "util",
    libraryDependencies ++= commonDeps ++ circe
  )
