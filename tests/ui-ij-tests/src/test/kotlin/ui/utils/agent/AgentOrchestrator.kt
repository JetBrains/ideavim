/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.utils.agent

import com.intellij.remoterobot.RemoteRobot
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Drives the perceive-reason-act loop:
 * 1. Take screenshot via Remote Robot
 * 2. Send screenshot + prompt to Claude
 * 3. Parse structured response
 * 4. Execute the action
 * 5. Repeat until success/failure or max steps
 */
class AgentOrchestrator(
  private val remoteRobot: RemoteRobot,
  private val visionClient: ClaudeVisionClient,
  private val actionExecutor: ActionExecutor,
) {

  /**
   * Run the agent loop for a given test goal.
   *
   * @param testName Name used for screenshot directory
   * @param goal Description of what the test should verify
   * @param maxSteps Maximum number of perceive-reason-act iterations
   * @return [TestResult] with success/failure and step history
   */
  fun run(testName: String, goal: String, maxSteps: Int = 15): TestResult {
    val screenshotDir = File("build/reports/agent/$testName").apply { mkdirs() }
    val steps = mutableListOf<StepRecord>()
    var lastObservation = ""

    for (step in 1..maxSteps) {
      // 1. Perceive: take screenshot
      val screenshot = takeScreenshot(remoteRobot)
      saveScreenshot(screenshot, screenshotDir, step)

      // 2. Reason: send to Claude
      val prompt = buildPrompt(goal, step, maxSteps, steps.lastOrNull())
      println("[Agent Step $step/$maxSteps] Sending screenshot to Claude...")

      val response = try {
        visionClient.analyzeScreenshot(screenshot, prompt)
      } catch (e: Exception) {
        println("[Agent Step $step] Error from Claude: ${e.message}")
        val errorResponse = AgentResponse(
          observation = "Error communicating with Claude: ${e.message}",
          reasoning = "Cannot continue due to API error",
          action = null,
          assertion = null,
          status = AgentStatus.FAILURE,
        )
        steps.add(StepRecord(step, errorResponse, null))
        return TestResult(success = false, steps = steps, finalObservation = errorResponse.observation)
      }

      println("[Agent Step $step] Status: ${response.status}")
      println("[Agent Step $step] Observation: ${response.observation}")
      println("[Agent Step $step] Reasoning: ${response.reasoning}")
      response.assertion?.let { assertion ->
        println("[Agent Step $step] Assertion: ${assertion.description} -> ${if (assertion.passed) "PASSED" else "FAILED"}")
      }

      lastObservation = response.observation

      // 3. Act: execute the action if present
      val actionResult = response.action?.let { action ->
        println("[Agent Step $step] Action: ${action.type} ${action.params}")
        try {
          val result = actionExecutor.execute(action)
          // Brief pause after action to let UI settle
          Thread.sleep(300)
          println("[Agent Step $step] Action result: $result")
          result
        } catch (e: Exception) {
          val error = "Action failed: ${e.message}"
          println("[Agent Step $step] $error")
          error
        }
      }

      steps.add(StepRecord(step, response, actionResult))

      // Check terminal status
      when (response.status) {
        AgentStatus.SUCCESS -> {
          println("[Agent] Test PASSED after $step steps")
          return TestResult(success = true, steps = steps, finalObservation = lastObservation)
        }

        AgentStatus.FAILURE -> {
          println("[Agent] Test FAILED after $step steps")
          return TestResult(success = false, steps = steps, finalObservation = lastObservation)
        }

        AgentStatus.CONTINUE -> { /* keep going */
        }
      }
    }

    println("[Agent] Test reached max steps ($maxSteps) without resolution")
    return TestResult(
      success = false,
      steps = steps,
      finalObservation = "Max steps ($maxSteps) reached. $lastObservation"
    )
  }

  private fun buildPrompt(goal: String, step: Int, maxSteps: Int, lastStep: StepRecord?): String {
    val sb = StringBuilder()
    sb.appendLine("## Test Goal")
    sb.appendLine(goal)
    sb.appendLine()
    sb.appendLine("## Current Step: $step of $maxSteps")

    if (lastStep != null) {
      sb.appendLine()
      sb.appendLine("## Previous Step Result")
      lastStep.actionResult?.let { sb.appendLine("Action result: $it") }
      lastStep.response.assertion?.let { assertion ->
        sb.appendLine("Previous assertion: ${assertion.description} -> ${if (assertion.passed) "PASSED" else "FAILED"}")
      }
    }

    if (step == 1) {
      sb.appendLine()
      sb.appendLine("This is the first step. Analyze the current IDE state and begin working toward the test goal.")
    }

    sb.appendLine()
    sb.appendLine("Analyze the screenshot and respond with a JSON object.")
    return sb.toString()
  }

  private fun takeScreenshot(robot: RemoteRobot): BufferedImage {
    return robot.getScreenshot()
  }

  private fun saveScreenshot(image: BufferedImage, dir: File, step: Int) {
    val file = dir.resolve("step-$step.png")
    ImageIO.write(image, "png", file)
    println("[Agent] Screenshot saved: ${file.absolutePath}")
  }
}
