import sbt.*

object AppDependencies {
  import play.core.PlayVersion

  val mongoDbVersion = "1.3.0"
  val bootstrapVersion = "7.22.0"

  val compileDependencies = Seq(
    "org.typelevel"     %% "cats-core"                     % "2.8.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.13.0-play-28",
    "uk.gov.hmrc"       %% "domain"                        % "8.3.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % mongoDbVersion,
    "uk.gov.hmrc"       %% "emailaddress"                  % "3.8.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "7.19.0-play-28",
    "uk.gov.hmrc"       %% "internal-auth-client-play-28"  % "1.6.0"
  )

  val testDependencies = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-28"    % bootstrapVersion % "test",
    "org.scalatest"          %% "scalatest"           % "3.2.15"            % "test,it",
    "com.typesafe.play"      %% "play-test"           % PlayVersion.current % "test,it",
    "org.scalatestplus.play" %% "scalatestplus-play"  % "5.1.0"             % "test,it",
    "com.github.tomakehurst"  % "wiremock-standalone" % "2.27.2"            % "test, it",
    "org.scalacheck"      %% "scalacheck"              % "1.17.0"   % "test,it",
    "org.scalatestplus"   %% "scalacheck-1-15"         % "3.2.11.0" % "test,it",
    "org.jsoup"            % "jsoup"                   % "1.15.4"   % "test,it",
    "us.codecraft"         % "xsoup"                   % "0.3.6"    % "test,it",
    "org.mockito"          % "mockito-core"            % "5.2.0"    % "test,it",
    "org.scalatestplus"   %% "mockito-4-6"             % "3.2.15.0" % "test, it",
    "org.pegdown"          % "pegdown"                 % "1.6.0" % "test, it",
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % mongoDbVersion  % "test, it",
    "com.vladsch.flexmark" % "flexmark-all"            % "0.64.6"   % "test,it"
  )

  def apply(): Seq[ModuleID] = compileDependencies ++ testDependencies
}
