import {describe, expect, it} from "vitest";
import {selectReleasedTickets} from "./releaseActions.js";
import {ReleaseContent} from "./releaseContent.js";

describe("releaseActions", () => {
    const content = new ReleaseContent({
        masterOnly: ["VIM-2 landed after the cut", "VIM-3 cherry-picked", "Some refactoring"],
        releaseOnly: ["VIM-3 cherry-picked", "Preparation to 2.48.0 release"],
        release: ["VIM-3 cherry-picked", "VIM-1 released"],
    });

    it("marks only the Ready To Release tickets that the release branch contains", () => {
        expect(selectReleasedTickets(["VIM-1", "VIM-2", "VIM-3"], content)).toEqual(["VIM-1", "VIM-3"]);
    });

    it("releases a ticket moved to Ready To Release by hand", () => {
        expect(selectReleasedTickets(["VIM-4"], content)).toEqual(["VIM-4"]);
    });
});
