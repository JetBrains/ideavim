#!/usr/bin/env tsx
/**
 * Completes the YouTrack ticket analysis by tagging and optionally commenting.
 *
 * Reads from: analysis_state.json in the project root
 *
 * Actions:
 * - Always tags the ticket with "claude-analyzed"
 * - Adds a comment only if result is "suitable" or "error"
 */

import { readFileSync, existsSync } from "fs";
import { setTag, addComment, CLAUDE_ANALYZED_TAG_ID } from "./youtrack.js";

interface AnalysisState {
  ticket_id: string;
  ticket_summary: string;
  has_pending_clarification: boolean;
  ticket_type: string | null;
  triage_result: string | null;
  triage_reason: string | null;
  check_answer: {
    status: string;
    attention_reason: string | null;
  };
  planning: {
    status: string;
    plan: string | null;
    questions: string | null;
    attention_reason: string | null;
  };
  implementation: {
    status: string;
    changed_files: string[];
    test_files: string[];
    notes: string | null;
    attention_reason: string | null;
  };
  review: {
    status: string;
    notes: string | null;
  };
  pr: {
    url: string | null;
    branch: string | null;
    attention_reason: string | null;
  };
  final_result: string | null;
}

function buildComment(analysisResult: string, prUrl: string | null): string {
  let content = "**Claude Code Automated Analysis**\n\n";

  switch (analysisResult) {
    case "suitable":
      content +=
        "This ticket was analyzed and determined to be suitable for automated fixing.";
      if (prUrl) {
        content += `\n\nA pull request has been created: ${prUrl}`;
      }
      break;
    case "error":
      content +=
        "An error occurred during automated analysis of this ticket.\nManual review may be required.";
      break;
    default:
      content += `Analysis completed with result: ${analysisResult}`;
  }

  content +=
    "\n\n---\n*This comment was generated automatically by the YouTrack Auto-Analysis workflow.*";

  return content;
}

async function main(): Promise<void> {
  const projectDir = process.argv[2] || "..";
  const statePath = `${projectDir}/analysis_state.json`;

  if (!existsSync(statePath)) {
    console.log(`No analysis_state.json found at ${statePath}, skipping completion`);
    return;
  }

  const state: AnalysisState = JSON.parse(readFileSync(statePath, "utf-8"));
  const ticketId = state.ticket_id;
  const analysisResult = state.final_result || "unknown";
  const prUrl = state.pr?.url ?? null;

  if (!ticketId) {
    console.log("No ticket ID in analysis state, skipping completion");
    return;
  }

  console.log(`Completing analysis for ticket: ${ticketId}`);
  console.log(`Analysis result: ${analysisResult}`);
  if (prUrl) {
    console.log(`PR URL: ${prUrl}`);
  }

  // Tag the ticket to exclude from future analysis runs
  // EXCEPT when clarification is needed or no answer yet (so it can be picked up again)
  if (analysisResult === "needs_clarification" || analysisResult === "no_answer") {
    console.log(`Result is '${analysisResult}' - skipping tag so ticket can be picked up again`);
  } else {
    console.log("Tagging ticket with 'claude-analyzed'...");
    await setTag(ticketId, CLAUDE_ANALYZED_TAG_ID);
    console.log("Ticket tagged successfully");
  }

  // Only add comment if explicitly "suitable" (PR created) or "error"
  // For "unsuitable" or unknown results, we just tag without commenting
  if (analysisResult === "suitable" || analysisResult === "error") {
    const comment = buildComment(analysisResult, prUrl);
    console.log("Adding private comment to ticket...");
    await addComment(ticketId, comment, true); // Private comment (JetBrains team only)
    console.log("Comment added successfully");
  } else {
    console.log(`Result is '${analysisResult}', skipping comment (only tag)`);
  }

  console.log(`Analysis completion finished for ${ticketId}`);
}

main().catch((error) => {
  console.error("Error:", error.message);
  process.exit(1);
});
