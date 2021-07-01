package _Self.buildTypes

import _Self.Constants.QODANA_TESTS
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.qodana
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object Qodana : BuildType({
  name = "Qodana checks"
  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", QODANA_TESTS)
    param("env.ORG_GRADLE_PROJECT_instrumentPluginCode", "false")
  }

  vcs {
    root(DslContext.settingsRoot)

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    qodana {
      name = "Qodana"
      reportAsTestsEnable = ""
      failBuildOnErrors = ""
      codeInspectionXmlConfig = "Custom"
      codeInspectionCustomXmlConfigPath = ".idea/inspectionProfiles/Qodana.xml"
      reportAsTestsEnable = "true"
      param("clonefinder-languages", "Java")
      param("clonefinder-mode", "")
      param("report-version", "")
      param("clonefinder-languages-container", "Java Kotlin")
      param("namesAndTagsCustom", "repo.labs.intellij.net/static-analyser/qodana")
      param("clonefinder-queried-project", "src")
      param("clonefinder-enable", "true")
      param("clonefinder-reference-projects", "src")
      param("yaml-configuration", "")
    }
  }

  triggers {
    vcs {
      enabled = false
      branchFilter = ""
    }
    schedule {
      schedulingPolicy = weekly {
        dayOfWeek = ScheduleTrigger.DAY.Tuesday
      }
      branchFilter = ""
      triggerBuild = always()
    }
  }

  failureConditions {
    failOnMetricChange {
      threshold = 0
      units = BuildFailureOnMetric.MetricUnit.DEFAULT_UNIT
      comparison = BuildFailureOnMetric.MetricComparison.MORE
      compareTo = value()
      metric = BuildFailureOnMetric.MetricType.TEST_FAILED_COUNT
      param("metricKey", "QodanaProblemsTotal")
    }
  }

  requirements {
    noLessThanVer("teamcity.agent.jvm.version", "1.8")
  }
})
