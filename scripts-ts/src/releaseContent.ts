/**
 * Tells which tickets and PRs the release branch contains.
 *
 * The release is built from the `release` branch, which only follows master when the EAP build resets
 * it, so master can hold changes the release does not have. The changelog of the release
 * (prepareReleaseChangelog.ts), its What's New page (written from that changelog by the Prepare What's
 * New workflow) and the YouTrack tickets marked as fixed in it (releaseActions.ts) all decide what is
 * released here, so they never disagree.
 *
 * A ticket or a PR is identified by the commits that mention it: any `VIM-123` in a commit subject, and
 * `(#123)` or `Merge pull request #123` for a PR.
 */

import {execFileSync} from "node:child_process";

export interface BranchCommits {
    /** Subjects of the commits on master but not on the release branch, `git log release..master`. */
    masterOnly: string[];
    /** Subjects of the commits on the release branch but not on master, `git log master..release`. */
    releaseOnly: string[];
    /** Subjects of all the commits on the release branch, `git log release`. */
    release: string[];
}

/**
 * - `released`: a commit on the release branch mentions it,
 * - `not-released`: it is mentioned only by commits that landed on master after the release branch was
 *   cut, and none of them was cherry-picked to the release branch,
 * - `unknown`: no commit mentions it, e.g. a changelog entry without a ticket or a ticket closed by hand.
 */
export type Verdict = "released" | "not-released" | "unknown";

export class ReleaseContent {
    private readonly onlyOnMaster: Set<string>;
    private readonly onRelease: Set<string>;
    private readonly masterIsAhead: boolean;

    constructor(commits: BranchCommits) {
        this.masterIsAhead = commits.masterOnly.length > 0;
        const cherryPicked = commitKeys(commits.releaseOnly);
        this.onlyOnMaster = new Set([...commitKeys(commits.masterOnly)].filter((key) => !cherryPicked.has(key)));
        this.onRelease = commitKeys(commits.release);
    }

    /**
     * Whether nothing landed on master after the release branch was cut, so everything master has is
     * released. Commits that mention no ticket count too: they can be changes without a ticket.
     */
    isUpToDate(): boolean {
        return !this.masterIsAhead;
    }

    /** [key] is `ticket:VIM-123` or `pr:123`, see [commitKeys]. */
    verdict(key: string): Verdict {
        // Checked first: a ticket fixed again on master was mentioned on the release branch before
        if (this.onlyOnMaster.has(key)) return "not-released";
        if (this.onRelease.has(key)) return "released";
        return "unknown";
    }
}

/** The tickets and PRs a list of commit subjects mentions, as `ticket:VIM-123` and `pr:123` keys. */
export function commitKeys(subjects: string[]): Set<string> {
    const keys = new Set<string>();
    for (const subject of subjects) {
        for (const match of subject.matchAll(/\b(VIM-\d+)\b/gi)) keys.add(ticketKey(match[1]));
        for (const match of subject.matchAll(/\(#(\d+)\)|^Merge pull request #(\d+)\b/g)) {
            keys.add(`pr:${match[1] ?? match[2]}`);
        }
    }
    return keys;
}

export function ticketKey(ticket: string): string {
    return `ticket:${ticket.toUpperCase()}`;
}

export function readBranchCommits(rootDir: string, masterRef: string, releaseRef: string): BranchCommits {
    const subjects = (...range: string[]) =>
        execFileSync("git", ["log", "--format=%s", ...range], {cwd: rootDir, encoding: "utf-8"})
            .split("\n")
            .filter(Boolean);
    return {
        masterOnly: subjects(`${releaseRef}..${masterRef}`),
        releaseOnly: subjects(`${masterRef}..${releaseRef}`),
        release: subjects(releaseRef),
    };
}
