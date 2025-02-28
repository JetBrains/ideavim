package _Self.buildTypes

import _Self.Constants.EAP_CHANNEL
import _Self.Constants.RELEASE_EAP
import _Self.IdeaVimBuildType
import _Self.vcsRoots.ReleasesVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange

object ReleaseEapFromBranch : IdeaVimBuildType({
  name = "EXP: Publish EAP Build from branch"
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
    password(
      "env.YOUTRACK_TOKEN",
      "credentialsJSON:2479995b-7b60-4fbb-b095-f0bafae7f622",
      display = ParameterDisplay.HIDDEN
    )
  }

  vcs {
    root(ReleasesVcsRoot)
    branchFilter = """
      +:heads/releases/*
      """.trimIndent()

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
      name = "Calculate new eap version from branch"
      tasks = "scripts:calculateNewEapVersionFromBranch"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    gradle {
      name = "Set TeamCity build number"
      tasks = "scripts:setTeamCityBuildNumber"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    gradle {
      name = "Add release tag"
      tasks = "scripts:addReleaseTag"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    gradle {
      name = "Publish plugin"
      tasks = "publishPlugin"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    script {
      name = "Push changes to the repo"
      scriptContent = """
      branch=$(git branch --show-current)
      echo current branch is ${'$'}branch
      git push origin %build.number%
      """.trimIndent()
    }
    gradle {
      name = "YouTrack post release actions"
      tasks = "scripts:eapReleaseActions"
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
})
