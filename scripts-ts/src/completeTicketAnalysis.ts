#!/usr/bin/env tsx
/**
 * Completes the YouTrack ticket analysis by tagging and optionally commenting.
 *
 * Arguments:
 * - argv[2]: ticket ID (e.g., "VIM-1234")
 * - argv[3]: analysis result ("suitable", "unsuitable", or "error")
 * - argv[4]: PR URL (optional, only present if a PR was created)
 *
 * Actions:
 * - Always tags the ticket with "claude-analyzed"
 * - Adds a comment only if result is "suitable" or "error"
 */

import { setTag, addComment, CLAUDE_ANALYZED_TAG_ID } from "./youtrack.js";

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
  const ticketId = process.argv[2];
  const analysisResult = process.argv[3] || "unknown";
  const prUrl = process.argv[4] || null;

  if (!ticketId) {
    console.log("No ticket ID provided, skipping completion");
    return;
  }

  console.log(`Completing analysis for ticket: ${ticketId}`);
  console.log(`Analysis result: ${analysisResult}`);
  if (prUrl) {
    console.log(`PR URL: ${prUrl}`);
  }

  // Always tag the ticket to exclude from future analysis runs
  console.log("Tagging ticket with 'claude-analyzed'...");
  await setTag(ticketId, CLAUDE_ANALYZED_TAG_ID);
  console.log("Ticket tagged successfully");

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
