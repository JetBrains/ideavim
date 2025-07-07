@file:Suppress("ClassName")

package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

open class TestingBuildType(
  private val testName: String,
  private val branch: String = "<default>",
  private val version: String = testName,
  private val javaVersion: String? = null,
  private val javaPlugin: Boolean = true,
) : IdeaVimBuildType({
  id("IdeaVimTests_${testName.vanish()}")
  name = "Tests for IntelliJ $testName"

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
      clearConditions()
      tasks = "clean test -x :tests:property-tests:test -x :tests:long-running-tests:test"
      buildFile = ""
      enableStacktrace = true
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
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

private fun String.vanish(): String {
  return this
    .replace(' ', '_')
    .replace('.', '_')
    .replace('-', '_')
}
