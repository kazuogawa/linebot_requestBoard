name := """linebot_requestBoard"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test",
  "mysql" % "mysql-connector-java" % "5.1.40",
  "joda-time" % "joda-time" % "2.9.9",
  "org.joda" % "joda-convert" % "1.8",
  "com.h2database"  %  "h2"                                % "1.4.193",
  "org.scalikejdbc" %% "scalikejdbc"                     % "2.5.1",
  "org.scalikejdbc" %% "scalikejdbc-config"             % "2.5.1",
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.5.1",
  "org.scalikejdbc" %% "scalikejdbc-play-fixture"      % "2.5.1",
  "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % "2.2.+",
  "org.scalikejdbc" %% "scalikejdbc-play-dbapi-adapter" % "2.5.1"
)

includeFilter in (Assets, LessKeys.less) := "*.less"
excludeFilter in (Assets, LessKeys.less) := "_*.less"

javaOptions in Test += "-Dconfig.file=conf/test.conf"