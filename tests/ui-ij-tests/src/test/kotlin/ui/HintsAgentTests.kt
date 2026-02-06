/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ui.utils.StepsLogger
import ui.utils.agent.ActionExecutor
import ui.utils.agent.AgentOrchestrator
import ui.utils.agent.ClaudeVisionClient
import ui.utils.uiTest
import kotlin.test.assertTrue

/**
 * LLM-powered visual tests for the IdeaVim hints feature.
 *
 * These tests use Claude's vision API to take screenshots of the IDE, analyze them,
 * and verify that hint overlays appear correctly and behave as expected.
 *
 * Tests are skipped when the ANTHROPIC_API_KEY environment variable is not set.
 */
class HintsAgentTests {
  init {
    StepsLogger.init()
  }

  private val apiKey: String? = System.getenv("ANTHROPIC_API_KEY")

  @BeforeEach
  fun checkApiKey() {
    Assumptions.assumeTrue(
      apiKey != null && apiKey.isNotBlank(),
      "ANTHROPIC_API_KEY environment variable is not set — skipping agent tests"
    )
  }

  private fun runAgentTest(
    testName: String,
    goal: String,
    maxSteps: Int = 15,
  ) = uiTest(testName) {
    val client = ClaudeVisionClient(apiKey)
    val executor = ActionExecutor(this)
    val orchestrator = AgentOrchestrator(this, client, executor)

    val result = orchestrator.run(testName, goal, maxSteps)

    assertTrue(result.success, buildString {
      appendLine("Agent test '$testName' failed after ${result.steps.size} steps.")
      appendLine("Final observation: ${result.finalObservation}")
      appendLine()
      appendLine("Step history:")
      result.steps.forEach { step ->
        appendLine("  Step ${step.stepNumber}: ${step.response.status}")
        appendLine("    Observation: ${step.response.observation}")
        step.response.assertion?.let { a ->
          appendLine("    Assertion: ${a.description} -> ${if (a.passed) "PASSED" else "FAILED"}")
          if (a.details.isNotEmpty()) appendLine("    Details: ${a.details}")
        }
        step.actionResult?.let { appendLine("    Action result: $it") }
      }
    })
  }

  /**
   * Verify that hint labels visually appear when the hints feature is triggered.
   *
   * Steps the agent should follow:
   * 1. Enable vimhints via `:set vimhints`
   * 2. Trigger hints with Ctrl+\
   * 3. Verify yellow labels are visible on the screenshot
   * 4. Dismiss hints with Escape
   */
  @Test
  fun hintsAppearVisually() = runAgentTest(
    testName = "hintsAppearVisually",
    goal = """
      Verify that hint labels appear visually when the hints feature is triggered.

      Steps:
      1. First, enable the hints feature by running the Vim command: set vimhints
      2. Then trigger hints by pressing Ctrl+\ (use keyboard action with keys "ctrl+BACK_SLASH")
      3. Take a screenshot and verify that yellow labels (hint overlays) are visible on interactive UI elements
      4. The labels should contain characters from the alphabet: A, S, D, F, G, H, J, K, L
      5. Dismiss the hints by pressing Escape
      6. Verify the hints are no longer visible

      Mark SUCCESS if yellow hint labels appeared and were then dismissed.
      Mark FAILURE if no hint labels appeared after triggering, or if they don't look correct.
    """.trimIndent(),
  )

  /**
   * Verify that hint labels contain only valid characters from the hints alphabet.
   *
   * Steps the agent should follow:
   * 1. Enable vimhints and trigger hints
   * 2. Read the label text from the visible hints
   * 3. Verify labels use only A, S, D, F, G, H, J, K, L characters
   * 4. Count the visible labels
   * 5. Dismiss hints
   */
  @Test
  fun hintContentIsCorrect() = runAgentTest(
    testName = "hintContentIsCorrect",
    goal = """
      Verify that hint label content is correct.

      Steps:
      1. Enable the hints feature: run Vim command "set vimhints"
      2. Trigger hints with Ctrl+\ (keyboard action: "ctrl+BACK_SLASH")
      3. Examine the visible hint labels carefully
      4. Assert that ALL labels contain only characters from the valid alphabet: A, S, D, F, G, H, J, K, L
      5. Count how many hint labels are visible — there should be at least several (the IDE has many clickable elements)
      6. If there are more than 9 hints, labels should be multi-character (e.g., "AS", "AD")
      7. Dismiss hints with Escape

      Mark SUCCESS if all labels are valid and there are a reasonable number of them.
      Mark FAILURE if any label contains characters outside the alphabet, or if there are no labels.
    """.trimIndent(),
  )

