import {describe, expect, it} from "vitest";
import {prepareReleaseChangelog, toBeReleasedSection} from "./prepareReleaseChangelog.js";

const preamble = `The Changelog
=============
`;

const older = `## 2.47.0, 2026-09-24

### Features:
* Older feature
`;

const masterContent = `${preamble}
## To Be Released

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature
* [VIM-3](https://youtrack.jetbrains.com/issue/VIM-3) Feature landed after the release branch was cut
* \`:help\` entry without a ticket that the release branch already documents
* Entry without a ticket that only master documents

### Fixes:
* [VIM-2](https://youtrack.jetbrains.com/issue/VIM-2) Fix cherry-picked to the release branch

### Merged PRs:
* [2099](https://github.com/JetBrains/ideavim/pull/2099) by someone: Released PR
* [2100](https://github.com/JetBrains/ideavim/pull/2100) by someone: PR merged after the cut
* [2101](https://github.com/JetBrains/ideavim/pull/2101) by someone: PR merged after the cut without its number in the commit
* [2102](https://github.com/JetBrains/ideavim/pull/2102) by someone: Released PR without its number in the commit

### Changes:
* [VIM-4](https://youtrack.jetbrains.com/issue/VIM-4) Change released before the cut, documented on master after it

${older}`;

const releaseContent = `${preamble}
## To Be Released

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature
* \`:help\` entry without a ticket that the release branch already documents

### Merged PRs:
* [2102](https://github.com/JetBrains/ideavim/pull/2102) by someone: Released PR without its number in the commit

${older}`;

const masterOnlyCommits = [
    "Fix(VIM-3): landed after the cut",
    "Fix(VIM-2): cherry-picked",
    "Something for the next release (#2100)",
    "Update changelog",
];
const releaseOnlyCommits = ["Fix(VIM-2): cherry-picked", "Preparation to 2.47.2 release"];
const releaseCommits = [
    ...releaseOnlyCommits,
    "Fix(VIM-1): released",
    "Released PR (#2099)",
    "VIM-4 released before the cut",
    "Old commit",
];

describe("prepareReleaseChangelog", () => {
    it("keeps master's entries for the release branch and drops the ones that are only on master", () => {
        const result = prepareReleaseChangelog({
            masterContent,
            releaseContent,
            commits: {masterOnly: masterOnlyCommits, releaseOnly: releaseOnlyCommits, release: releaseCommits},
        });
        expect(result).toBe(`${preamble}
## To Be Released

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature
* \`:help\` entry without a ticket that the release branch already documents

### Fixes:
* [VIM-2](https://youtrack.jetbrains.com/issue/VIM-2) Fix cherry-picked to the release branch

### Merged PRs:
* [2099](https://github.com/JetBrains/ideavim/pull/2099) by someone: Released PR
* [2102](https://github.com/JetBrains/ideavim/pull/2102) by someone: Released PR without its number in the commit

### Changes:
* [VIM-4](https://youtrack.jetbrains.com/issue/VIM-4) Change released before the cut, documented on master after it

${older}`);
    });

    it("uses master's changelog as is when the branches do not differ", () => {
        const result = prepareReleaseChangelog({
            masterContent,
            releaseContent: `${preamble}\n${older}`,
            commits: {masterOnly: [], releaseOnly: [], release: ["Old commit"]},
        });
        expect(result).toBe(masterContent);
    });

    it("leaves a changelog without To Be Released alone", () => {
        const content = `${preamble}\n${older}`;
        expect(prepareReleaseChangelog({
            masterContent: content,
            releaseContent: content,
            commits: {masterOnly: ["Fix(VIM-9): whatever"], releaseOnly: [], release: []},
        })).toBe(content);
    });

    it("drops an entry whose ticket is mentioned without the Fix() prefix", () => {
        const result = prepareReleaseChangelog({
            masterContent,
            releaseContent,
            commits: {
                masterOnly: ["VIM-1 follow-up after the cut", ...masterOnlyCommits],
                releaseOnly: releaseOnlyCommits,
                release: releaseCommits,
            },
        });
        expect(result).not.toContain("Released feature");
    });

    it("gives the To Be Released section alone", () => {
        expect(toBeReleasedSection(`${preamble}
## To Be Released

### Fixes:
* A fix


${older}`)).toBe(`## To Be Released

### Fixes:
* A fix
`);
        expect(toBeReleasedSection(`${preamble}\n${older}`)).toBe("");
    });
});
