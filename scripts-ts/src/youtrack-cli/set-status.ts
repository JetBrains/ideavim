#!/usr/bin/env npx tsx
/**
 * CLI wrapper to set the status of a YouTrack ticket
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/set-status.ts <ticket-id> <status>
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/set-status.ts VIM-1234 "Ready To Release"
 *   npx tsx scripts-ts/src/youtrack-cli/set-status.ts VIM-1234 "Open"
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { setStatus } from "../tools/youtrack.js";

async function main() {
  const args = process.argv.slice(2);

  if (args.length !== 2) {
    console.error("Usage: set-status.ts <ticket-id> <status>");
    console.error("Example: set-status.ts VIM-1234 \"Ready To Release\"");
    process.exit(1);
  }

  const [ticketId, status] = args;

  try {
    await setStatus(ticketId, status);
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
