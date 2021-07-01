package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

sealed class ActiveTests(buildName: String, ijVersion: String) : BuildType({
  name = buildName
  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", ijVersion)
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(DslContext.settingsRoot)

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      tasks = "clean check -x ktlintMainSourceSetCheck"
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

object TestsForIntelliJEAP : ActiveTests("Tests for IntelliJ Latest EAP", "LATEST-EAP-SNAPSHOT")
object TestsForIntelliJ20211 : ActiveTests("Tests for IntelliJ 2021.1", "2021.1")
object TestsForIntelliJ20203 : ActiveTests("Tests for IntelliJ 2020.3", "2020.3")
object TestsForIntelliJ20202 : ActiveTests("Tests for IntelliJ 2020.2", "2020.2")
