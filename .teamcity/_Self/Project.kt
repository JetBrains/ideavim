package _Self

import _Self.buildTypes.Compatibility
import _Self.buildTypes.LongRunning
import _Self.buildTypes.Nvim
import _Self.buildTypes.PluginVerifier
import _Self.buildTypes.PropertyBased
import _Self.buildTypes.RandomOrderTests
import _Self.buildTypes.TestingBuildType
import _Self.subprojects.GitHub
import _Self.subprojects.Releases
import _Self.vcsRoots.GitHubPullRequest
import _Self.vcsRoots.ReleasesVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object Project : Project({
  description = "Vim engine for JetBrains IDEs"

  subProject(Releases)
  subProject(GitHub)

  // VCS roots
  vcsRoot(GitHubPullRequest)
  vcsRoot(ReleasesVcsRoot)

  // Active tests
  buildType(TestingBuildType("Latest EAP", version = "LATEST-EAP-SNAPSHOT"))
  buildType(TestingBuildType("2025.3"))
  buildType(TestingBuildType("Latest EAP With Xorg", "<default>", version = "LATEST-EAP-SNAPSHOT"))

  buildType(PropertyBased)
  buildType(LongRunning)
  buildType(RandomOrderTests)

  buildType(Nvim)
  buildType(PluginVerifier)
  buildType(Compatibility)
})

// Agent size configurations (CPU count)
object AgentSize {
  const val MEDIUM = "4"
  const val XLARGE = "16"
}

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

  failureConditions {
    // Disable detection of the java OOM
    javaCrash = false
  }
})