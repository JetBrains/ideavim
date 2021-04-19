package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.qodana
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object Qodana : BuildType({
  name = "Qodana checks"
  params {
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_ideaVersion", "LATEST-EAP-SNAPSHOT")
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
      namesAndTags = "custom"
      param("namesAndTagsCustom", "jetbrains/qodana:latest")
    }
  }

  triggers {
    vcs {
      branchFilter = ""
    }
  }

  failureConditions {
    failOnMetricChange {
      threshold = 0
      units = BuildFailureOnMetric.MetricUnit.DEFAULT_UNIT
      comparison = BuildFailureOnMetric.MetricComparison.MORE
      compareTo = value()
      param("metricKey", "QodanaProblemsTotal")
    }
  }

  requirements {
    noLessThanVer("teamcity.agent.jvm.version", "1.8")
  }
})
