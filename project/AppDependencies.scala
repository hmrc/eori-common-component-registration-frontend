import sbt.*

object AppDependencies {
  import play.core.PlayVersion

  val mongoDbVersion   = "2.11.0"
  val bootstrapVersion = "10.5.0"
  val playVersion      = 30

  val compileDependencies: Seq[ModuleID] = Seq(
    "org.typelevel"     %% "cats-core"                                        % "2.10.0",
    "uk.gov.hmrc"       %% s"bootstrap-frontend-play-$playVersion"            % bootstrapVersion,
    "uk.gov.hmrc"       %% s"play-conditional-form-mapping-play-$playVersion" % "3.4.0",
    "uk.gov.hmrc"       %% s"domain-play-$playVersion"                        % "11.0.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-play-$playVersion"                    % mongoDbVersion,
    "uk.gov.hmrc"       %% s"play-frontend-hmrc-play-$playVersion"            % "12.25.0",
    "uk.gov.hmrc"       %% s"internal-auth-client-play-$playVersion"          % "4.3.0"
  )

  val testDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-play-$playVersion"  % bootstrapVersion    % "test",
    "org.scalatest"          %% "scalatest"                          % "3.2.18"            % "test,it",
    "org.playframework"      %% "play-test"                          % PlayVersion.current % "test,it",
    "org.scalatestplus.play" %% "scalatestplus-play"                 % "7.0.1"             % "test,it",
    "org.scalacheck"         %% "scalacheck"                         % "1.18.0"            % "test,it",
    "org.scalatestplus"      %% "scalacheck-1-15"                    % "3.2.11.0"          % "test,it",
    "org.jsoup"               % "jsoup"                              % "1.17.2"            % "test,it",
    "us.codecraft"            % "xsoup"                              % "0.3.6"             % "test,it",
    "org.mockito"             % "mockito-core"                       % "5.11.0"            % "test,it",
    "org.scalatestplus"      %% "mockito-4-6"                        % "3.2.15.0"          % "test, it",
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-play-$playVersion" % mongoDbVersion      % "test, it",
    "com.vladsch.flexmark"    % "flexmark-all"                       % "0.64.8"            % "test,it"
  )

  def apply(): Seq[ModuleID] = compileDependencies ++ testDependencies
}
