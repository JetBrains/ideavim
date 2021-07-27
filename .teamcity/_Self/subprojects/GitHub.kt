package _Self.subprojects

import _Self.buildTypes.GithubTests
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object GitHub : Project({
  name = "Pull Requests checks"
  description = "Automatic checking of GitHub Pull Requests"

  buildType(GithubTests)
})
