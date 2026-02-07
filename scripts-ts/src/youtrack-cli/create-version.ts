#!/usr/bin/env npx tsx
/**
 * CLI wrapper to create a new release version in YouTrack
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/create-version.ts <version-name>
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/create-version.ts "2.28.0"
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { createReleaseVersion } from "../tools/youtrack.js";

async function main() {
  const args = process.argv.slice(2);

  if (args.length !== 1) {
    console.error("Usage: create-version.ts <version-name>");
    console.error("Example: create-version.ts \"2.28.0\"");
    process.exit(1);
  }

  const [versionName] = args;

  try {
    const versionId = await createReleaseVersion(versionName);
    console.log(`Version ID: ${versionId}`);
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
