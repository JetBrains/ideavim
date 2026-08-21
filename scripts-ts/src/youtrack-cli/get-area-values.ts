#!/usr/bin/env npx tsx
/**
 * CLI wrapper to list the possible Area values of the VIM project
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/get-area-values.ts [--json]
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/get-area-values.ts
 *   npx tsx scripts-ts/src/youtrack-cli/get-area-values.ts --json
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { getAreaValues } from "../tools/youtrack.js";

async function main() {
  const jsonOutput = process.argv.slice(2).includes("--json");

  try {
    const values = await getAreaValues();

    if (jsonOutput) {
      console.log(JSON.stringify(values, null, 2));
    } else {
      console.log(`Area values (${values.length}):`);
      for (const value of values) {
        const description = value.description ? ` - ${value.description}` : "";
        console.log(`  ${value.name}${description}`);
      }
    }
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
