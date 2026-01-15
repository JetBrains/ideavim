#!/usr/bin/env npx tsx
/**
 * CLI wrapper to delete a release version from YouTrack
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/delete-version.ts <version-name>
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/delete-version.ts "2.28.0"
 *
 * Note: This looks up the version by name and deletes it.
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { getVersionIdByName, deleteVersionById } from "../tools/youtrack.js";

async function main() {
  const args = process.argv.slice(2);

  if (args.length !== 1) {
    console.error("Usage: delete-version.ts <version-name>");
    console.error("Example: delete-version.ts \"2.28.0\"");
    process.exit(1);
  }

  const [versionName] = args;

  try {
    const versionId = await getVersionIdByName(versionName);

    if (!versionId) {
      console.error(`Error: Version "${versionName}" not found`);
      process.exit(1);
    }

    await deleteVersionById(versionId);
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
