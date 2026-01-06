#!/usr/bin/env npx tsx
/**
 * CLI wrapper to remove a tag from a YouTrack ticket
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/remove-tag.ts <ticket-id> <tag-id>
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/remove-tag.ts VIM-1234 68-507582
 *
 * Common tag IDs:
 *   68-507461 - claude-analyzed
 *   68-507582 - claude-pending-clarification
 *   68-385032 - IdeaVim Released In EAP
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { removeTag } from "../tools/youtrack.js";

async function main() {
  const args = process.argv.slice(2);

  if (args.length !== 2) {
    console.error("Usage: remove-tag.ts <ticket-id> <tag-id>");
    console.error("Example: remove-tag.ts VIM-1234 68-507582");
    console.error("");
    console.error("Common tag IDs:");
    console.error("  68-507461 - claude-analyzed");
    console.error("  68-507582 - claude-pending-clarification");
    console.error("  68-385032 - IdeaVim Released In EAP");
    process.exit(1);
  }

  const [ticketId, tagId] = args;

  try {
    await removeTag(ticketId, tagId);
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
