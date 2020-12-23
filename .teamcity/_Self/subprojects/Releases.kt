package _Self.subprojects

import _Self.buildTypes.Release
import _Self.buildTypes.ReleaseEap
import _Self.buildTypes.Release_201
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object Releases : Project({
  description = "IdeaVim releases"

  buildType(Release)
  buildType(Release_201)
  buildType(ReleaseEap)
})