import com.typesafe.sbt.packager.MappingsHelper._
import play.core.PlayVersion
import play.sbt.routes.RoutesKeys
import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import scala.language.postfixOps

Universal / mappings ++= directory(baseDirectory.value / "public")
// see https://stackoverflow.com/a/37180566

name := "eori-common-component-registration-frontend"

targetJvm := "jvm-11"

scalaVersion := "2.13.8"

majorVersion := 0

PlayKeys.devSettings := Seq("play.server.http.port" -> "6751")

val mongoDbVersion = "1.3.0"

val bootstrapVersion = "7.19.0"

lazy val allResolvers = resolvers ++= Seq(Resolver.jcenterRepo)

lazy val IntegrationTest = config("it") extend Test

val testConfig = Seq(IntegrationTest, Test)

lazy val microservice = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    scalacOptions ++= Seq(
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-Wunused:imports", // Warn if an import selector is not referenced.
      "-Wunused:privates", // Warn if a private member is unused.
      "-Wunused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Wunused:locals", // Warn if a local definition is unused.
      "-Wunused:explicits", // Warn if an explicit parameter is unused.
      "-Wunused:implicits", // Warn if an implicit parameter is unused.
    ),
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
      Test / testOptions := Seq(Tests.Filter(filterTestsOnPackageName("unit"))),
      Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      Test / fork := true,
      Test / unmanagedSourceDirectories := Seq((Test / baseDirectory).value / "test"),
      addTestReportOption(Test, "test-reports")
    )

lazy val integrationTestSettings =
  inConfig(IntegrationTest)(Defaults.testTasks) ++
    Seq(
      IntegrationTest / testOptions := Seq(Tests.Filters(Seq(filterTestsOnPackageName("integration")))),
      IntegrationTest / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      IntegrationTest / fork := false,
      IntegrationTest / parallelExecution := false,
      addTestReportOption(IntegrationTest, "int-test-reports")
    )

lazy val commonSettings: Seq[Setting[_]] = defaultSettings()

lazy val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq("uk.gov.hmrc.eoricommoncomponent.frontend.domain._"),
  RoutesKeys.routesImport += "uk.gov.hmrc.eoricommoncomponent.frontend.models._"
)

lazy val twirlSettings: Seq[Setting[_]] = Seq(
  TwirlKeys.templateImports ++= Seq(
    "uk.gov.hmrc.eoricommoncomponent.frontend.views.html._",
    "uk.gov.hmrc.eoricommoncomponent.frontend.domain._"
  )
)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys

  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := List(
      "<empty>",
      "Reverse.*",
      "uk\\.gov\\.hmrc\\.eoricommoncomponent\\.models\\.data\\..*",
      "uk\\.gov\\.hmrc\\.eoricommoncomponent\\.view.*",
      "uk\\.gov\\.hmrc\\.eoricommoncomponent\\.models.*",
      "uk\\.gov\\.hmrc\\.eoricommoncomponent\\.config.*",
      "logger.*\\(.*\\)",
      ".*(AuthService|BuildInfo|Routes|TestOnly).*"
    ).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

val compileDependencies = Seq(
  "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % bootstrapVersion,
  "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.13.0-play-28",
  "uk.gov.hmrc"       %% "domain"                        % "8.3.0-play-28",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % mongoDbVersion,
  "uk.gov.hmrc"       %% "emailaddress"                  % "3.8.0",
  "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "7.13.0-play-28"
)

val testDependencies = Seq(

"uk.gov.hmrc"       %% "bootstrap-test-play-28"    % bootstrapVersion % "test",
  "org.scalatest"          %% "scalatest"           % "3.2.15"            % "test,it",
  "com.typesafe.play"      %% "play-test"           % PlayVersion.current % "test,it",
  "org.scalatestplus.play" %% "scalatestplus-play"  % "5.1.0"             % "test,it",
  "com.github.tomakehurst"  % "wiremock-standalone" % "2.27.2"            % "test, it"
    exclude ("org.apache.httpcomponents", "httpclient") exclude ("org.apache.httpcomponents", "httpcore"),
  "org.scalacheck"      %% "scalacheck"              % "1.17.0"   % "test,it",
  "org.scalatestplus"   %% "scalacheck-1-15"         % "3.2.11.0" % "test,it",
  "org.jsoup"            % "jsoup"                   % "1.15.4"   % "test,it",
  "us.codecraft"         % "xsoup"                   % "0.3.6"    % "test,it",
  "org.mockito"          % "mockito-core"            % "5.2.0"    % "test,it",
  "org.scalatestplus"   %% "mockito-4-6"             % "3.2.15.0" % "test, it",
  "org.pegdown"          % "pegdown"                 % "1.6.0",
  "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % mongoDbVersion  % "test, it",
  "com.vladsch.flexmark" % "flexmark-all"            % "0.64.6"   % "test,it"
)

libraryDependencies ++= compileDependencies ++ testDependencies

lazy val silencerSettings: Seq[Setting[_]] = {
  val silencerVersion = "1.7.12"
  Seq(
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full)
    ),
    // silence all warnings on autogenerated files
    scalacOptions += "-P:silencer:pathFilters=target/.*",
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"
  )
}

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)
