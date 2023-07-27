/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package _Self.buildTypes

import _Self.Constants.DEFAULT_CHANNEL
import _Self.Constants.DEV_CHANNEL
import _Self.Constants.EAP_CHANNEL
import _Self.Constants.RELEASE
import _Self.Constants.VERSION
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange

object ReleaseMinor : IdeaVimBuildType({
  name = "Publish Minor Release"
  description = "Build and publish IdeaVim plugin"

  artifactRules = "build/distributions/*"

  params {
    param("env.ORG_GRADLE_PROJECT_ideaVersion", RELEASE)
    password(
      "env.ORG_GRADLE_PROJECT_publishToken",
      "credentialsJSON:61a36031-4da1-4226-a876-b8148bf32bde",
      label = "Password"
    )
    param("env.ORG_GRADLE_PROJECT_version", "%build.number%")
    param("env.ORG_GRADLE_PROJECT_downloadIdeaSources", "false")
    param("env.ORG_GRADLE_PROJECT_publishChannels", "$DEFAULT_CHANNEL,$EAP_CHANNEL,$DEV_CHANNEL")
    password(
      "env.ORG_GRADLE_PROJECT_slackUrl",
      "credentialsJSON:a8ab8150-e6f8-4eaf-987c-bcd65eac50b5",
      label = "Slack Token"
    )
    password("env.ORG_GRADLE_PROJECT_youtrackToken", "credentialsJSON:3cd3e867-282c-451f-b958-bc95d56a8450", display = ParameterDisplay.HIDDEN)
    param("env.ORG_GRADLE_PROJECT_releaseType", "minor")
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
      name = "Calculate new version"
      tasks = "scripts:calculateNewVersion"
      buildFile = ""
      enableStacktrace = true
    }
    gradle {
      name = "Update change log"
      tasks = "scripts:changelogUpdateUnreleased"
      buildFile = ""
      enableStacktrace = true
    }
    gradle {
      name = "Commit preparation changes"
      tasks = "scripts:commitChanges"
      buildFile = ""
      enableStacktrace = true
    }
    gradle {
      name = "Add release tag"
      tasks = "scripts:addReleaseTag"
      buildFile = ""
      enableStacktrace = true
    }
    gradle {
      name = "Publish release"
      tasks = "clean publishPlugin"
      buildFile = ""
      enableStacktrace = true
      enabled = false
    }
    gradle {
      name = "Run Integrations"
      tasks = "releaseActions"
      enabled = false
    }
    gradle {
      name = "Slack Notification"
      tasks = "slackNotification"
      enabled = false
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
