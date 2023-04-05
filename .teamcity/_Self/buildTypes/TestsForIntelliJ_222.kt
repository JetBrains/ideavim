@file:Suppress("ClassName")

package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object TestsForIntelliJ20222 : TestingBuildType("IC-2022.2.3", branch = "222", javaPlugin = false)
object TestsForIntelliJ20213 : TestingBuildType("IC-2021.3.2", "213-221", "1.8", javaPlugin = false)
object TestsForIntelliJ20212 : TestingBuildType("IC-2021.2.2", "203-212", "1.8", javaPlugin = false)
object TestsForIntelliJ20211 : TestingBuildType("IC-2021.1", "203-212", "1.8", javaPlugin = false)
object TestsForIntelliJ20203 : TestingBuildType("IC-2020.3", "203-212", "1.8", javaPlugin = false)
object TestsForIntelliJ20202 : TestingBuildType("IC-2020.2", "202", "1.8", javaPlugin = false)
object TestsForIntelliJ20201 : TestingBuildType("IC-2020.1", "201", "1.8", javaPlugin = false)
object TestsForIntelliJ20191 : TestingBuildType("IC-2019.1", "191-193", "1.8", javaPlugin = false)
object TestsForIntelliJ20192 : TestingBuildType("IC-2019.2", "191-193", "1.8", javaPlugin = false)
object TestsForIntelliJ20193 : TestingBuildType("IC-2019.3", "191-193", "1.8", javaPlugin = false)
object TestsForIntelliJ20181 : TestingBuildType("IC-2018.1", "181-182", "1.8", javaPlugin = false)
object TestsForIntelliJ20182 : TestingBuildType("IC-2018.2", "181-182", "1.8", javaPlugin = false)
object TestsForIntelliJ20183 : TestingBuildType("IC-2018.3", "183", "1.8", javaPlugin = false)

sealed class TestingBuildType(
  private val testName: String,
  private val branch: String,
  private val version: String = testName,
  private val javaVersion: String? = null,
  private val javaPlugin: Boolean = true,
) : IdeaVimBuildType({
  name = "Tests for IntelliJ $version"

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_legacyNoJavaPlugin", javaPlugin.not().toString())
    param("env.ORG_GRADLE_PROJECT_ideaVersion", version)
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
    if (javaVersion != null) {
      param("env.ORG_GRADLE_PROJECT_javaVersion", javaVersion)
    }
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:$branch"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      tasks = "clean test"
      buildFile = ""
      enableStacktrace = true
    }
  }

  triggers {
    vcs {
      branchFilter = "+:$branch"
    }
  }

  failureConditions {
    failOnMetricChange {
      metric = BuildFailureOnMetric.MetricType.TEST_COUNT
      threshold = 20
      units = BuildFailureOnMetric.MetricUnit.PERCENTS
      comparison = BuildFailureOnMetric.MetricComparison.LESS
      compareTo = build {
        buildRule = lastSuccessful()
      }
    }
  }
})
