@file:Suppress("ClassName")

package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

sealed class TestsForIntelliJ_213_221_branch(private val version: String) : BuildType({
  name = "Tests for IntelliJ $version"

  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_legacyNoJavaPlugin", "true")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", "IC-$version")
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
    param("env.ORG_GRADLE_PROJECT_javaVersion", "1.8")
  }

  vcs {
    root(_Self.vcsRoots.Branch_213_221)

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      tasks = "clean test"
      buildFile = ""
      enableStacktrace = true
      param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
    }
  }

  triggers {
    vcs {
      branchFilter = ""
    }
  }

  requirements {
    noLessThanVer("teamcity.agent.jvm.version", "1.8")
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


object TestsForIntelliJ20213 : TestsForIntelliJ_213_221_branch("2021.3.2")
