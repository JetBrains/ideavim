#!/usr/bin/env npx tsx
/**
 * Brings the released section of CHANGES.md from the release branch over to master.
 *
 * The release branch only follows master when the EAP build resets it, so by the time a release
 * is published master's `## To Be Released` section can hold entries the release does not contain.
 * Promoting master's section wholesale would label those as released. Instead, the section that
 * promoteChangelog.ts produced on the release branch is copied as is, and only the entries that
 * are not in it stay under `## To Be Released` on master.
 *
 * Usage:
 *   npx tsx scripts-ts/src/syncChangelogToMaster.ts <version> <master CHANGES.md> <release CHANGES.md>
 *
 * Example:
 *   git show release:CHANGES.md > /tmp/release-CHANGES.md
 *   npx tsx scripts-ts/src/syncChangelogToMaster.ts 2.48.0 CHANGES.md /tmp/release-CHANGES.md
 */

import {readFileSync, writeFileSync} from "node:fs";
import {TO_BE_RELEASED_HEADER} from "./promoteChangelog.js";

export interface SyncArgs {
    version: string;
    masterContent: string;
    releaseContent: string;
}

export interface Section {
    /** Index of the header line. */
    start: number;
    /** Index of the next `## ` header, or the number of lines. */
    end: number;
    lines: string[];
}

export const isTopHeader = (line: string): boolean => line.startsWith("## ");
export const isSubHeader = (line: string): boolean => line.startsWith("### ");
export const isEntry = (line: string): boolean => /^[*-] /.test(line);
export const isVersionHeader = (version: string) => (line: string): boolean =>
    line === `## ${version}` || line.startsWith(`## ${version},`);

export function syncChangelogToMaster(args: SyncArgs): string {
    const releaseLines = args.releaseContent.split("\n");
    const released = findSection(releaseLines, isVersionHeader(args.version));
    // Nothing was promoted on the release branch (a patch release), so there is nothing to bring over.
    if (!released) return args.masterContent;

    const masterLines = args.masterContent.split("\n");
    if (findSection(masterLines, isVersionHeader(args.version))) return args.masterContent;

    const releasedSection = withSingleTrailingBlank(released.lines);
    const toBeReleased = findSection(masterLines, (line) => line === TO_BE_RELEASED_HEADER);
    if (!toBeReleased) {
        const firstHeader = masterLines.findIndex(isTopHeader);
        const insertAt = firstHeader === -1 ? masterLines.length : firstHeader;
        return [
            ...masterLines.slice(0, insertAt),
            ...releasedSection,
            ...masterLines.slice(insertAt),
        ].join("\n");
    }

    const remaining = unreleasedEntries(toBeReleased.lines, entryKeys(released.lines));
    const unreleasedSection = remaining.some(isEntry) ? [TO_BE_RELEASED_HEADER, "", ...remaining] : [];
    return [
        ...masterLines.slice(0, toBeReleased.start),
        ...unreleasedSection,
        ...releasedSection,
        ...masterLines.slice(toBeReleased.end),
    ].join("\n");
}

export function findSection(lines: string[], isHeader: (line: string) => boolean): Section | null {
    const start = lines.findIndex(isHeader);
    if (start === -1) return null;
    let end = start + 1;
    while (end < lines.length && !isTopHeader(lines[end])) end++;
    return {start, end, lines: lines.slice(start, end)};
}

export function entryKeys(sectionLines: string[]): Set<string> {
    return new Set(sectionLines.filter(isEntry).map(entryKey));
}

/**
 * What identifies an entry across the two branches. The changelog is maintained on both of them, so
 * the same change can be worded differently; the ticket or the PR it links is stable, the text is not.
 */
export function entryKey(line: string): string {
    const ticket = /\[(VIM-\d+)\]\(/.exec(line);
    if (ticket) return `ticket:${ticket[1]}`;
    const pr = /\[(\d+)\]\(https:\/\/github\.com\/[^)]*\/pull\/\1\)/.exec(line);
    if (pr) return `pr:${pr[1]}`;
    return `text:${line.trim()}`;
}

/**
 * The body of the `## To Be Released` section without the entries that are in [released],
 * and without the subsections that are left empty by that.
 */
export function unreleasedEntries(sectionLines: string[], released: Set<string>): string[] {
    const body = sectionLines.slice(1);
    const kept: string[] = [];
    for (let i = 0; i < body.length;) {
        if (!isEntry(body[i])) {
            kept.push(body[i]);
            i++;
            continue;
        }
        // An entry is its bullet line plus the indented continuation lines after it
        let next = i + 1;
        while (next < body.length && body[next].trim() !== "" && !isEntry(body[next]) && !body[next].startsWith("#")) {
            next++;
        }
        if (!released.has(entryKey(body[i]))) kept.push(...body.slice(i, next));
        i = next;
    }

    const withoutEmptySubsections: string[] = [];
    for (let i = 0; i < kept.length;) {
        if (!isSubHeader(kept[i])) {
            withoutEmptySubsections.push(kept[i]);
            i++;
            continue;
        }
        let next = i + 1;
        while (next < kept.length && !isSubHeader(kept[next])) next++;
        const subsection = kept.slice(i, next);
        if (subsection.some(isEntry)) withoutEmptySubsections.push(...subsection);
        i = next;
    }

    return withSingleTrailingBlank(collapseBlankLines(withoutEmptySubsections));
}

function collapseBlankLines(lines: string[]): string[] {
    const result: string[] = [];
    for (const line of lines) {
        const blank = line.trim() === "";
        if (blank && (result.length === 0 || result[result.length - 1].trim() === "")) continue;
        result.push(line);
    }
    return result;
}

function withSingleTrailingBlank(lines: string[]): string[] {
    const result = [...lines];
    while (result.length > 0 && result[result.length - 1].trim() === "") result.pop();
    result.push("");
    return result;
}

const isMainModule = import.meta.url === `file://${process.argv[1]}`;
if (isMainModule) {
    const [version, masterPath, releasePath] = process.argv.slice(2);
    if (!version || !masterPath || !releasePath) {
        console.error("Usage: syncChangelogToMaster.ts <version> <master CHANGES.md> <release CHANGES.md>");
        process.exit(1);
    }
    const masterContent = readFileSync(masterPath, "utf-8");
    const updated = syncChangelogToMaster({
        version,
        masterContent,
        releaseContent: readFileSync(releasePath, "utf-8"),
    });
    if (updated === masterContent) {
        console.log(`Changelog unchanged (no ## ${version} section on the release branch, or master already has it)`);
    } else {
        writeFileSync(masterPath, updated);
        console.log(`Brought ## ${version} over to ${masterPath}`);
    }
}
