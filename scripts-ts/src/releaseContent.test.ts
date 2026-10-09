import {describe, expect, it} from "vitest";
import {commitKeys, ReleaseContent} from "./releaseContent.js";

describe("commitKeys", () => {
    it("reads tickets and PR numbers out of commit subjects", () => {
        expect(commitKeys([
            "Fix(VIM-1): a",
            "VIM-2524 calculate indent after cc",
            "Squashed PR (#2099)",
            "Merge pull request #2100 from someone/branch",
            "vim-7 lower case",
            "VIM-8 and VIM-9 at once",
        ])).toEqual(new Set([
            "ticket:VIM-1", "ticket:VIM-2524", "pr:2099", "pr:2100", "ticket:VIM-7", "ticket:VIM-8", "ticket:VIM-9",
        ]));
    });

    it("does not take a PR number from the middle of a subject", () => {
        expect(commitKeys(["Revert the change from #2100"])).toEqual(new Set());
    });
});

describe("ReleaseContent", () => {
    const content = new ReleaseContent({
        masterOnly: ["VIM-3 landed after the cut", "VIM-2 cherry-picked", "VIM-5 fixed again after the cut"],
        releaseOnly: ["VIM-2 cherry-picked"],
        release: ["VIM-2 cherry-picked", "VIM-1 released", "VIM-5 released before the cut"],
    });

    it("tells released, not released and unknown tickets apart", () => {
        expect(content.verdict("ticket:VIM-1")).toBe("released");
        expect(content.verdict("ticket:VIM-2")).toBe("released");
        expect(content.verdict("ticket:VIM-3")).toBe("not-released");
        expect(content.verdict("ticket:VIM-4")).toBe("unknown");
    });

    it("holds a ticket back while some of its commits are only on master", () => {
        expect(content.verdict("ticket:VIM-5")).toBe("not-released");
    });

    it("is up to date only when master has no commits of its own", () => {
        expect(content.isUpToDate()).toBe(false);
        expect(new ReleaseContent({masterOnly: ["Some refactoring"], releaseOnly: [], release: []}).isUpToDate()).toBe(false);
        expect(new ReleaseContent({masterOnly: [], releaseOnly: ["Preparation to 2.48.0 release"], release: []})
            .isUpToDate()).toBe(true);
    });
});
