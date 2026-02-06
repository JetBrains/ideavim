/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.utils.agent

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.TextBlockParam
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

/**
 * Client that sends screenshots to Claude's vision API and receives structured JSON responses.
 * Maintains conversation history for multi-step context within a single test.
 */
class ClaudeVisionClient(apiKey: String? = null) {

  private val client: AnthropicClient = if (apiKey != null) {
    AnthropicOkHttpClient.builder().apiKey(apiKey).build()
  } else {
    AnthropicOkHttpClient.fromEnv()
  }

  private val mapper = jacksonObjectMapper()

  private val paramsBuilder: MessageCreateParams.Builder = MessageCreateParams.builder()
    .model(Model.CLAUDE_SONNET_4_5_20250929)
    .maxTokens(2048)
    .system(SYSTEM_PROMPT)

  /**
   * Send a screenshot with a text prompt to Claude and get a structured response.
   * The conversation history is automatically maintained across calls.
   */
  fun analyzeScreenshot(screenshot: BufferedImage, prompt: String): AgentResponse {
    val base64Image = encodeImage(screenshot)

    val imageBlock = ContentBlockParam.ofImage(
      ImageBlockParam.builder()
        .source(
          Base64ImageSource.builder()
            .mediaType(Base64ImageSource.MediaType.IMAGE_PNG)
            .data(base64Image)
            .build()
        )
        .build()
    )
    val textBlock = ContentBlockParam.ofText(TextBlockParam.builder().text(prompt).build())

    paramsBuilder.addUserMessageOfBlockParams(listOf(imageBlock, textBlock))

    val message = client.messages().create(paramsBuilder.build())

    // Add assistant response to conversation history
    paramsBuilder.addMessage(message)

    val responseText = message.content()
      .mapNotNull { block -> block.text().orElse(null) }
      .joinToString("") { it.text() }

    return parseResponse(responseText)
  }

  private fun encodeImage(image: BufferedImage): String {
    val baos = ByteArrayOutputStream()
    ImageIO.write(image, "png", baos)
    return Base64.getEncoder().encodeToString(baos.toByteArray())
  }

  private fun parseResponse(text: String): AgentResponse {
    // Extract JSON from the response — Claude might wrap it in markdown code blocks
    val jsonStr = extractJson(text)
    return mapper.readValue(jsonStr)
  }

  private fun extractJson(text: String): String {
    // Try to find JSON block in markdown code fence
    val fencePattern = Regex("```(?:json)?\\s*\\n?(\\{.*?})\\s*\\n?```", RegexOption.DOT_MATCHES_ALL)
    fencePattern.find(text)?.let { return it.groupValues[1] }

    // Try to find a raw JSON object
    val jsonPattern = Regex("(\\{.*})", RegexOption.DOT_MATCHES_ALL)
    jsonPattern.find(text)?.let { return it.groupValues[1] }

    throw IllegalStateException("Could not extract JSON from Claude response: $text")
  }

  companion object {
    private const val SYSTEM_PROMPT = """You are a test agent for the IdeaVim plugin's "hints" feature in IntelliJ IDEA.

## Feature Description
The hints feature (triggered by Ctrl+\ or Cmd+\) overlays yellow labels on clickable UI components, allowing keyboard-driven navigation. When activated:
- Yellow labels appear on interactive elements (buttons, tabs, tool windows, etc.)
- Labels use characters from the alphabet: A, S, D, F, G, H, J, K, L
- With many targets (>9), labels become multi-character combinations (e.g., "AS", "AD", "AF")
- Typing a label's character(s) activates that UI element
- After selecting a hint, the target briefly highlights in green (500ms)
- Pressing Escape dismisses the hints without selecting anything
- The feature must be enabled via `:set vimhints` in IdeaVim's command line

## Your Task
You are analyzing screenshots of the IDE to verify the hints feature works correctly. For each screenshot:
1. Observe what you see (hint labels, their positions, colors, the IDE state)
2. Reason about whether the state matches expectations
3. Decide on the next action to take
4. Make assertions about correctness

## Response Format
You MUST respond with a single JSON object (no markdown fences, no extra text):
{
  "observation": "What you see in the screenshot",
  "reasoning": "Your analysis and thought process",
  "action": {
    "type": "keyboard|invoke_action|click_text|wait|vim_command|escape",
    "params": {"key": "value"}
  },
  "assertion": {
    "description": "What you're checking",
    "passed": true,
    "details": "Additional details"
  },
  "status": "CONTINUE|SUCCESS|FAILURE"
}

## Action Types
- keyboard: Type keys. params: {"keys": "text to type"} or {"keys": "ctrl+BACK_SLASH"} for shortcuts
- invoke_action: Trigger an IDE action. params: {"action_id": "com.maddyhome.idea.vim.extension.hints.ToggleHintsAction"}
- click_text: Click on visible text. params: {"text": "text to click"}
- wait: Pause. params: {"duration_ms": "500"}
- vim_command: Execute a Vim command. params: {"command": "set vimhints"}
- escape: Press Escape. params: {}

## Important Notes
- Set status to CONTINUE when more steps are needed
- Set status to SUCCESS when the test goal is fully verified
- Set status to FAILURE when something is clearly wrong and cannot be recovered
- action can be null when status is SUCCESS or FAILURE (final step)
- assertion can be null if there's nothing to assert on this step"""
  }
}
