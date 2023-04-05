package _Self.subprojects

import _Self.buildTypes.TestingBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object OldTests : Project({
  name = "Old IdeaVim tests"
  description = "Tests for older versions of IJ"

  buildType(TestingBuildType("IC-2018.1", "181-182", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2018.2", "181-182", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2018.3", "183", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2019.1", "191-193", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2019.2", "191-193", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2019.3", "191-193", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2020.1", "201", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2020.2", "202", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2020.3", "203-212", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2021.1", "203-212", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2021.2.2", "203-212", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2021.3.2", "213-221", javaVersion = "1.8", javaPlugin = false))
  buildType(TestingBuildType("IC-2022.2.3", branch = "222", javaPlugin = false))
})
