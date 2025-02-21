package _Self

import _Self.buildTypes.Compatibility
import _Self.buildTypes.LongRunning
import _Self.buildTypes.Nvim
import _Self.buildTypes.PluginVerifier
import _Self.buildTypes.PropertyBased
import _Self.buildTypes.Qodana
import _Self.buildTypes.TestingBuildType
import _Self.subprojects.GitHub
import _Self.subprojects.Releases
import _Self.vcsRoots.GitHubPullRequest
import _Self.vcsRoots.ReleasesVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object Project : Project({
  description = "Vim engine for JetBrains IDEs"

  subProjects(Releases, GitHub)

  // VCS roots
  vcsRoot(GitHubPullRequest)
  vcsRoot(ReleasesVcsRoot)

  // Active tests
  buildType(TestingBuildType("Latest EAP", "<default>", version = "LATEST-EAP-SNAPSHOT"))
  buildType(TestingBuildType("2024.3.3", "<default>"))
  buildType(TestingBuildType("Latest EAP With Xorg", "<default>", version = "LATEST-EAP-SNAPSHOT"))

  buildType(PropertyBased)
  buildType(LongRunning)

  buildType(Nvim)
  buildType(PluginVerifier)
  buildType(Compatibility)

  buildType(Qodana)
})

// Common build type for all configurations
abstract class IdeaVimBuildType(init: BuildType.() -> Unit) : BuildType({
  artifactRules = """
        +:build/reports => build/reports
        +:tests/java-tests/build/reports => java-tests/build/reports
        +:tests/long-running-tests/build/reports => long-running-tests/build/reports
        +:tests/property-tests/build/reports => property-tests/build/reports
        +:/mnt/agent/temp/buildTmp/ => /mnt/agent/temp/buildTmp/
    """.trimIndent()

  init()

  requirements {
    // These requirements define Linux-Medium configuration.
    // Unfortunately, requirement by name (teamcity.agent.name) doesn't work
    //   IDK the reason for it, but on our agents this property is empty
    equals("teamcity.agent.hardware.cpuCount", "16")
    equals("teamcity.agent.os.family", "Linux")
  }

  failureConditions {
    // Disable detection of the java OOM
    javaCrash = false
  }
})