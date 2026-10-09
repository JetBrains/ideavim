#!/usr/bin/env tsx
/**
 * Marks the Ready To Release YouTrack tickets that the release contains as fixed in it.
 *
 * The release is built from the release branch, so a ticket whose change is only on master stays in
 * Ready To Release for the next release. releaseContent.ts decides that, the same way it decides which
 * changelog entries the release has.
 *
 * Usage:
 *   npx tsx scripts-ts/src/releaseActions.ts <version> <release-type> <root-dir> [master-ref] [release-ref]
 *
 * Example (after the release build pushed the release branch):
 *   npx tsx scripts-ts/src/releaseActions.ts 2.48.0 minor .. origin/master release
 */

import {readBranchCommits, ReleaseContent, ticketKey} from "./releaseContent.js";
import {
    createReleaseVersion,
    getTicketsByQuery,
    getVersionIdByName,
    setFixVersion,
    setStatus
} from "./tools/youtrack.js";

export const READY_TO_RELEASE_QUERY = "#{Ready To Release}";

/**
 * The tickets that are fixed in the release. A ticket no commit mentions was moved to Ready To Release
 * by hand, and nothing says it is missing from the release, so it is released.
 */
export function selectReleasedTickets(readyToRelease: string[], content: ReleaseContent): string[] {
    return readyToRelease.filter((ticket) => content.verdict(ticketKey(ticket)) !== "not-released");
}

export async function runReleaseActions(version: string, content: ReleaseContent): Promise<void> {
    const readyToRelease = await getTicketsByQuery(READY_TO_RELEASE_QUERY);
    console.log(`Tickets in Ready To Release: ${JSON.stringify(readyToRelease)}`);

    const tickets = selectReleasedTickets(readyToRelease, content);
    const skipped = readyToRelease.filter((ticket) => !tickets.includes(ticket));
    if (skipped.length > 0) {
        console.log(`Only on master, left in Ready To Release: ${JSON.stringify(skipped)}`);
    }
    if (tickets.length === 0) {
        console.log("No tickets to update");
        return;
    }

    for (const ticket of tickets) await setStatus(ticket, "Fixed");

    if (await getVersionIdByName(version) === null) await createReleaseVersion(version);
    for (const ticket of tickets) await setFixVersion(ticket, version);
}

const isMainModule = import.meta.url === `file://${process.argv[1]}`;
if (isMainModule) {
    const [version, releaseType, rootDir, masterRef = "origin/master", releaseRef = "release"] = process.argv.slice(2);
    if (!version || !releaseType || !rootDir) {
        console.error("Usage: releaseActions.ts <version> <release-type> <root-dir> [master-ref] [release-ref]");
        process.exit(1);
    }
    if (releaseType === "patch") {
        console.log("Skipping release actions for patch release");
    } else {
        const content = new ReleaseContent(readBranchCommits(rootDir, masterRef, releaseRef));
        runReleaseActions(version, content).catch((error) => {
            console.error(`Error: ${error instanceof Error ? error.message : error}`);
            process.exit(1);
        });
    }
}
