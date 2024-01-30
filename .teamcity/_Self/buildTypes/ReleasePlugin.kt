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
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange

object ReleaseMajor : ReleasePlugin("major")
object ReleaseMinor : ReleasePlugin("minor")
object ReleasePatch : ReleasePlugin("patch")

sealed class ReleasePlugin(private val releaseType: String) : IdeaVimBuildType({
  name = "Publish $releaseType release"
  description = "Build and publish IdeaVim plugin"

  artifactRules = "build/distributions/*"

  params {
    param("env.ORG_GRADLE_PROJECT_ideaVersion", RELEASE)
    password(
      "env.ORG_GRADLE_PROJECT_publishToken",
      "credentialsJSON:61a36031-4da1-4226-a876-b8148bf32bde",
      label = "Password"
    )
    param("env.ORG_GRADLE_PROJECT_publishChannels", "$DEFAULT_CHANNEL,$EAP_CHANNEL,$DEV_CHANNEL")
    password(
      "env.ORG_GRADLE_PROJECT_slackUrl",
      "credentialsJSON:a8ab8150-e6f8-4eaf-987c-bcd65eac50b5",
      label = "Slack Token"
    )
    password("env.ORG_GRADLE_PROJECT_youtrackToken", "credentialsJSON:3cd3e867-282c-451f-b958-bc95d56a8450", display = ParameterDisplay.HIDDEN)
    param("env.ORG_GRADLE_PROJECT_releaseType", releaseType)
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
      name = "Select branch"
      tasks = "scripts:selectBranch"
    }
    gradle {
      name = "Calculate new version"
      tasks = "scripts:calculateNewVersion"
    }
    gradle {
      name = "Set TeamCity build number"
      tasks = "scripts:setTeamCityBuildNumber"
    }
    gradle {
      name = "Update change log"
      tasks = "scripts:changelogUpdateUnreleased"
    }
    gradle {
      name = "Commit preparation changes"
      tasks = "scripts:commitChanges"
    }
    gradle {
      name = "Add release tag"
      tasks = "scripts:addReleaseTag"
    }
//    gradle {
//      name = "Reset release branch"
//      tasks = "scripts:resetReleaseBranch"
//    }
    script {
      name = "Reset release branch"
      //language=Shell Script
      scriptContent = """
        if [ "major" = $releaseType ] && [ "minor" = $releaseType ] && [ "patch" = $releaseType ]
        then
          branch=${'$'}(git branch --show-current)  
          echo current branch is ${'$'}branch
          
          if [ $releaseType != "patch" ]
          then
            commit=${'$'}(git rev-parse HEAD)
            git checkout release
            echo Checked out release branch
            git reset --hard ${'$'}commit
            echo Release branch reset
            git checkout master
            echo Checked out master
          else
            echo Skip release branch reset because release type is patch
          fi
        else
          echo This function accepts only major, minor, or patch as release type. Current value: $releaseType
        fi
      """.trimIndent()
    }
    gradle {
      name = "Publish release"
      tasks = "publishPlugin"
    }
//    gradle {
//      name = "Push changes to the repo"
//      tasks = "scripts:pushChangesWithReleaseBranch"
//    }
    script {
      name = "Push changes to the repo"
      scriptContent = """
      branch=$(git branch --show-current)  
      echo current branch is ${'$'}branch
      if [ "master" != "${'$'}branch" ];
      then
        git checkout master
      fi
      
      git push origin --tags
      git push origin
      
      if [ "patch" != $releaseType  ];
      then
        git checkout release
        echo checkout release branch
        git branch --set-upstream-to=origin/release release
        git push --tags
        git push origin --force
      fi
      
      git checkout ${'$'}branch
      """.trimIndent()
    }
    gradle {
      name = "Run Integrations"
      tasks = "releaseActions"
    }
    gradle {
      name = "Slack Notification"
      tasks = "slackNotification"
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
