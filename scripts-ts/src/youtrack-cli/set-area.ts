#!/usr/bin/env npx tsx
/**
 * CLI wrapper to set the Area field of a YouTrack ticket
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/set-area.ts <ticket-id> <area>...
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/set-area.ts VIM-1234 "Visual Mode"
 *   npx tsx scripts-ts/src/youtrack-cli/set-area.ts VIM-1234 "Visual Mode" "Caret position"
 *
 * Area is a multi-value field, so the given values replace every previous one.
 * Use get-area-values.ts to see the available names.
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { setArea } from "../tools/youtrack.js";

async function main() {
  const args = process.argv.slice(2);

  if (args.length < 2) {
    console.error("Usage: set-area.ts <ticket-id> <area>...");
    console.error('Example: set-area.ts VIM-1234 "Visual Mode" "Caret position"');
    process.exit(1);
  }

  const [ticketId, ...areaNames] = args;

  try {
    await setArea(ticketId, areaNames);
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
