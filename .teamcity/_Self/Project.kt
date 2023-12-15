package _Self

import _Self.buildTypes.Compatibility
import _Self.buildTypes.LongRunning
import _Self.buildTypes.Nvim
import _Self.buildTypes.PluginVerifier
import _Self.buildTypes.PropertyBased
import _Self.buildTypes.Qodana
import _Self.buildTypes.TestingBuildType
import _Self.subprojects.GitHub
import _Self.subprojects.OldTests
import _Self.subprojects.Releases
import _Self.vcsRoots.GitHubPullRequest
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object Project : Project({
  description = "Vim engine for IDEs based on the IntelliJ platform"

  subProjects(Releases, OldTests, GitHub)

  // VCS roots
  vcsRoot(GitHubPullRequest)

  // Active tests
  buildType(TestingBuildType("Latest EAP", "<default>", version = "LATEST-EAP-SNAPSHOT"))
  buildType(TestingBuildType("2023.3", "<default>", version = "2023.3"))
  buildType(TestingBuildType("2023.3_XYZ", "xyz", version = "2023.3"))
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

  artifactRules = """
        +:build/reports => build/reports
        +:/mnt/agent/temp/buildTmp/ => /mnt/agent/temp/buildTmp/
    """.trimIndent()
})