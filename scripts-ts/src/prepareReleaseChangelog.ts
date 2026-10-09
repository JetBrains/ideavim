#!/usr/bin/env npx tsx
/**
 * Writes the CHANGES.md the release branch is released with.
 *
 * The changelog is maintained on master, and master may be ahead of the release branch (the EAP
 * build is the only thing that moves the release branch to master). So the release gets master's
 * CHANGES.md with the `## To Be Released` entries that are only on master taken out, as releaseContent.ts
 * tells them apart:
 *  - an entry that links a VIM ticket or a PR the release branch does not contain is dropped, and one
 *    it contains is kept,
 *  - an entry the commits say nothing about (no ticket or PR, or a PR merged without its number in
 *    the commit subject) is kept only if the release branch's own CHANGES.md already has it.
 * The dropped entries stay under `## To Be Released` on master (see syncChangelogToMaster.ts) and
 * go out with the next release.
 *
 * Usage:
 *   npx tsx scripts-ts/src/prepareReleaseChangelog.ts <root-dir> <master-ref> [release-ref]
 *     On the release branch: rewrites its CHANGES.md from <master-ref>'s.
 *   npx tsx scripts-ts/src/prepareReleaseChangelog.ts --print <root-dir> <release-ref>
 *     On master: prints the `## To Be Released` section the release would get from the working tree's
 *     CHANGES.md. The Prepare What's New workflow describes exactly this on the page.
 *
 * Examples:
 *   npx tsx scripts-ts/src/prepareReleaseChangelog.ts .. origin/master
 *   npx tsx scripts-ts/src/prepareReleaseChangelog.ts --print . origin/release
 */

import {execFileSync} from "node:child_process";
import {readFileSync, writeFileSync} from "node:fs";
import {join} from "node:path";
import {TO_BE_RELEASED_HEADER} from "./promoteChangelog.js";
import {type BranchCommits, readBranchCommits, ReleaseContent} from "./releaseContent.js";
import {entryKey, entryKeys, findSection, isEntry, unreleasedEntries} from "./syncChangelogToMaster.js";

export interface PrepareArgs {
    masterContent: string;
    releaseContent: string;
    commits: BranchCommits;
}

export function prepareReleaseChangelog(args: PrepareArgs): string {
    const masterLines = args.masterContent.split("\n");
    const toBeReleased = findSection(masterLines, (line) => line === TO_BE_RELEASED_HEADER);
    if (!toBeReleased) return args.masterContent;

    const content = new ReleaseContent(args.commits);
    // Nothing landed on master after the cut, so everything in the changelog is released
    if (content.isUpToDate()) return args.masterContent;

    const releaseSection = findSection(args.releaseContent.split("\n"), (line) => line === TO_BE_RELEASED_HEADER);
    const knownToRelease = releaseSection ? entryKeys(releaseSection.lines) : new Set<string>();

    const dropped = new Set<string>();
    for (const line of toBeReleased.lines.filter(isEntry)) {
        const key = entryKey(line);
        const verdict = content.verdict(key);
        if (verdict === "not-released" || (verdict === "unknown" && !knownToRelease.has(key))) dropped.add(key);
    }

    const remaining = unreleasedEntries(toBeReleased.lines, dropped);
    const section = remaining.some(isEntry) ? [TO_BE_RELEASED_HEADER, "", ...remaining] : [];
    return [
        ...masterLines.slice(0, toBeReleased.start),
        ...section,
        ...masterLines.slice(toBeReleased.end),
    ].join("\n");
}

/** The `## To Be Released` section of [content], or an empty string if it has none. */
export function toBeReleasedSection(content: string): string {
    const section = findSection(content.split("\n"), (line) => line === TO_BE_RELEASED_HEADER);
    return section ? section.lines.join("\n").trimEnd() + "\n" : "";
}

function showFile(rootDir: string, ref: string, path: string): string {
    return execFileSync("git", ["show", `${ref}:${path}`], {cwd: rootDir, encoding: "utf-8"});
}

const isMainModule = import.meta.url === `file://${process.argv[1]}`;
if (isMainModule) {
    const args = process.argv.slice(2);
    if (args[0] === "--print") {
        const [, rootDir, releaseRef] = args;
        if (!rootDir || !releaseRef) {
            console.error("Usage: prepareReleaseChangelog.ts --print <root-dir> <release-ref>");
            process.exit(1);
        }
        const prepared = prepareReleaseChangelog({
            masterContent: readFileSync(join(rootDir, "CHANGES.md"), "utf-8"),
            releaseContent: showFile(rootDir, releaseRef, "CHANGES.md"),
            commits: readBranchCommits(rootDir, "HEAD", releaseRef),
        });
        const section = toBeReleasedSection(prepared);
        process.stdout.write(section || `Nothing under ${TO_BE_RELEASED_HEADER} is on ${releaseRef}\n`);
    } else {
        const [rootDir, masterRef, releaseRef = "HEAD"] = args;
        if (!rootDir || !masterRef) {
            console.error("Usage: prepareReleaseChangelog.ts <root-dir> <master-ref> [release-ref]");
            process.exit(1);
        }
        const changelogPath = join(rootDir, "CHANGES.md");
        const commits = readBranchCommits(rootDir, masterRef, releaseRef);
        console.log(`${commits.masterOnly.length} commits only on ${masterRef}, ${commits.releaseOnly.length} only on ${releaseRef}`);

        const updated = prepareReleaseChangelog({
            masterContent: showFile(rootDir, masterRef, "CHANGES.md"),
            releaseContent: readFileSync(changelogPath, "utf-8"),
            commits,
        });
        writeFileSync(changelogPath, updated);
        console.log(`Wrote ${changelogPath} from ${masterRef} without the entries that are only on ${masterRef}`);
    }
}
