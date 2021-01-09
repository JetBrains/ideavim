package _Self.subprojects

import _Self.buildTypes.TestsForIntelliJ20181
import _Self.buildTypes.TestsForIntelliJ20182
import _Self.buildTypes.TestsForIntelliJ20183
import _Self.buildTypes.TestsForIntelliJ20191
import _Self.buildTypes.TestsForIntelliJ20192
import _Self.buildTypes.TestsForIntelliJ20193
import _Self.buildTypes.TestsForIntelliJ20201
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object OldTests : Project({
  name = "Old IdeaVim tests"
  description = "Tests for older versions of IJ"

  buildType(TestsForIntelliJ20201)
  buildType(TestsForIntelliJ20191)
  buildType(TestsForIntelliJ20181)
  buildType(TestsForIntelliJ20192)
  buildType(TestsForIntelliJ20182)
  buildType(TestsForIntelliJ20193)
  buildType(TestsForIntelliJ20183)
})
