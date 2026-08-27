import { describe, it, expect } from "vitest";
import {
  affectedTests,
  branchNameFor,
  dedupeFailures,
  fingerprintFailure,
  normalizeMessage,
  parseFailureDetails,
  renderReport,
  selectFailedBuildsInWindow,
  summarizeFailure,
  toFailure,
} from "./propertyTestFailures.js";
import { parseTeamCityDate, type TeamCityBuild, type TeamCityTestOccurrence } from "./tools/teamcity.js";

/** Verbatim head of a real TeamCity failure (build #1728 of Ideavim_PropertyBased), trace trimmed. */
const realDetails = `org.jetbrains.jetCheck.PropertyFalsified: Failed with java.lang.IndexOutOfBoundsException: Index out of range: 76; length: 76
On commands:
Put caret at position 74
  Use command:  . Action: VimMotionSpaceAction
  Use command:  . Action: VimMotionSpaceAction
  Use command: (. Action: VimMotionSentencePreviousStartAction
  Use command: @. Action: VimPlaybackRegisterAction
  Use command:  . Action: VimMotionSpaceAction
  Use command: C. Action: VimChangeEndOfLineAction
  Use command: <C-2>. Action: VimInsertPreviousInsertExitAction
  Use command: :. Action: VimExEntryAction
  Use command: <C-R>. Action: VimInsertRegisterAction
  Use command: <C-R><C-F>. Action: VimInsertFilenameUnderCaretAction
Shrunk in 13 stages, by trying 125 examples

To re-run the minimal failing case, run
  PropertyChecker.customized().rechecking("8Ov7wQrxi7GbEl1KCgAAAAAABwBTAAAAVgABABAADQAB")
    .checkScenarios(...)
To re-run the test with all intermediate shrinking steps, use \`recheckingIteration(6061698797119349489L, 93)\` instead for last iteration, or \`withSeed(869298284856218035L)\` for all iterations

 Property failure reason, innermost exception (see full trace below): java.lang.IndexOutOfBoundsException: Index out of range: 76; length: 76
\tat com.intellij.util.text.ImmutableText.outOfRange(ImmutableText.java:273)
\tat com.intellij.util.text.ImmutableText.findLeaf(ImmutableText.java:252)
\tat com.intellij.util.text.ImmutableText.charAt(ImmutableText.java:237)
\tat com.intellij.openapi.editor.impl.DocumentImpl$1.charAt(DocumentImpl.java:96)
\tat com.maddyhome.idea.vim.api.VimSearchHelperBase.findFilenameAtOrFollowingCursor(VimSearchHelperBase.kt:180)
\tat com.maddyhome.idea.vim.action.ex.InsertFilenameUnderCaretAction.execute(InsertFilenameUnderCaretAction.kt:28)
\tat com.maddyhome.idea.vim.action.ex.CommandLineActionHandler.execute(CommandLineActionHandler.kt:42)
`;

/** The other real shape: the exception escapes before jetCheck can report a scenario (build #1726). */
const plainExceptionDetails = `java.lang.AssertionError: Assertion failed
\tat com.maddyhome.idea.vim.yank.YankGroupBase.yankMotion(YankGroupBase.kt:77)
\tat com.maddyhome.idea.vim.action.copy.YankMotionAction.execute(YankMotionAction.kt:36)
\tat com.maddyhome.idea.vim.handler.VimActionHandler.baseExecute(VimActionHandler.kt:87)
`;

const build = (overrides: Partial<TeamCityBuild> = {}): TeamCityBuild => ({
  id: 18998,
  number: "1728",
  status: "FAILURE",
  state: "finished",
  branchName: "master",
  finishDate: "20260826T091714+0000",
  webUrl: "https://ideavim.teamcity.com/buildConfiguration/Ideavim_PropertyBased/18998",
  ...overrides,
});

const test = (overrides: Partial<TeamCityTestOccurrence> = {}): TeamCityTestOccurrence => ({
  id: "build:(id:18998),id:2000000003",
  name: "org.jetbrains.plugins.ideavim.propertybased.RandomActionsPropertyTest.testRandomActions",
  status: "FAILURE",
  newFailure: true,
  details: realDetails,
  ...overrides,
});

