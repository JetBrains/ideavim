package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
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
//    param("env.ORG_GRADLE_PROJECT_ideaVersion", RELEASE)
//    password(
//      "env.ORG_GRADLE_PROJECT_publishToken",
//      "credentialsJSON:61a36031-4da1-4226-a876-b8148bf32bde",
//      label = "Password"
//    )
//    param("env.ORG_GRADLE_PROJECT_version", "%build.number%")
//    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
//    param("env.ORG_GRADLE_PROJECT_publishChannels", DEV_CHANNEL)
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    gradle {
      tasks = ":vim-engine:publish"
      buildFile = ""
      enableStacktrace = true
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
