import {describe, expect, it} from "vitest";
import {syncChangelogToMaster} from "./syncChangelogToMaster.js";

const preamble = `The Changelog
=============

History of changes in IdeaVim for the IntelliJ platform.
`;

const older = `## 2.47.0, 2026-09-24

### Features:
* Older feature
`;

const releaseContent = `${preamble}
## 2.48.0, 2026-10-09

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature

### Fixes:
* [VIM-2](https://youtrack.jetbrains.com/issue/VIM-2) Released fix

${older}`;

const masterContent = `${preamble}
## To Be Released

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature
* [VIM-3](https://youtrack.jetbrains.com/issue/VIM-3) Feature landed after the release branch was cut

### Fixes:
* [VIM-2](https://youtrack.jetbrains.com/issue/VIM-2) Released fix

### Merged PRs:
* [2099](https://github.com/JetBrains/ideavim/pull/2099) by someone: PR landed after the cut

${older}`;

describe("syncChangelogToMaster", () => {
    it("copies the released section and keeps only the unreleased entries under To Be Released", () => {
        const result = syncChangelogToMaster({version: "2.48.0", masterContent, releaseContent});
        expect(result).toBe(`${preamble}
## To Be Released

### Features:
* [VIM-3](https://youtrack.jetbrains.com/issue/VIM-3) Feature landed after the release branch was cut

### Merged PRs:
* [2099](https://github.com/JetBrains/ideavim/pull/2099) by someone: PR landed after the cut

## 2.48.0, 2026-10-09

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature

### Fixes:
* [VIM-2](https://youtrack.jetbrains.com/issue/VIM-2) Released fix

${older}`);
    });

    it("drops To Be Released when everything in it was released", () => {
        const master = `${preamble}
## To Be Released

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature

### Fixes:
* [VIM-2](https://youtrack.jetbrains.com/issue/VIM-2) Released fix

${older}`;
        const result = syncChangelogToMaster({version: "2.48.0", masterContent: master, releaseContent});
        expect(result).toBe(releaseContent);
    });

    it("keeps the continuation lines of an unreleased entry", () => {
        const master = `${preamble}
## To Be Released

### Features:
* [VIM-3](https://youtrack.jetbrains.com/issue/VIM-3) Multi-line entry
  that continues on the next line
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature

${older}`;
        const result = syncChangelogToMaster({version: "2.48.0", masterContent: master, releaseContent});
        expect(result).toContain(`### Features:
* [VIM-3](https://youtrack.jetbrains.com/issue/VIM-3) Multi-line entry
  that continues on the next line

## 2.48.0, 2026-10-09`);
        expect(result).not.toContain("## To Be Released\n\n### Features:\n* [VIM-1]");
    });

    it("inserts the released section above the first version when master has no To Be Released", () => {
        const master = `${preamble}
${older}`;
        const result = syncChangelogToMaster({version: "2.48.0", masterContent: master, releaseContent});
        expect(result).toBe(releaseContent);
    });

    it("leaves master alone for a patch release, which promotes nothing", () => {
        const result = syncChangelogToMaster({version: "2.48.1", masterContent, releaseContent});
        expect(result).toBe(masterContent);
    });

    it("treats a differently worded entry for the same ticket or PR as released", () => {
        const master = `${preamble}
## To Be Released

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Same feature, worded by the master changelog run

### Merged PRs:
* [2099](https://github.com/JetBrains/ideavim/pull/2099) by someone: Same PR, other wording
* [2100](https://github.com/JetBrains/ideavim/pull/2100) by someone: Not released

${older}`;
        const release = `${preamble}
## 2.48.0, 2026-10-09

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature

### Merged PRs:
* [2099](https://github.com/JetBrains/ideavim/pull/2099) by someone: PR

${older}`;
        const result = syncChangelogToMaster({version: "2.48.0", masterContent: master, releaseContent: release});
        expect(result).toBe(`${preamble}
## To Be Released

### Merged PRs:
* [2100](https://github.com/JetBrains/ideavim/pull/2100) by someone: Not released

## 2.48.0, 2026-10-09

### Features:
* [VIM-1](https://youtrack.jetbrains.com/issue/VIM-1) Released feature

### Merged PRs:
* [2099](https://github.com/JetBrains/ideavim/pull/2099) by someone: PR

${older}`);
    });

    it("is idempotent", () => {
        const once = syncChangelogToMaster({version: "2.48.0", masterContent, releaseContent});
        const twice = syncChangelogToMaster({version: "2.48.0", masterContent: once, releaseContent});
        expect(twice).toBe(once);
    });
});
