#!/usr/bin/env npx tsx
/**
 * CLI wrapper to get details of a YouTrack ticket
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/get-ticket.ts <ticket-id> [--json]
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/get-ticket.ts VIM-1234
 *   npx tsx scripts-ts/src/youtrack-cli/get-ticket.ts VIM-1234 --json
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { getTicketDetails, getTicketComments } from "../tools/youtrack.js";

async function main() {
  const args = process.argv.slice(2);

  if (args.length < 1) {
    console.error("Usage: get-ticket.ts <ticket-id> [--json]");
    console.error("Example: get-ticket.ts VIM-1234");
    process.exit(1);
  }

  const ticketId = args[0];
  const jsonOutput = args.includes("--json");

  try {
    const details = await getTicketDetails(ticketId);
    const comments = await getTicketComments(ticketId);

    if (jsonOutput) {
      console.log(JSON.stringify({ ...details, comments }, null, 2));
    } else {
      console.log(`Ticket: ${details.id}`);
      console.log(`Summary: ${details.summary}`);
      console.log(`State: ${details.state}`);
      console.log(`Created: ${details.created}`);
      console.log("");
      console.log("Description:");
      console.log(details.description || "(no description)");
      console.log("");
      console.log(`Comments (${comments.length}):`);
      for (const comment of comments) {
        console.log(`  [${comment.created}] ${comment.author}:`);
        console.log(`    ${comment.text.substring(0, 100)}${comment.text.length > 100 ? "..." : ""}`);
      }
    }
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
