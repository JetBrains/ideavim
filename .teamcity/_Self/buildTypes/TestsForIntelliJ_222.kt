@file:Suppress("ClassName")

package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object TestsForIntelliJ20222 : TestsForIntelliJ_222_branch("2022.2.3")

sealed class TestsForIntelliJ_222_branch(private val version: String) : BuildType({
  name = "Tests for IntelliJ $version"

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_legacyNoJavaPlugin", "true")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", "IC-$version")
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:222"

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
      branchFilter = "+:222"
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
