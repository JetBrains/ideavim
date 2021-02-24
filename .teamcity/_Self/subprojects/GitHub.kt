package _Self.subprojects

import _Self.buildTypes.GithubLint
import _Self.buildTypes.GithubTests
import _Self.buildTypes.Release
import _Self.buildTypes.ReleaseDev
import _Self.buildTypes.ReleaseEap
import _Self.buildTypes.Release_201
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object GitHub : Project({
  name = "Pull Requests checks"
  description = "Automatic checking of GitHub Pull Requests"

  buildType(GithubTests)
  buildType(GithubLint)
})
