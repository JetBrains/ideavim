/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package _Self.buildTypes

import _Self.IdeaVimBuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

object SlackNotificationTest : IdeaVimBuildType({
  name = "Test Slack Notification"
  description = "Test Slack message generation with Claude Code (dry run)"

  vcs {
    root(DslContext.settingsRoot)
    branchFilter = "+:<default>"
    checkoutMode = CheckoutMode.AUTO
  }

  steps {
    script {
      name = "Install Claude Code CLI"
      scriptContent = """
        echo "Installing Claude Code CLI..."
        npm install -g @anthropic-ai/claude-code
        claude --version
      """.trimIndent()
    }
    script {
      name = "Test Slack Notification (dry run)"
      scriptContent = """
        claude -p "$(cat <<'PROMPT'
        You need to generate a Slack notification message for IdeaVim release.

        TASK:
        1. Read CHANGES.md and extract the latest version section (first ## header that is not "Unreleased")
        2. Generate a valid Slack Block Kit JSON message announcing the release
        3. This is a DRY RUN - just print the generated JSON, do not send it

        SLACK MESSAGE RULES:
        - Output valid JSON with structure: { "text": "...", "blocks": [...] }
        - Use Slack mrkdwn format: *bold*, _italic_, <url|text> for links
        - Keep it concise - summarize if changelog is long
        - Include version number in the announcement

        Print the generated JSON message.
        PROMPT
        )"
      """.trimIndent()
    }
  }
})
