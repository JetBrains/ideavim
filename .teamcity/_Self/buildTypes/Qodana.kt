package _Self.buildTypes

import _Self.Constants.QODANA_TESTS
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.Qodana
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.qodana
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
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
    gradle {
      name = "Generate grammar"
      tasks = "generateGrammarSource"
    }
    qodana {
      name = "Qodana"
      param("clonefinder-languages", "")
      param("collect-anonymous-statistics", "")
      param("licenseaudit-enable", "")
      param("clonefinder-languages-container", "")
      param("linterVersion", "")
      param("clonefinder-queried-project", "")
      param("clonefinder-enable", "")
      param("clonefinder-reference-projects", "")
      linter = jvm {
        version = Qodana.JVMVersion.LATEST
      }
      reportAsTests = true
      additionalDockerArguments = "-e QODANA_TOKEN=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJvcmdhbml6YXRpb24iOiIzUFZrQSIsInByb2plY3QiOiIzN1FlQSIsInRva2VuIjoiM0t2bXoifQ.uohp81tM7iAfvvB6k8faarfpV-OjusAaEbWQ8iNrOgs"
      additionalQodanaArguments = "--baseline qodana.sarif.json"
    }
  }

  triggers {
    vcs {
      enabled = false
      branchFilter = ""
    }
    schedule {
      schedulingPolicy = daily {
        hour = 12
        minute = 0
        timezone = "SERVER"
      }
      param("dayOfWeek", "Sunday")
    }
  }

  failureConditions {
    failOnMetricChange {
      threshold = 0
      units = BuildFailureOnMetric.MetricUnit.DEFAULT_UNIT
      comparison = BuildFailureOnMetric.MetricComparison.MORE
      compareTo = value()
      metric = BuildFailureOnMetric.MetricType.TEST_FAILED_COUNT
      param("metricKey", "QodanaProblemsNew")
    }
  }

  requirements {
    noLessThanVer("teamcity.agent.jvm.version", "1.8")
  }
})
