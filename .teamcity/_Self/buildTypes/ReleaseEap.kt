package _Self.buildTypes

import _Self.Constants.DEV_VERSION
import _Self.Constants.EAP_CHANNEL
import _Self.Constants.RELEASE_EAP
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.vcsLabeling
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
      label = "Slack Token"
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
    gradle {
      name = "Calculate new eap version"
      tasks = "scripts:calculateNewEapVersion"
    }
    gradle {
      name = "Add release tag"
      tasks = "scripts:addReleaseTag"
    }
    gradle {
      tasks = "publishPlugin"
      buildFile = ""
      enableStacktrace = true
    }
    gradle {
      name = "Push changes to the repo"
      tasks = "scripts:pushChanges"
    }
  }

  features {
    vcsLabeling {
      vcsRootId = "${DslContext.settingsRoot.id}"
      labelingPattern = "%system.build.number%"
      successfulOnly = false
      branchFilter = "+:<default>"
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
    // These requirements define Linux-XLarge configuration.
    // Unfortunately, requirement by name (teamcity.agent.name) doesn't work
    //   IDK the reason for it, but on our agents this property is empty
    equals("teamcity.agent.hardware.cpuCount", "16")
    equals("teamcity.agent.os.family", "Linux")
  }
})
