package _Self.buildTypes

import _Self.AgentSize
import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

object SlackNotificationTest : IdeaVimBuildType({
  name = "Test Slack Notification"
  description = "Test Slack notification flow with changelog generation"

  params {
    param("env.TEST_VERSION", "2.99.0-test")
    password(
      "env.ORG_GRADLE_PROJECT_slackUrl",
      "credentialsJSON:a8ab8150-e6f8-4eaf-987c-bcd65eac50b5",
      label = "Slack Token"
    )
    password(
      "env.ANTHROPIC_API_KEY",
      "credentialsJSON:712a6516-4f43-41dc-b9e9-e32b1453dde8",
      label = "Anthropic API Key",
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
      name = "Debug API Key"
      scriptContent = """
        #!/bin/bash
        echo "Checking ANTHROPIC_API_KEY..."
        if [ -z "${'$'}ANTHROPIC_API_KEY" ]; then
          echo "ERROR: ANTHROPIC_API_KEY is empty!"
          exit 1
        fi
        echo "ANTHROPIC_API_KEY is set (length: ${'$'}{#ANTHROPIC_API_KEY} chars)"
        echo "First 10 chars: ${'$'}{ANTHROPIC_API_KEY:0:10}..."

        echo ""
        echo "Testing API key with simple request..."
        response=${'$'}(curl -s -w "\n%{http_code}" https://api.anthropic.com/v1/messages \
          -H "Content-Type: application/json" \
          -H "x-api-key: ${'$'}ANTHROPIC_API_KEY" \
          -H "anthropic-version: 2023-06-01" \
          -d '{"model":"claude-sonnet-4-20250514","max_tokens":10,"messages":[{"role":"user","content":"hi"}]}')

        http_code=${'$'}(echo "${'$'}response" | tail -n1)
        body=${'$'}(echo "${'$'}response" | sed '${'$'}d')

        echo "HTTP Code: ${'$'}http_code"
        echo "Response: ${'$'}body"

        if [ "${'$'}http_code" != "200" ]; then
          echo "API key validation failed!"
          exit 1
        fi
        echo "API key is valid!"
      """.trimIndent()
    }
    script {
      name = "Generate Changelog JSON"
      scriptContent = """
        # Install Claude Code CLI if not present
        if ! command -v claude &> /dev/null; then
          echo "Installing Claude Code CLI..."
          npm install -g @anthropic-ai/claude-code
        fi

        # Generate changelog and save to file
        claude -p "$(cat <<'PROMPT'
        Read CHANGES.md and extract the "To Be Released" or "Unreleased" section.

        Generate a Slack Block Kit JSON message for IdeaVim version %env.TEST_VERSION%.

        REQUIREMENTS:
        - Output ONLY the raw JSON, no markdown code blocks, no explanation
        - Valid JSON format: { "text": "...", "blocks": [...] }
        - Use Slack mrkdwn: *bold*, _italic_, <url|text> for links
        - Keep it concise
        - Calm, professional tone - no emojis, no excitement
        - Simple factual announcement

        Output the JSON to stdout only.
        PROMPT
        )" > /tmp/slack_message.json

        echo "Generated Slack message:"
        cat /tmp/slack_message.json
      """.trimIndent()
    }
    script {
      name = "Send Slack Notification"
      scriptContent = """
        echo "Sending Slack notification..."

        if [ ! -f /tmp/slack_message.json ]; then
          echo "ERROR: /tmp/slack_message.json not found"
          exit 1
        fi

        # Send to Slack
        response=${'$'}(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" \
          -d @/tmp/slack_message.json \
          "${'$'}ORG_GRADLE_PROJECT_slackUrl")

        http_code=${'$'}(echo "${'$'}response" | tail -n1)
        body=${'$'}(echo "${'$'}response" | sed '${'$'}d')

        echo "Response: ${'$'}body"
        echo "HTTP Code: ${'$'}http_code"

        if [ "${'$'}http_code" != "200" ]; then
          echo "ERROR: Slack notification failed"
          exit 1
        fi

        echo "Slack notification sent successfully!"
      """.trimIndent()
    }
  }

  requirements {
    equals("teamcity.agent.hardware.cpuCount", AgentSize.MEDIUM)
    equals("teamcity.agent.os.family", "Linux")
  }
})