describe("parseFailureDetails", () => {
  const parsed = parseFailureDetails(realDetails);

  it("extracts the innermost exception, not the PropertyFalsified wrapper", () => {
    expect(parsed.exceptionClass).toBe("java.lang.IndexOutOfBoundsException");
    expect(parsed.exceptionMessage).toBe("Index out of range: 76; length: 76");
  });

  it("extracts the minimal scenario", () => {
    expect(parsed.caretPosition).toBe(74);
    expect(parsed.commands).toHaveLength(10);
    expect(parsed.commands[0]).toBe(" . Action: VimMotionSpaceAction");
    expect(parsed.commands[9]).toBe("<C-R><C-F>. Action: VimInsertFilenameUnderCaretAction");
    expect(parsed.shrinkInfo).toBe("Shrunk in 13 stages, by trying 125 examples");
  });

  it("extracts every reproduction seed", () => {
    expect(parsed.recheckingSeed).toBe("8Ov7wQrxi7GbEl1KCgAAAAAABwBTAAAAVgABABAADQAB");
    expect(parsed.recheckingIteration).toEqual({ seed: "6061698797119349489", iteration: 93 });
    expect(parsed.seed).toBe("869298284856218035");
  });

  it("keeps IdeaVim frames only, closest first", () => {
    expect(parsed.engineFrames[0]).toBe(
      "com.maddyhome.idea.vim.api.VimSearchHelperBase.findFilenameAtOrFollowingCursor(VimSearchHelperBase.kt:180)",
    );
    expect(parsed.engineFrames.every((frame) => frame.startsWith("com.maddyhome.idea.vim."))).toBe(true);
    expect(parsed.engineFrames).toHaveLength(3);
  });

  it("starts the stack excerpt at the innermost exception", () => {
    expect(parsed.stackExcerpt.split("\n")[0]).toContain("Property failure reason");
    expect(parsed.stackExcerpt.split("\n").length).toBeLessThanOrEqual(60);
  });

  it("falls back to the wrapper message when there is no innermost exception", () => {
    const parsedShort = parseFailureDetails(
      "org.jetbrains.jetCheck.PropertyFalsified: Failed with java.lang.AssertionError: expected foo",
    );
    expect(parsedShort.exceptionClass).toBe("java.lang.AssertionError");
    expect(parsedShort.exceptionMessage).toBe("expected foo");
  });

  it("takes only the first scenario block, which jetCheck repeats later in its output", () => {
    // jetCheck prints the shrunk scenario again deep inside the full trace; counting every
    // `Use command:` line in the output would double the key sequence.
    const withRepeat = `${realDetails}\n${"\tat com.intellij.Filler.fill(Filler.java:1)\n".repeat(50)}\n${realDetails}`;
    const parsedTwice = parseFailureDetails(withRepeat);

    expect(parsedTwice.commands).toEqual(parsed.commands);
    expect(parsedTwice.commands).toHaveLength(10);
    expect(parsedTwice.caretPosition).toBe(74);
  });

  it("finds the scenario even without the `On commands:` header", () => {
    const headerless = parseFailureDetails(realDetails.replace("On commands:\n", ""));
    expect(headerless.commands).toHaveLength(10);
  });

  it("parses a plain stack trace with no jetCheck scenario", () => {
    const plain = parseFailureDetails(plainExceptionDetails);
    expect(plain.exceptionClass).toBe("java.lang.AssertionError");
    expect(plain.exceptionMessage).toBe("Assertion failed");
    expect(plain.engineFrames[0]).toBe("com.maddyhome.idea.vim.yank.YankGroupBase.yankMotion(YankGroupBase.kt:77)");
    expect(plain.hasScenario).toBe(false);
    expect(plain.recheckingSeed).toBeNull();
  });

  it("flags a jetCheck scenario as replayable", () => {
    expect(parsed.hasScenario).toBe(true);
  });

  it("degrades gracefully on unrecognized output", () => {
    const empty = parseFailureDetails("");
    expect(empty.exceptionClass).toBeNull();
    expect(empty.commands).toEqual([]);
    expect(empty.recheckingSeed).toBeNull();
  });
});

