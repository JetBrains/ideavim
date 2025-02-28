package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule

object PublishVimEngine : IdeaVimBuildType({
  name = "Publish vim-engine"
  description = "Build and publish vim-engine library"

  artifactRules = "build/distributions/*"
  buildNumberPattern = "0.0.%build.counter%"

  params {
    param("env.ORG_GRADLE_PROJECT_engineVersion", "%build.number%")
    param("env.ORG_GRADLE_PROJECT_uploadUrl", "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    password("env.ORG_GRADLE_PROJECT_spacePassword", "credentialsJSON:5ea56f8c-efe7-4e1e-83de-0c02bcc39d0b", display = ParameterDisplay.HIDDEN)
    param("env.ORG_GRADLE_PROJECT_spaceUsername", "a121c67e-39ac-40e6-bf82-649855ec27b6")
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:fleet"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      tasks = ":vim-engine:publish"
      buildFile = ""
      enableStacktrace = true
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  triggers {
    schedule {
      enabled = true
      schedulingPolicy = weekly {
        dayOfWeek = ScheduleTrigger.DAY.Sunday
      }
      branchFilter = ""
    }
  }

  failureConditions {
    failOnMetricChange {
      metric = BuildFailureOnMetric.MetricType.ARTIFACT_SIZE
      threshold = 5
      units = BuildFailureOnMetric.MetricUnit.PERCENTS
      comparison = BuildFailureOnMetric.MetricComparison.DIFF
      compareTo = build {
        buildRule = lastSuccessful()
      }
    }
  }
})
