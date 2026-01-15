package _Self.buildTypes

import _Self.AgentSize
import _Self.Constants.DEV_CHANNEL
import _Self.Constants.RELEASE_DEV
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule

object ReleaseDev : IdeaVimBuildType({
  name = "Publish Dev Build"
  description = "Build and publish Dev of IdeaVim plugin"

  artifactRules = "build/distributions/*"

  params {
    param("env.ORG_GRADLE_PROJECT_ideaVersion", RELEASE_DEV)
    password(
      "env.ORG_GRADLE_PROJECT_publishToken",
      "credentialsJSON:61a36031-4da1-4226-a876-b8148bf32bde",
      label = "Password"
    )
    param("env.ORG_GRADLE_PROJECT_publishChannels", DEV_CHANNEL)
  }

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"

    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    script {
      name = "Pull git tags"
      scriptContent = "git fetch --tags origin"
    }
    script {
      name = "Pull git history"
      scriptContent = "git fetch --unshallow"
    }
    gradle {
      name = "Calculate new dev version"
      tasks = "scripts:calculateNewDevVersion"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    gradle {
      name = "Set TeamCity build number"
      tasks = "scripts:setTeamCityBuildNumber"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    gradle {
      tasks = "publishPlugin"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  triggers {
    schedule {
      enabled = true
      schedulingPolicy = daily {
        hour = 2
      }
      branchFilter = ""
    }
  }

  features {
    sshAgent {
      teamcitySshKey = "IdeaVim ssh keys"
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

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})