describe("fingerprintFailure", () => {
  const parsed = parseFailureDetails(realDetails);

  it("ignores offsets and line numbers, so one bug keeps one identity", () => {
    const other = parseFailureDetails(
      realDetails
        .replace(/76/g, "412")
        .replace("VimSearchHelperBase.kt:180", "VimSearchHelperBase.kt:186")
        .replace("8Ov7wQrxi7GbEl1KCgAAAAAABwBTAAAAVgABABAADQAB", "differentSeed=="),
    );
    expect(fingerprintFailure(other)).toBe(fingerprintFailure(parsed));
  });

  it("separates different exceptions", () => {
    const other = parseFailureDetails(realDetails.replace(/IndexOutOfBoundsException/g, "NullPointerException"));
    expect(fingerprintFailure(other)).not.toBe(fingerprintFailure(parsed));
  });

  it("separates different crash sites", () => {
    const other = parseFailureDetails(
      realDetails.replace("VimSearchHelperBase.findFilenameAtOrFollowingCursor", "VimSearchHelperBase.findNextWord"),
    );
    expect(fingerprintFailure(other)).not.toBe(fingerprintFailure(parsed));
  });

  it("does not depend on which test surfaced the bug", () => {
    expect(fingerprintFailure(parsed, "SomeOtherPropertyTest.testSomething")).toBe(fingerprintFailure(parsed));
  });

  it("falls back to the test name when nothing could be parsed", () => {
    const unparseable = parseFailureDetails("something went wrong, no idea what");
    expect(fingerprintFailure(unparseable, "TestA.test")).not.toBe(fingerprintFailure(unparseable, "TestB.test"));
  });

  it("produces a usable branch name", () => {
    expect(branchNameFor(fingerprintFailure(parsed))).toMatch(/^fix\/property-[0-9a-f]{12}$/);
  });
});

describe("normalizeMessage", () => {
  it("collapses varying numbers", () => {
    expect(normalizeMessage("Index out of range: 76; length: 76")).toBe("Index out of range: N; length: N");
    expect(normalizeMessage(null)).toBe("");
  });
});

describe("dedupeFailures", () => {
  it("keeps one entry per bug, prefers the newest build and records every occurrence", () => {
    const older = toFailure(build({ id: 18990, number: "1727" }), test());
    const newer = toFailure(build(), test({ details: realDetails.replace(/76/g, "80") }));

    const deduped = dedupeFailures([older, newer]);

    expect(deduped).toHaveLength(1);
    expect(deduped[0].buildId).toBe(18998);
    expect(deduped[0].occurrences.map((occurrence) => occurrence.buildNumber)).toEqual(["1728", "1727"]);
  });

  it("collapses the same crash reached from different test methods", () => {
    const viaRandomActions = toFailure(build(), test());
    const viaLoremIpsum = toFailure(
      build({ id: 18960, number: "1724" }),
      test({ name: "org.jetbrains.plugins.ideavim.propertybased.RandomActionsPropertyTest.testRandomActionsOnLoremIpsum" }),
    );

    const deduped = dedupeFailures([viaRandomActions, viaLoremIpsum]);

    expect(deduped).toHaveLength(1);
    expect(affectedTests(deduped[0])).toEqual([
      "org.jetbrains.plugins.ideavim.propertybased.RandomActionsPropertyTest.testRandomActions",
      "org.jetbrains.plugins.ideavim.propertybased.RandomActionsPropertyTest.testRandomActionsOnLoremIpsum",
    ]);
  });

  it("prefers the occurrence that has replay seeds", () => {
    const withoutScenario = toFailure(build(), test({ details: plainExceptionDetails }));
    const withScenario = toFailure(
      build({ id: 18951, number: "1723" }),
      test({
        details: realDetails
          .replace(/java\.lang\.IndexOutOfBoundsException/g, "java.lang.AssertionError")
          .replace(/Index out of range: 76; length: 76/g, "Assertion failed")
          .replace(
            "com.maddyhome.idea.vim.api.VimSearchHelperBase.findFilenameAtOrFollowingCursor(VimSearchHelperBase.kt:180)",
            "com.maddyhome.idea.vim.yank.YankGroupBase.yankMotion(YankGroupBase.kt:77)",
          ),
      }),
    );

    const deduped = dedupeFailures([withoutScenario, withScenario]);

    expect(deduped).toHaveLength(1);
    expect(deduped[0].buildNumber).toBe("1728"); // newest build still identifies the failure
    expect(deduped[0].parsed.hasScenario).toBe(true); // but the replayable scenario wins
    expect(deduped[0].parsed.recheckingSeed).toBe("8Ov7wQrxi7GbEl1KCgAAAAAABwBTAAAAVgABABAADQAB");
  });

  it("keeps genuinely different bugs apart", () => {
    const crash = toFailure(build(), test());
    const other = toFailure(build(), test({ details: plainExceptionDetails }));

    expect(dedupeFailures([crash, other])).toHaveLength(2);
  });
});

