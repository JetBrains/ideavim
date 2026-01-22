#!/usr/bin/env tsx
/**
 * Updates YouTrack tickets to "Ready To Release" status when commits with
 * fix(VIM-XXXX): pattern are pushed to master.
 *
 * Reads commits between HEAD and SUCCESS_COMMIT env var, extracts ticket IDs
 * from commit messages, and updates their status in YouTrack.
 *
 * Environment:
 * - SUCCESS_COMMIT: Git hash of last successful workflow run
 * - YOUTRACK_TOKEN: Bearer token for YouTrack API
 */

import { execSync } from "child_process";
import { setStatus } from "./tools/youtrack.js";

interface Change {
  id: string;
  text: string;
}

function getChanges(projectDir: string): Change[] {
  const lastSuccessfulCommit = process.env.SUCCESS_COMMIT;
  if (!lastSuccessfulCommit) {
    throw new Error("SUCCESS_COMMIT environment variable is not set");
  }

  console.log(`Last successful commit: ${lastSuccessfulCommit}`);

  // Get commit messages between HEAD and last successful commit
  const gitOutput = execSync(
    `git log --format=%s ${lastSuccessfulCommit}..HEAD`,
    { cwd: projectDir, encoding: "utf-8" }
  );

  const messages = gitOutput.trim().split("\n").filter(Boolean);
  console.log(`Amount of commits: ${messages.length}`);
  console.log("Start changes processing");

  const changes: Change[] = [];
  const regex = /^fix\((vim-\d+)\):/i;

  for (const message of messages) {
    console.log(`Processing '${message}'...`);
    const match = message.match(regex);

    if (match) {
      console.log("Message matches");
      const ticketId = match[1].toUpperCase();
      const shortMessage = message.slice(match[0].length).trim();
      changes.push({ id: ticketId, text: shortMessage });
    } else {
      console.log("Message doesn't match");
    }
  }

  return changes;
}

async function main(): Promise<void> {
  const projectDir = process.argv[2] || ".";

  console.log("Start updating youtrack");
  console.log(`Project directory: ${projectDir}`);

  const changes = getChanges(projectDir);
  const ticketIds = changes.map((c) => c.id);

  console.log(`Set new status for ${JSON.stringify(ticketIds)}`);

  for (const ticketId of ticketIds) {
    await setStatus(ticketId, "Ready To Release");
  }

  console.log("Done");
}

main().catch((error) => {
  console.error("Error:", error.message);
  process.exit(1);
});
