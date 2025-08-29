val Http4sVersion = "0.23.30"
val CirceVersion = "0.14.14"
val MunitVersion = "1.1.1"
val LogbackVersion = "1.5.18"
val MunitCatsEffectVersion = "2.1.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.adewusi",
    name := "roundup",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.16",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "com.typesafe" % "config" % "1.4.2",
      "com.github.pureconfig" %% "pureconfig" % "0.17.4",
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect" % MunitCatsEffectVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Runtime
    ),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.13.3" cross CrossVersion.full
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value.apply(x)
    }
  )

lazy val integration = (project in file("integration"))
  .dependsOn(root)
  .settings(
    scalaVersion := "2.13.16",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % MunitVersion,
      "org.typelevel" %% "munit-cats-effect" % MunitCatsEffectVersion
    )
  )
