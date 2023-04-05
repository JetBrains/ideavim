package _Self

import _Self.buildTypes.Compatibility
import _Self.buildTypes.Compatibility.requirements
import _Self.buildTypes.LongRunning
import _Self.buildTypes.Nvim
import _Self.buildTypes.PluginVerifier
import _Self.buildTypes.PropertyBased
import _Self.buildTypes.Qodana
import _Self.buildTypes.TestsForIntelliJEAP
import _Self.subprojects.GitHub
import _Self.subprojects.OldTests
import _Self.subprojects.Releases
import _Self.vcsRoots.GitHubPullRequest
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object Project : Project({
  description = "Vim engine for IDEs based on the IntelliJ platform"

  requirements {
    // These requirements define Linux-Medium configuration.
    // Unfortunately, requirement by name (teamcity.agent.name) doesn't work
    //   IDK the reason for it, but on our agents this property is empty
    equals("teamcity.agent.hardware.cpuCount", "4")
    equals("teamcity.agent.os.family", "Linux")
  }

  subProjects(Releases, OldTests, GitHub)

  // VCS roots
  vcsRoot(GitHubPullRequest)

  // Builds
  buildType(TestsForIntelliJEAP)

  buildType(PropertyBased)
  buildType(LongRunning)

  buildType(Nvim)
  buildType(PluginVerifier)
  buildType(Compatibility)

  buildType(Qodana)
})
