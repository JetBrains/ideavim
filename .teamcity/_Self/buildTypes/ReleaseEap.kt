package _Self.buildTypes

import _Self.AgentSize
import _Self.Constants.EAP_CHANNEL
import _Self.Constants.RELEASE_EAP
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.Triggers
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule

object ReleaseEap : IdeaVimBuildType({
  name = "Publish EAP Build"
  description = "Reset the release branch to master and publish it as an EAP of IdeaVim plugin"

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
      scriptContent = """
        mkdir -p ~/.ssh && chmod 700 ~/.ssh
        ssh-keyscan -H github.com >> ~/.ssh/known_hosts
        git fetch --tags --force origin
      """.trimIndent()
    }
    script {
      name = "Pull git history"
      scriptContent = "git fetch --unshallow"
    }
    script {
      name = "Reset release branch to master"
      scriptContent = """
        set -e
        git checkout master
        master_commit=${'$'}(git rev-parse HEAD)
        echo Master commit: ${'$'}master_commit
        git checkout release
        git reset --hard ${'$'}master_commit
        echo Release branch reset to the latest master
      """.trimIndent()
    }
    gradle {
      name = "Calculate new eap version"
      tasks = "scripts:calculateNewEapVersion"
      gradleParams = "--build-cache --configuration-cache"
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    script {
      name = "Set TeamCity build number"
      scriptContent = """
        set -e
        cd scripts-ts
        npm ci --silent --no-fund --no-audit
        npx tsx src/setTeamCityBuildNumber.ts
      """.trimIndent()
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
      set -e
      branch=$(git branch --show-current)
      echo current branch is ${'$'}branch
      if [ "release" != "${'$'}branch" ];
      then
        exit 1
      fi

      # Force, because the release branch was reset to master and may have dropped commits
      # (a preparation commit of the previous release, patch cherry-picks) in the process.
      git push --force origin release
      git push origin %build.number%
      """.trimIndent()
    }
    script {
      name = "YouTrack post release actions"
      scriptContent = """
        set -e
        cd scripts-ts
        npm ci --silent --no-fund --no-audit
        : "${'$'}{ORG_GRADLE_PROJECT_youtrackToken:?ORG_GRADLE_PROJECT_youtrackToken is not set}"
        export YOUTRACK_TOKEN="${'$'}ORG_GRADLE_PROJECT_youtrackToken"
        npx tsx src/eapReleaseActions.ts "%build.number%"
      """.trimIndent()
    }
  }

  triggers {
    // Roughly every two weeks: the first and the third Tuesday of the month. Quartz cron has no way
    // to say "every 14 days", and a single expression supports only one `#` (nth weekday) value,
    // so this is two triggers rather than one.
    eapSchedule(tuesdayOfMonth = 1)
    eapSchedule(tuesdayOfMonth = 3)
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

/** Triggers the build at 05:00 on the [tuesdayOfMonth]-th Tuesday of every month. */
private fun Triggers.eapSchedule(tuesdayOfMonth: Int) {
  schedule {
    enabled = true
    schedulingPolicy = cron {
      seconds = "0"
      minutes = "0"
      hours = "5"
      dayOfMonth = "?"
      month = "*"
      // Quartz counts the days from Sunday, so 3 is Tuesday, and `#n` picks its n-th occurrence.
      dayOfWeek = "3#$tuesdayOfMonth"
      year = "*"
    }
    branchFilter = ""
    // Nothing new on master means nothing to publish and nowhere new to move the release branch.
    withPendingChangesOnly = true
  }
}
