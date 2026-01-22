#!/usr/bin/env npx tsx
/**
 * CLI wrapper to add a comment to a YouTrack ticket
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/add-comment.ts <ticket-id> <comment-text> [--private]
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/add-comment.ts VIM-1234 "This is a comment"
 *   npx tsx scripts-ts/src/youtrack-cli/add-comment.ts VIM-1234 "Private note" --private
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { addComment } from "../tools/youtrack.js";

async function main() {
  const args = process.argv.slice(2);

  if (args.length < 2) {
    console.error("Usage: add-comment.ts <ticket-id> <comment-text> [--private]");
    console.error("Example: add-comment.ts VIM-1234 \"This is a comment\"");
    process.exit(1);
  }

  const ticketId = args[0];
  const isPrivate = args.includes("--private");
  const textArgs = args.filter((arg) => arg !== "--private" && arg !== ticketId);
  const text = textArgs.join(" ");

  if (!text) {
    console.error("Error: Comment text is required");
    process.exit(1);
  }

  try {
    await addComment(ticketId, text, isPrivate);
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
