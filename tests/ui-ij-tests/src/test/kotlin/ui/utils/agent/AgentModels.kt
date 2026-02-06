/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.utils.agent

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Structured response from Claude describing what it observes, thinks, and wants to do next.
 */
data class AgentResponse(
  val observation: String,
  val reasoning: String,
  val action: AgentAction?,
  val assertion: AgentAssertion?,
  val status: AgentStatus,
)

/**
 * An action the agent wants to perform on the IDE.
 *
 * @param type One of: keyboard, invoke_action, click_text, wait, vim_command, escape
 * @param params Key-value parameters for the action (e.g., "keys", "action_id", "text", "duration_ms", "command")
 */
data class AgentAction(
  val type: String,
  val params: Map<String, String> = emptyMap(),
)

/**
 * An assertion the agent is making about the current state.
 */
data class AgentAssertion(
  val description: String,
  val passed: Boolean,
  val details: String = "",
)

/**
 * Status of the agent's test execution.
 */
enum class AgentStatus {
  @JsonProperty("CONTINUE")
  CONTINUE,
  @JsonProperty("SUCCESS")
  SUCCESS,
  @JsonProperty("FAILURE")
  FAILURE,
}

/**
 * Result of running the agent loop to completion.
 */
data class TestResult(
  val success: Boolean,
  val steps: List<StepRecord>,
  val finalObservation: String,
)

/**
 * Record of a single perceive-reason-act step.
 */
data class StepRecord(
  val stepNumber: Int,
  val response: AgentResponse,
  val actionResult: String?,
)
