#!/usr/bin/env npx tsx
/**
 * Promotes the `## [To Be Released]` section of CHANGES.md to a versioned release section.
 *
 * Usage:
 *   npx tsx scripts-ts/src/promoteChangelog.ts <new-version> <release-type> [root-dir]
 *
 * Examples:
 *   npx tsx scripts-ts/src/promoteChangelog.ts 2.36.0 minor
 *   npx tsx scripts-ts/src/promoteChangelog.ts 2.35.3 patch /path/to/repo
 */

import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

export const TO_BE_RELEASED_HEADER = "## [To Be Released]";

export type ReleaseType = "major" | "minor" | "patch";

export interface PromoteArgs {
  version: string;
  releaseType: ReleaseType;
  today: Date;
  content: string;
}

export function promoteChangelog(args: PromoteArgs): string {
  // Project convention: patches roll into the parent minor section, so no
  // separate changelog entry is added for patch releases.
  if (args.releaseType === "patch") return args.content;

  const newHeader = `## ${args.version}, ${formatIsoDate(args.today)}`;

  if (args.content.includes(TO_BE_RELEASED_HEADER)) {
    return args.content.replace(TO_BE_RELEASED_HEADER, () => newHeader);
  }

  const firstSection = /^## /m.exec(args.content);
  if (!firstSection) {
    const trailingNewline = args.content.endsWith("\n") ? "" : "\n";
    return args.content + trailingNewline + newHeader + "\n";
  }
  return (
    args.content.slice(0, firstSection.index) +
    newHeader +
    "\n\n" +
    args.content.slice(firstSection.index)
  );
}

function formatIsoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

const isMainModule = import.meta.url === `file://${process.argv[1]}`;
if (isMainModule) {
  const [version, releaseType, rootDir = process.cwd()] = process.argv.slice(2);
  if (!version || !releaseType) {
    console.error(
      "Usage: promoteChangelog.ts <new-version> <release-type> [root-dir]",
    );
    process.exit(1);
  }
  if (releaseType !== "major" && releaseType !== "minor" && releaseType !== "patch") {
    console.error(`Unknown release type "${releaseType}". Expected major | minor | patch.`);
    process.exit(1);
  }
  const changelogPath = join(rootDir, "CHANGES.md");
  const content = readFileSync(changelogPath, "utf-8");
  const updated = promoteChangelog({
    version,
    releaseType,
    today: new Date(),
    content,
  });
  if (updated === content) {
    console.log(`Changelog unchanged (no ${TO_BE_RELEASED_HEADER} or previous patch section found)`);
  } else {
    writeFileSync(changelogPath, updated);
    console.log(`Promoted CHANGES.md → ## ${version}`);
  }
}
