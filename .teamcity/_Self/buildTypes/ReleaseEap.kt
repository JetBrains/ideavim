package _Self.buildTypes

import _Self.AgentSize
import _Self.Constants.EAP_CHANNEL
import _Self.Constants.RELEASE_EAP
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange

object ReleaseEap : IdeaVimBuildType({
  name = "Publish EAP Build"
  description = "Build and publish EAP of IdeaVim plugin"

  artifactRules = "build/distributions/*"

  params {
    param("env.ORG_GRADLE_PROJECT_ideaVersion", RELEASE_EAP)
    password(
      "env.ORG_GRADLE_PROJECT_publishToken",
      "credentialsJSON:61a36031-4da1-4226-a876-b8148bf32bde",
      label = "Password"
    )
    param("env.ORG_GRADLE_PROJECT_publishChannels", EAP_CHANNEL)
    password(
      "env.ORG_GRADLE_PROJECT_slackUrl",
      "credentialsJSON:a8ab8150-e6f8-4eaf-987c-bcd65eac50b5",
      label = "Slack URL"
    )
    password(
      "env.ORG_GRADLE_PROJECT_youtrackToken",
      "credentialsJSON:eedfa0eb-c329-462a-b7b4-bc263bda8c01",
      display = ParameterDisplay.HIDDEN
    )
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
      name = "Calculate new eap version"
      tasks = "scripts:calculateNewEapVersion"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    gradle {
      name = "Set TeamCity build number"
      tasks = "scripts:setTeamCityBuildNumber"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    gradle {
      name = "Add release tag"
      tasks = "scripts:addReleaseTag"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    gradle {
      name = "Publish plugin"
      tasks = "publishPlugin"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    script {
      name = "Push changes to the repo"
      scriptContent = """
      branch=$(git branch --show-current)
      echo current branch is ${'$'}branch
      if [ "master" != "${'$'}branch" ];
      then
        exit 1
      fi

      git push origin %build.number%
      """.trimIndent()
    }
    gradle {
      name = "YouTrack post release actions"
      tasks = "scripts:eapReleaseActions"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
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
    equals("teamcity.agent.hardware.cpuCount", AgentSize.XLARGE)
    equals("teamcity.agent.os.family", "Linux")
  }
})
