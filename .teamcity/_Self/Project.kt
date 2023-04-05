package _Self

import _Self.buildTypes.Compatibility
import _Self.buildTypes.LongRunning
import _Self.buildTypes.Nvim
import _Self.buildTypes.PluginVerifier
import _Self.buildTypes.PropertyBased
import _Self.buildTypes.Qodana
import _Self.buildTypes.TestsForIntelliJEAP
import _Self.subprojects.GitHub
import _Self.subprojects.OldTests
import _Self.subprojects.Releases
import _Self.vcsRoots.Branch_181
import _Self.vcsRoots.Branch_183
import _Self.vcsRoots.Branch_191_193
import _Self.vcsRoots.Branch_201
import _Self.vcsRoots.Branch_202
import _Self.vcsRoots.Branch_203_212
import _Self.vcsRoots.Branch_213_221
import _Self.vcsRoots.Branch_222
import _Self.vcsRoots.Branch_Release
import _Self.vcsRoots.GitHubPullRequest
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object Project : Project({
  description = "Vim engine for IDEs based on the IntelliJ platform"

  subProjects(Releases, OldTests, GitHub)

  // VCS roots
  vcsRoot(Branch_183)
  vcsRoot(Branch_181)
  vcsRoot(Branch_191_193)
  vcsRoot(Branch_201)
  vcsRoot(Branch_202)
  vcsRoot(Branch_203_212)
  vcsRoot(Branch_213_221)
  vcsRoot(Branch_222)
  vcsRoot(Branch_Release)
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
