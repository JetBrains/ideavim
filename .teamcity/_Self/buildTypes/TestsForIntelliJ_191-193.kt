@file:Suppress("ClassName")

package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object TestsForIntelliJ20191 : TestsForIntelliJ_191_193_branch("2019.1")
object TestsForIntelliJ20192 : TestsForIntelliJ_191_193_branch("2019.2")
object TestsForIntelliJ20193 : TestsForIntelliJ_191_193_branch("2019.3")

sealed class TestsForIntelliJ_191_193_branch(private val version: String) : IdeaVimBuildType({
  name = "Tests for IntelliJ $version"

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_legacyNoJavaPlugin", "true")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", "IC-$version")
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
    param("env.ORG_GRADLE_PROJECT_javaVersion", "1.8")
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:191-193"

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
      branchFilter = "+:191-193"
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
