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
      label = "Slack Token"
    )
    password(
      "env.YOUTRACK_TOKEN",
      "credentialsJSON:eedfa0eb-c329-462a-b7b4-bc263bda8c01",
      display = ParameterDisplay.HIDDEN
    )
    password(
      "env.ANTHROPIC_API_KEY",
      "credentialsJSON:712a6516-4f43-41dc-b9e9-e32b1453dde8",
      label = "Anthropic API Key"
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
      jdkHome = "/usr/lib/jvm/java-21-amazon-corretto"
    }
    script {
      name = "Slack Notification"
      scriptContent = """
        # Install Claude Code CLI if not present
        if ! command -v claude &> /dev/null; then
          echo "Installing Claude Code CLI..."
          npm install -g @anthropic-ai/claude-code
        fi

        claude -p "$(cat <<'PROMPT'
        Send a Slack notification for IdeaVim EAP release to the internal team.

        TASK:
        1. Read CHANGES.md and extract the changelog. For EAP releases, include the "To Be Released" or "Unreleased" section if present, as EAP versions contain upcoming changes.
        2. Generate a valid Slack Block Kit JSON message for IdeaVim EAP version %build.number%
        3. Send it to the Slack webhook URL stored in env var ORG_GRADLE_PROJECT_slackUrl
        4. If Slack returns an error, analyze the error and fix the JSON, then retry (max 3 attempts)

        TONE AND STYLE:
        - This is an internal team notification, use a calm, professional tone
        - No excitement, no emojis, no celebratory language
        - Simple factual announcement: "IdeaVim EAP %build.number% has been released"
        - List key changes briefly

        SLACK MESSAGE FORMAT:
        - Valid JSON: { "text": "...", "blocks": [...] }
        - Use Slack mrkdwn: *bold*, _italic_, <url|text> for links
        - Keep it concise and informative

        Use the Bash tool with curl to POST the JSON to the webhook URL.
        The webhook URL is available in the ORG_GRADLE_PROJECT_slackUrl environment variable.
        Report success or failure at the end.
        PROMPT
        )"
      """.trimIndent()
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