  /**
   * Verify that selecting a hint label activates the corresponding UI element.
   *
   * Steps the agent should follow:
   * 1. Enable vimhints and trigger hints
   * 2. Identify a hint label on a visible UI element
   * 3. Type the hint characters to select it
   * 4. Verify the target was activated (green highlight or UI change)
   * 5. Verify hints are dismissed after selection
   */
  @Test
  fun hintSelectionWorks() = runAgentTest(
    testName = "hintSelectionWorks",
    goal = """
      Verify that typing a hint label activates the corresponding UI element.

      Steps:
      1. Enable the hints feature: run Vim command "set vimhints"
      2. Trigger hints with Ctrl+\ (keyboard action: "ctrl+BACK_SLASH")
      3. Wait briefly for hints to render (wait 500ms)
      4. Identify a hint label on a clearly visible UI element (like a toolbar button or tab)
      5. Type the hint character(s) using the keyboard action to select that hint
      6. After selection, the target should briefly highlight in green and hints should be dismissed
      7. Verify the hints overlay is gone after selection

      Mark SUCCESS if typing the hint label caused the target to activate and hints to dismiss.
      Mark FAILURE if the hint selection didn't work or hints remained visible.
    """.trimIndent(),
  )

  /**
   * Full end-to-end lifecycle test of the hints feature.
   *
   * Covers: no hints initially -> enable -> trigger -> verify appearance -> select -> verify dismiss -> re-trigger -> escape dismiss
   */
  @Test
  fun fullEndToEndFlow() = runAgentTest(
    testName = "fullEndToEndFlow",
    goal = """
      Test the complete lifecycle of the hints feature.

      Steps:
      1. First, verify the IDE is in a normal state with no hint overlays visible
      2. Enable the hints feature: run Vim command "set vimhints"
      3. Trigger hints with Ctrl+\ (keyboard action: "ctrl+BACK_SLASH")
      4. Verify yellow hint labels appear on interactive elements
      5. Select a hint by typing its label character(s)
      6. Verify hints are dismissed after selection and a green highlight briefly appears
      7. Wait 600ms for the green highlight to fade
      8. Verify the IDE returns to normal (no overlays)
      9. Re-trigger hints with Ctrl+\ again
      10. Verify hints appear again
      11. Press Escape to dismiss hints without selecting
      12. Verify hints are gone

      Mark SUCCESS only after completing ALL steps above.
      Mark FAILURE if any step doesn't behave as expected.
    """.trimIndent(),
    maxSteps = 20,
  )

  /**
   * Exploratory test where the agent tries creative edge cases.
   *
   * The agent is given freedom to discover and test unusual scenarios
   * like invalid keys, rapid re-triggering, hints with dialogs open, etc.
   */
  @Test
  fun creativeScenarioDiscovery() = runAgentTest(
    testName = "creativeScenarioDiscovery",
    goal = """
      Explore edge cases and creative scenarios for the hints feature.
      You have freedom to try unusual things and discover potential issues.

      Start by enabling vimhints (Vim command "set vimhints"), then explore scenarios like:
      - Trigger hints and type an invalid key (not in A,S,D,F,G,H,J,K,L) — hints should dismiss with error
      - Trigger hints, type the first character of a multi-char hint, observe that hints narrow down to only matching labels
      - Trigger and dismiss hints rapidly multiple times
      - Trigger hints, select one, then immediately re-trigger — should work cleanly
      - Any other creative scenarios you can think of

      For each scenario:
      1. Describe what you're testing
      2. Perform the actions
      3. Verify the result
      4. Move to the next scenario

      Mark SUCCESS if you tested at least 3 different scenarios and all behaved reasonably.
      Mark FAILURE if you find a clear bug or crash.
    """.trimIndent(),
    maxSteps = 25,
  )
}
