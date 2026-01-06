/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package _Self.buildTypes

import _Self.AgentSize
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

object ReleaseMajor : ReleasePlugin("major")
object ReleaseMinor : ReleasePlugin("minor")
object ReleasePatch : ReleasePlugin("patch")

sealed class ReleasePlugin(private val releaseType: String) : IdeaVimBuildType({
  name = "Publish $releaseType release"
  description = "Build and publish IdeaVim plugin"

  artifactRules = """
        build/distributions/*
        build/reports/*
    """.trimIndent()

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
      label = "Slack URL"
    )
    password(
      "env.ORG_GRADLE_PROJECT_youtrackToken",
      "credentialsJSON:7bc0eb3a-b86a-4ebd-b622-d4ef12d7e1d3",
      display = ParameterDisplay.HIDDEN
    )
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
    script {
      name = "Reset release branch"
      scriptContent = """
        if [ "major" = "$releaseType" ] || [ "minor" = "$releaseType" ]
        then
          echo Resetting the release branch because the release type is $releaseType
          git checkout master
          latest_eap=${'$'}(git describe --tags --match="[0-9].[0-9]*.[0-9]-eap.[0-9]*" --abbrev=0 HEAD)
          echo Latest EAP: ${'$'}latest_eap
          commit_of_latest_eap=${'$'}(git rev-list -n 1 ${'$'}latest_eap)
          echo Commit of latest EAP: ${'$'}commit_of_latest_eap
          git checkout release
          git reset --hard ${'$'}commit_of_latest_eap
        else
          git checkout release
          echo Do not reset the release branch because the release type is $releaseType
        fi
        echo Checked out release branch
      """.trimIndent()
    }
    gradle {
      name = "Calculate new version"
      tasks = "scripts:calculateNewVersion"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    gradle {
      name = "Set TeamCity build number"
      tasks = "scripts:setTeamCityBuildNumber"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
//    gradle {
//      name = "Update change log"
//      tasks = "scripts:changelogUpdateUnreleased"
//    }
//    gradle {
//      name = "Commit preparation changes"
//      tasks = "scripts:commitChanges"
//    }
    gradle {
      name = "Add release tag"
      tasks = "scripts:addReleaseTag"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    script {
      name = "Run tests"
      scriptContent = "./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test"
    }
    gradle {
      name = "Publish release"
      tasks = "publishPlugin"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
//    script {
//      name = "Checkout master branch"
//      scriptContent = """
//        echo Checkout master
//        git checkout master
//      """.trimIndent()
//    }
//    gradle {
//      name = "Update change log in master"
//      tasks = "scripts:changelogUpdateUnreleased"
//    }
//    gradle {
//      name = "Commit preparation changes in master"
//      tasks = "scripts:commitChanges"
//    }
    script {
      name = "Push changes to the repo"
      scriptContent = """
      git checkout release
      echo checkout release branch
      git branch --set-upstream-to=origin/release release
      git push origin --force
      # Push tag
      git push origin %build.number%
      """.trimIndent()
    }
    gradle {
      name = "Run Integrations"
      tasks = "releaseActions"
      gradleParams = "--no-configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
  }

  features {
    sshAgent {
      teamcitySshKey = "IdeaVim ssh keys"
    }
  }

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})
