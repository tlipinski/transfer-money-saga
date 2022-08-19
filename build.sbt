val scalaVer = "2.13.6"

val kafkaClients   = "org.apache.kafka"      % "kafka-clients"          % "2.8.0"
val catsEffect     = "org.typelevel"        %% "cats-effect"            % "3.1.1"
val typesafeConfig = "com.typesafe"          % "config"                 % "1.4.1"
val fs2Core        = "co.fs2"               %% "fs2-core"               % "3.0.4"
val fs2Kafka       = "com.github.fd4s"      %% "fs2-kafka"              % "2.1.0"
val couchbaseJava  = "com.couchbase.client"  % "java-client"            % "3.1.5"
val couchbaseScala = "com.couchbase.client" %% "scala-client"           % "1.2.1"
val couchbaseTrans = "com.couchbase.client"  % "couchbase-transactions" % "1.2.1"
val scalaLogging   = "org.typelevel"        %% "log4cats-slf4j"         % "2.1.1"
val logbackClassic = "ch.qos.logback"        % "logback-classic"        % "1.2.3"
val contextApplied = "org.augustjune"       %% "context-applied"        % "0.1.4"
val sttp           =
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.3.5"
val utest          = "com.lihaoyi"    %% "utest"      % "0.7.10" % Test
val scalacheck     = "org.scalacheck" %% "scalacheck" % "1.14.1" % Test
val scalatest      = "org.scalatest"  %% "scalatest"  % "3.2.9"  % Test
val circeSeq       = Seq(
  "circe-core",
  "circe-generic",
  "circe-generic-extras",
  "circe-parser"
).map("io.circe" %% _ % "0.14.1")
val http4sSeq      = Seq(
  "http4s-dsl",
  "http4s-blaze-server",
  "http4s-blaze-client",
  "http4s-circe"
).map("org.http4s" %% _ % "1.0.0-M23")

lazy val transfers = project
  .in(file("transfers"))
  .settings(
    name := "transfers",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      kafkaClients,
      typesafeConfig,
      fs2Core,
      fs2Kafka,
      couchbaseJava,
      scalaLogging,
      logbackClassic,
      sttp
    ),
    libraryDependencies ++= http4sSeq,
    libraryDependencies ++= circeSeq,
    addCompilerPlugin(contextApplied)
  )
  .dependsOn(publisher, saga, outbox)

lazy val bank = project
  .in(file("bank"))
  .settings(
    name := "bank",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations",
      "-deprecation"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      kafkaClients,
      typesafeConfig,
      fs2Core,
      fs2Kafka,
      couchbaseJava,
      couchbaseTrans,
      scalaLogging,
      logbackClassic
    ),
    libraryDependencies ++= http4sSeq,
    libraryDependencies ++= circeSeq,
    libraryDependencies += utest,
    libraryDependencies += scalatest,
    libraryDependencies += scalacheck,
    addCompilerPlugin(contextApplied)
  )
  .dependsOn(publisher, outbox)

lazy val `bank-pg` = project
  .in(file("bank-pg"))
  .settings(
    name := "bank-pg",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations",
      "-deprecation"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      kafkaClients,
      typesafeConfig,
      fs2Core,
      fs2Kafka,
      scalaLogging,
      logbackClassic,
      "org.tpolecat" %% "doobie-core"     % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-specs2"   % "1.0.0-RC1"
    ),
    libraryDependencies ++= http4sSeq,
    libraryDependencies ++= circeSeq,
    libraryDependencies += utest,
    libraryDependencies += scalatest,
    libraryDependencies += scalacheck,
    addCompilerPlugin(contextApplied)
  )
  .dependsOn(publisher)

lazy val `bank-sc` = project
  .in(file("bank-sc"))
  .settings(
    name := "bank",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations",
      "-deprecation"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      kafkaClients,
      typesafeConfig,
      fs2Core,
      fs2Kafka,
      couchbaseScala,
      couchbaseTrans,
      scalaLogging,
      logbackClassic
    ),
    libraryDependencies ++= http4sSeq,
    libraryDependencies ++= circeSeq,
    libraryDependencies += utest,
    libraryDependencies += scalatest,
    libraryDependencies += scalacheck,
    addCompilerPlugin(contextApplied)
  )
  .dependsOn(publisher)

lazy val `bank-tx` = project
  .in(file("bank-tx"))
  .settings(
    name := "bank",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations",
      "-deprecation"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      kafkaClients,
      typesafeConfig,
      fs2Core,
      fs2Kafka,
      couchbaseJava,
      couchbaseTrans,
      scalaLogging,
      logbackClassic
    ),
    libraryDependencies ++= http4sSeq,
    libraryDependencies ++= circeSeq,
    libraryDependencies += utest,
    libraryDependencies += scalatest,
    libraryDependencies += scalacheck,
    addCompilerPlugin(contextApplied)
  )
  .dependsOn(publisher, outbox)

lazy val `bank-outbox` = project
  .in(file("bank-outbox"))
  .settings(
    name := "bank-outbox",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations",
      "-deprecation"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      kafkaClients,
      typesafeConfig,
      fs2Core,
      fs2Kafka,
      couchbaseJava,
      couchbaseTrans,
      scalaLogging,
      logbackClassic
    ),
    libraryDependencies ++= http4sSeq,
    libraryDependencies ++= circeSeq,
    libraryDependencies += utest,
    libraryDependencies += scalatest,
    libraryDependencies += scalacheck,
    addCompilerPlugin(contextApplied)
  )
  .dependsOn(publisher, outbox)

lazy val outbox = project
  .in(file("outbox"))
  .settings(
    name := "outbox",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations",
      "-deprecation"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      kafkaClients,
      typesafeConfig,
      fs2Core,
      fs2Kafka,
      couchbaseJava,
      couchbaseTrans,
      scalaLogging,
      logbackClassic
    ),
    libraryDependencies ++= http4sSeq,
    libraryDependencies ++= circeSeq,
    libraryDependencies += utest,
    libraryDependencies += scalatest,
    libraryDependencies += scalacheck,
    addCompilerPlugin(contextApplied)
  )
  .dependsOn(publisher)

lazy val `bank-necromant` = project
  .in(file("bank-necromant"))
  .settings(
    name := "bank-necromant",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations",
      "-deprecation"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      kafkaClients,
      typesafeConfig,
      fs2Core,
      fs2Kafka,
      scalaLogging,
      logbackClassic
    ),
    libraryDependencies ++= http4sSeq,
    libraryDependencies ++= circeSeq,
    libraryDependencies += utest,
    libraryDependencies += scalatest,
    libraryDependencies += scalacheck,
    addCompilerPlugin(contextApplied)
  )
  .dependsOn(publisher)

lazy val publisher = project
  .in(file("publisher"))
  .settings(
    name := "publisher",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      typesafeConfig,
      fs2Core,
      fs2Kafka,
      scalaLogging,
      logbackClassic
    ),
    libraryDependencies ++= circeSeq,
    libraryDependencies += utest,
    addCompilerPlugin(contextApplied)
  )

lazy val saga = project
  .in(file("saga"))
  .settings(
    name := "saga",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    libraryDependencies ++= Seq(
      catsEffect,
      scalaLogging,
      logbackClassic,
      utest
    ),
    libraryDependencies ++= circeSeq,
    addCompilerPlugin(contextApplied)
  )
