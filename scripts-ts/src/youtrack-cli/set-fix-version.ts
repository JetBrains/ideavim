#!/usr/bin/env npx tsx
/**
 * CLI wrapper to set the fix version of a YouTrack ticket
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/set-fix-version.ts <ticket-id> <version>
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/set-fix-version.ts VIM-1234 "2.28.0"
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { setFixVersion } from "../tools/youtrack.js";

async function main() {
  const args = process.argv.slice(2);

  if (args.length !== 2) {
    console.error("Usage: set-fix-version.ts <ticket-id> <version>");
    console.error("Example: set-fix-version.ts VIM-1234 \"2.28.0\"");
    process.exit(1);
  }

  const [ticketId, version] = args;

  try {
    await setFixVersion(ticketId, version);
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
