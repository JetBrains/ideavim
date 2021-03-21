package _Self.subprojects

import _Self.buildTypes.Release
import _Self.buildTypes.ReleaseDev
import _Self.buildTypes.ReleaseEap
import _Self.buildTypes.Release_201
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object Releases : Project({
  name = "IdeaVim releases"
  description = "Stable and EAP releases for IdeaVim"

  buildType(Release)
  buildType(Release_201)
  buildType(ReleaseEap)
  buildType(ReleaseDev)
})
