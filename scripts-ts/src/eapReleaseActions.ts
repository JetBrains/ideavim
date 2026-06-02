#!/usr/bin/env tsx
/**
 * Marks Ready To Release YouTrack tickets as released in the current EAP build.
 *
 * Usage:
 *   npx tsx scripts-ts/src/eapReleaseActions.ts [version]
 *
 * When no version is passed, reads ORG_GRADLE_PROJECT_version from the environment.
 */

import { resolveReleaseVersion, runEapReleaseActions } from "./eapReleaseActionsCore.js";

const args = process.argv.slice(2);
runEapReleaseActions(resolveReleaseVersion(args, process.env)).catch((error) => {
  console.error(`Error: ${error instanceof Error ? error.message : error}`);
  process.exit(1);
});
