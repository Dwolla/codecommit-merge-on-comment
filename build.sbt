import sbtassembly.Log4j2MergeStrategy

lazy val buildSettings = Seq(
  organization := "com.dwolla",
  homepage := Some(url("https://github.com/Dwolla/codecommit-merge-on-comment")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  startYear := Option(2019),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  resolvers ++= Seq(
    Resolver.bintrayRepo("dwolla", "maven"),
    Resolver.sonatypeRepo("releases"),
  ),
  assemblyMergeStrategy in assembly := {
    case PathList(ps @ _*) if ps.last == "io.netty.versions.properties" =>
      MergeStrategy.concat
    case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" =>
      Log4j2MergeStrategy.plugincache
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  assemblyJarName in assembly := normalizedName.value + ".jar"
)

lazy val `codecommit-merge-on-comment` = (project in file("."))
  .settings(buildSettings: _*)
  .settings(
    description := "Listen to CodeCommit PR Comment feeds, and merge pull requests when someone comments approvingly",
    libraryDependencies ++= {
      val fs2AwsVersion = "2.0.0-M5"
      Seq(
        "software.amazon.awssdk" % "codecommit" % "2.7.18",
        "org.typelevel" %% "cats-core" % "2.6.1",
        "io.circe" %% "circe-optics" % "0.12.0",
        "com.dwolla" %% "fs2-aws-java-sdk2" % fs2AwsVersion,
        "com.dwolla" %% "fs2-aws-lambda-io-app" % fs2AwsVersion,
        "com.dwolla" %% "testutils-scalatest-fs2" % "2.0.0-M3" % Test,
        "com.ironcorelabs" %% "cats-scalatest" % "3.0.0" % Test,
      )
    },
  )
