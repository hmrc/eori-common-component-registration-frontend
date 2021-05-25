import com.typesafe.sbt.packager.MappingsHelper._
import play.core.PlayVersion
import play.sbt.routes.RoutesKeys
import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import scala.language.postfixOps

mappings in Universal ++= directory(baseDirectory.value / "public")
// see https://stackoverflow.com/a/37180566

name := "eori-common-component-frontend"

targetJvm := "jvm-1.8"

scalaVersion := "2.12.12"

majorVersion := 0

PlayKeys.devSettings := Seq("play.server.http.port" -> "6750")

lazy val allResolvers = resolvers ++= Seq(Resolver.jcenterRepo)

lazy val IntegrationTest = config("it") extend Test

val testConfig = Seq(IntegrationTest, Test)

lazy val microservice = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    commonSettings,
    unitTestSettings,
    integrationTestSettings,
    playSettings,
    allResolvers,
    scoverageSettings,
    twirlSettings,
    TwirlKeys.templateImports += "uk.gov.hmrc.eoricommoncomponent.frontend.models._",
    silencerSettings
  )

def filterTestsOnPackageName(rootPackage: String): String => Boolean = {
  testName => testName startsWith rootPackage
}

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      testOptions in Test := Seq(Tests.Filter(filterTestsOnPackageName("unit"))),
      testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in Test := true,
      unmanagedSourceDirectories in Test := Seq((baseDirectory in Test).value / "test"),
      addTestReportOption(Test, "test-reports")
    )

lazy val integrationTestSettings =
  inConfig(IntegrationTest)(Defaults.testTasks) ++
    Seq(
      testOptions in IntegrationTest := Seq(Tests.Filters(Seq(filterTestsOnPackageName("integration")))),
      testOptions in IntegrationTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in IntegrationTest := false,
      parallelExecution in IntegrationTest := false,
      addTestReportOption(IntegrationTest, "int-test-reports")
    )

lazy val commonSettings: Seq[Setting[_]] = publishingSettings ++ defaultSettings()

lazy val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq("uk.gov.hmrc.eoricommoncomponent.frontend.domain._"),
  RoutesKeys.routesImport += "uk.gov.hmrc.eoricommoncomponent.frontend.models._"
)

lazy val twirlSettings: Seq[Setting[_]] = Seq(
  TwirlKeys.templateImports ++= Seq("uk.gov.hmrc.eoricommoncomponent.frontend.views.html._", "uk.gov.hmrc.eoricommoncomponent.frontend.domain._")
)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys

  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := List("<empty>",
      "Reverse.*",
      "uk\\.gov\\.hmrc\\.customs\\.rosmfrontend\\.models\\.data\\..*",
      "uk\\.gov\\.hmrc\\.customs\\.rosmfrontend\\.view.*",
      "uk\\.gov\\.hmrc\\.customs\\.rosmfrontend\\.models.*",
      "uk\\.gov\\.hmrc\\.customs\\.rosmfrontend\\.config.*",
      "logger.*\\(.*\\)",
      ".*(AuthService|BuildInfo|Routes|TestOnly).*").mkString(";"),
    ScoverageKeys.coverageMinimum := 86,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

val compileDependencies = Seq(
  "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "5.1.0",
  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.6.0-play-27",
  "uk.gov.hmrc" %% "domain" % "5.11.0-play-27",
  "uk.gov.hmrc" %% "mongo-caching" % "7.0.0-play-27",
  "uk.gov.hmrc" %% "emailaddress" % "3.5.0",
  "uk.gov.hmrc" %% "logback-json-logger" % "5.1.0",
  "com.typesafe.play" %% "play-json-joda" % "2.7.3",
  "uk.gov.hmrc" %% "play-language" % "4.12.0-play-27",
  "uk.gov.hmrc" %% "play-ui" % "9.1.0-play-27",
  "org.webjars.npm" % "accessible-autocomplete" % "2.0.3"
)


val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test,it",
  "com.typesafe.play" %% "play-test" % PlayVersion.current % "test,it",
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % "test,it",
  "com.github.tomakehurst" % "wiremock-standalone" % "2.23.2" % "test, it"
    exclude("org.apache.httpcomponents", "httpclient") exclude("org.apache.httpcomponents", "httpcore"),
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test,it",
  "org.jsoup" % "jsoup" % "1.11.3" % "test,it",
  "us.codecraft" % "xsoup" % "0.3.1" % "test,it",
  "org.mockito" % "mockito-core" % "3.0.0" % "test,it",
  "org.pegdown" % "pegdown" % "1.6.0",
  "uk.gov.hmrc" %% "reactivemongo-test" % "5.0.0-play-27" % "test, it"
)

libraryDependencies ++= compileDependencies ++ testDependencies

lazy val silencerSettings: Seq[Setting[_]] = {
  val silencerVersion = "1.7.0"
  Seq(
    libraryDependencies ++= Seq(compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full)),
    // silence all warnings on autogenerated files
    scalacOptions += "-P:silencer:pathFilters=target/.*",
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"
  )
}