describe("selectFailedBuildsInWindow", () => {
  const now = new Date("2026-08-26T12:00:00Z");

  it("takes failed builds inside the window, newest first", () => {
    const builds = [
      build({ id: 18990, finishDate: "20260826T054556+0000" }),
      build({ id: 18998, finishDate: "20260826T091714+0000" }),
      build({ id: 18979, finishDate: "20260825T095149+0000" }), // older than 24h
      build({ id: 18999, status: "SUCCESS", finishDate: "20260826T100000+0000" }),
      build({ id: 19000, state: "running", finishDate: undefined }),
    ];

    expect(selectFailedBuildsInWindow(builds, { now, hours: 24 }).map((b) => b.id)).toEqual([18998, 18990]);
  });

  it("widens with a longer window", () => {
    const builds = [build({ id: 18979, finishDate: "20260825T095149+0000" })];
    expect(selectFailedBuildsInWindow(builds, { now, hours: 24 })).toHaveLength(0);
    expect(selectFailedBuildsInWindow(builds, { now, hours: 72 })).toHaveLength(1);
  });
});

describe("parseTeamCityDate", () => {
  it("parses TeamCity's compact timestamps", () => {
    expect(parseTeamCityDate("20260826T091714+0000").toISOString()).toBe("2026-08-26T09:17:14.000Z");
    expect(parseTeamCityDate("20260826T111714+0200").toISOString()).toBe("2026-08-26T09:17:14.000Z");
  });

  it("rejects garbage", () => {
    expect(() => parseTeamCityDate("yesterday")).toThrow();
  });
});

describe("renderReport", () => {
  const failure = toFailure(build(), test());
  const report = renderReport(failure);

  it("carries everything needed to reproduce", () => {
    expect(report).toContain(failure.fingerprint);
    expect(report).toContain("RandomActionsPropertyTest.testRandomActions");
    expect(report).toContain('rechecking("8Ov7wQrxi7GbEl1KCgAAAAAABwBTAAAAVgABABAADQAB")');
    expect(report).toContain("recheckingIteration(6061698797119349489L, 93)");
    expect(report).toContain("VimInsertFilenameUnderCaretAction");
    expect(report).toContain("findFilenameAtOrFollowingCursor");
    expect(report).toContain(branchNameFor(failure.fingerprint));
    expect(report).toContain(failure.buildUrl);
    expect(report).toContain("Times seen in the window**: 1");
    // The excerpt is truncated on purpose, so the report must say where the rest lives.
    expect(report).toContain(
      "https://ideavim.teamcity.com/guestAuth/app/rest/testOccurrences/build:(id:18998),id:2000000003?fields=details",
    );
  });

  it("says so when there is nothing to replay", () => {
    const plain = renderReport(toFailure(build(), test({ details: plainExceptionDetails })));
    expect(plain).toContain("no shrunk scenario");
    expect(plain).toContain("YankGroupBase.yankMotion");
  });

  it("summarizes the crash site for the PR title", () => {
    expect(summarizeFailure(failure)).toBe(
      "IndexOutOfBoundsException in api.VimSearchHelperBase.findFilenameAtOrFollowingCursor",
    );
  });
});
