/**
 * Parsing and grouping of property test failures reported by TeamCity.
 *
 * Two shapes of failure output show up in the `Ideavim_PropertyBased` configuration:
 * - jetCheck's `PropertyFalsified`, with a shrunk command list and replay seeds;
 * - a plain exception with a stack trace, when the failure escapes before jetCheck reports.
 * Both are parsed here; only the first one carries seeds.
 *
 * Everything in this file is pure so it can be tested without touching the network;
 * `collectPropertyTestFailures.ts` is the thin IO layer on top.
 */

import { createHash } from "node:crypto";
import {
  parseTeamCityDate,
  testOccurrenceDetailsUrl,
  type TeamCityBuild,
  type TeamCityTestOccurrence,
} from "./tools/teamcity.js";

/** Frames worth showing: the IdeaVim ones, not the 200 platform frames around them. */
const ENGINE_FRAME = /^\s*at (com\.maddyhome\.idea\.vim\.[\w.$]+)\(([^)]*)\)/gm;

const MAX_ENGINE_FRAMES = 10;
const MAX_STACK_EXCERPT_LINES = 60;

const SCENARIO_HEADER = /^On commands:/;
const SCENARIO_LINE = /^\s*(?:Use command:|Put caret at position)/;
const COMMAND_LINE = /^\s*Use command:\s?(.*)$/;
const CARET_LINE = /^\s*Put caret at position (\d+)/;

export interface ParsedFailure {
  /** Innermost exception class, e.g. `java.lang.IndexOutOfBoundsException`. */
  exceptionClass: string | null;
  /** Innermost exception message, e.g. `Index out of range: 76; length: 76`. */
  exceptionMessage: string | null;
  /** The `Put caret at position N` line of the minimal scenario. */
  caretPosition: number | null;
  /** The `Use command: ...` lines of the minimal scenario, in order. */
  commands: string[];
  /** Argument of `PropertyChecker.customized().rechecking("...")` - replays the minimal case. */
  recheckingSeed: string | null;
  /** Arguments of `recheckingIteration(seed, iteration)` - replays with shrinking steps. */
  recheckingIteration: { seed: string; iteration: number } | null;
  /** Argument of `withSeed(...)` - replays all iterations. */
  seed: string | null;
  /** The `Shrunk in N stages, by trying M examples` line. */
  shrinkInfo: string | null;
  /** IdeaVim stack frames, closest to the failure first. */
  engineFrames: string[];
  /** Head of the stack trace, trimmed to something a prompt can hold. */
  stackExcerpt: string;
  /** Whether jetCheck reported a shrunk scenario, i.e. whether there is anything to replay. */
  hasScenario: boolean;
}

/** One build/test pair that hit a failure. */
export interface FailureOccurrence {
  buildId: number;
  buildNumber: string;
  buildUrl: string;
  branchName: string;
  finishDate: string | null;
  testName: string;
}

export interface PropertyFailure {
  /** Stable id of the underlying bug: the same crash from another seed or another sample text
   *  gets the same value, so one bug never turns into two pull requests. */
  fingerprint: string;
  testName: string;
  testClass: string;
  testMethod: string;
  buildId: number;
  buildNumber: string;
  buildUrl: string;
  branchName: string;
  finishDate: string | null;
  newFailure: boolean;
  /** Where to fetch the complete, untruncated failure output. */
  fullOutputUrl: string;
  parsed: ParsedFailure;
  /** Every build/test pair that hit this bug, newest first. Filled in by {@link dedupeFailures}. */
  occurrences: FailureOccurrence[];
}

const EXCEPTION_LINE = /^([\w.$]+(?:Exception|Error|Throwable|Failure))(?::\s*(.*))?$/s;

function splitException(value: string): { exceptionClass: string | null; exceptionMessage: string | null } {
  const trimmed = value.trim();
  const match = EXCEPTION_LINE.exec(trimmed);
  if (match) return { exceptionClass: match[1], exceptionMessage: match[2]?.trim() || null };
  return { exceptionClass: null, exceptionMessage: trimmed || null };
}

/**
 * The shrunk scenario: the caret it starts from and the keys that follow.
 *
 * Only the FIRST block counts. jetCheck repeats the same block further down its (megabyte-sized)
 * output, and collecting every `Use command:` line in the text would hand out a scenario with the
 * key sequence pasted twice - a reproduction that no longer reproduces.
 */
function extractScenario(lines: string[]): { commands: string[]; caretPosition: number | null } {
  const header = lines.findIndex((line) => SCENARIO_HEADER.test(line.trim()));
  const start = header >= 0 ? header + 1 : lines.findIndex((line) => COMMAND_LINE.test(line));
  if (start < 0) return { commands: [], caretPosition: null };

  const commands: string[] = [];
  let caretPosition: number | null = null;

  for (let i = start; i < lines.length; i++) {
    const line = lines[i];
    // The block ends at the first line that is not part of it - `Shrunk in ...`, a blank
    // line, or the start of the replay instructions.
    if (!SCENARIO_LINE.test(line)) break;

    const caret = CARET_LINE.exec(line);
    if (caret) {
      caretPosition ??= Number(caret[1]);
      continue;
    }
    const command = COMMAND_LINE.exec(line);
    if (command) commands.push(command[1].trimEnd());
  }

  return { commands, caretPosition };
}

export function parseFailureDetails(details: string): ParsedFailure {
  const text = details.replace(/\r\n/g, "\n");
  const lines = text.split("\n");

  // Preference order: jetCheck's innermost exception, its wrapper line, then a plain stack trace.
  const innermost = /innermost exception[^:]*:\s*(.+)/.exec(text);
  const failedWith = /Failed with\s+(.+)/.exec(text);
  const firstLine = lines.find((line) => line.trim().length > 0) ?? "";
  const { exceptionClass, exceptionMessage } = splitException(innermost?.[1] ?? failedWith?.[1] ?? firstLine);

  const { commands, caretPosition } = extractScenario(lines);

  const rechecking = /rechecking\("([^"]+)"\)/.exec(text);
  const iteration = /recheckingIteration\((-?\d+)L?\s*,\s*(\d+)\)/.exec(text);
  const seed = /withSeed\((-?\d+)L?\)/.exec(text);
  const shrink = /^Shrunk in .*$/m.exec(text);

  const engineFrames: string[] = [];
  for (const match of text.matchAll(ENGINE_FRAME)) {
    const frame = `${match[1]}(${match[2]})`;
    if (!engineFrames.includes(frame)) engineFrames.push(frame);
    if (engineFrames.length >= MAX_ENGINE_FRAMES) break;
  }

  const traceStart = lines.findIndex((line) => line.includes("innermost exception"));
  const excerptFrom = traceStart >= 0 ? traceStart : 0;
  const stackExcerpt = lines.slice(excerptFrom, excerptFrom + MAX_STACK_EXCERPT_LINES).join("\n").trimEnd();

  return {
    exceptionClass,
    exceptionMessage,
    caretPosition,
    commands,
    recheckingSeed: rechecking?.[1] ?? null,
    recheckingIteration: iteration ? { seed: iteration[1], iteration: Number(iteration[2]) } : null,
    seed: seed?.[1] ?? null,
    shrinkInfo: shrink?.[0]?.trim() ?? null,
    engineFrames,
    stackExcerpt,
    hasScenario: commands.length > 0,
  };
}

/**
 * Drops the parts of a message that differ between runs of the same bug - offsets,
 * lengths, caret positions - so that two seeds hitting one crash fingerprint alike.
 */
export function normalizeMessage(message: string | null): string {
  if (!message) return "";
  return message.replace(/-?\d+/g, "N").replace(/\s+/g, " ").trim();
}

/** Frame without its line number: a shifted line must not look like a new bug. */
function frameWithoutLocation(frame: string | undefined): string {
  return frame ? frame.replace(/\([^)]*\)/, "") : "";
}

/**
 * Identity of the bug, deliberately independent of the test that surfaced it: the same
 * crash reached from `testRandomActions` and from `testRandomActionsOnLoremIpsum` is one
 * bug and deserves one fix, not two competing pull requests.
 *
 * `testName` is only a tiebreaker for output we could not parse at all.
 */
export function fingerprintFailure(parsed: ParsedFailure, testName = ""): string {
  const crashSite = frameWithoutLocation(parsed.engineFrames[0]);
  const parts = parsed.exceptionClass || crashSite
    ? [parsed.exceptionClass ?? "unknown-exception", normalizeMessage(parsed.exceptionMessage), crashSite]
    : [testName, normalizeMessage(parsed.stackExcerpt.split("\n").slice(0, 3).join(" "))];

  return createHash("sha256").update(parts.join("|")).digest("hex").slice(0, 12);
}

export function branchNameFor(fingerprint: string): string {
  return `fix/property-${fingerprint}`;
}

function splitTestName(testName: string): { testClass: string; testMethod: string } {
  const lastDot = testName.lastIndexOf(".");
  if (lastDot < 0) return { testClass: testName, testMethod: "" };
  return { testClass: testName.slice(0, lastDot), testMethod: testName.slice(lastDot + 1) };
}

export function toFailure(build: TeamCityBuild, test: TeamCityTestOccurrence): PropertyFailure {
  const parsed = parseFailureDetails(test.details ?? "");
  const { testClass, testMethod } = splitTestName(test.name);
  const branchName = build.branchName ?? "<default>";

  return {
    fingerprint: fingerprintFailure(parsed, test.name),
    testName: test.name,
    testClass,
    testMethod,
    buildId: build.id,
    buildNumber: build.number,
    buildUrl: build.webUrl,
    branchName,
    finishDate: build.finishDate ?? null,
    newFailure: test.newFailure ?? false,
    fullOutputUrl: testOccurrenceDetailsUrl(test.id),
    parsed,
    occurrences: [
      {
        buildId: build.id,
        buildNumber: build.number,
        buildUrl: build.webUrl,
        branchName,
        finishDate: build.finishDate ?? null,
        testName: test.name,
      },
    ],
  };
}

/**
 * Failed builds that finished inside the lookback window, newest first.
 * A nightly run with `hours: 24` therefore sees exactly the previous day.
 */
export function selectFailedBuildsInWindow(
  builds: TeamCityBuild[],
  options: { now: Date; hours: number },
): TeamCityBuild[] {
  const cutoff = options.now.getTime() - options.hours * 60 * 60 * 1000;

  return builds
    .filter((build) => build.status === "FAILURE" && build.state === "finished")
    .filter((build) => {
      if (!build.finishDate) return false;
      return parseTeamCityDate(build.finishDate).getTime() >= cutoff;
    })
    .sort((a, b) => b.id - a.id);
}

/**
 * One entry per distinct bug, newest first. The newest occurrence supplies the report's
 * seeds and scenario; the rest are kept in `occurrences` as evidence the failure is not
 * a one-off, and to show which tests reach it.
 */
export function dedupeFailures(failures: PropertyFailure[]): PropertyFailure[] {
  const byFingerprint = new Map<string, PropertyFailure>();

  for (const failure of [...failures].sort((a, b) => b.buildId - a.buildId)) {
    const known = byFingerprint.get(failure.fingerprint);
    if (known) {
      known.occurrences.push(...failure.occurrences);
      // A scenario with replay seeds beats one without, whatever the build order.
      if (!known.parsed.hasScenario && failure.parsed.hasScenario) known.parsed = failure.parsed;
    } else {
      byFingerprint.set(failure.fingerprint, { ...failure, occurrences: [...failure.occurrences] });
    }
  }

  return [...byFingerprint.values()];
}

export function summarizeFailure(failure: PropertyFailure): string {
  const exception = failure.parsed.exceptionClass?.split(".").pop() ?? "Property falsified";
  const frame = failure.parsed.engineFrames[0]?.replace(/^com\.maddyhome\.idea\.vim\./, "").replace(/\([^)]*\)/, "");
  return frame ? `${exception} in ${frame}` : `${exception} in ${failure.testMethod}`;
}

/** Test methods that reached this bug, so the fix can be verified against all of them. */
export function affectedTests(failure: PropertyFailure): string[] {
  return [...new Set(failure.occurrences.map((occurrence) => occurrence.testName))];
}

export function renderReport(failure: PropertyFailure): string {
  const { parsed } = failure;

  const commands = parsed.commands.length > 0
    ? parsed.commands.map((command) => `- \`${command}\``).join("\n")
    : "_jetCheck reported no shrunk scenario - reproduce from the stack trace instead_";

  const engineFrames = parsed.engineFrames.length > 0
    ? parsed.engineFrames.map((frame) => `- \`${frame}\``).join("\n")
    : "_no IdeaVim frames in the trace_";

  const rechecking = parsed.recheckingSeed
    ? `PropertyChecker.customized().rechecking("${parsed.recheckingSeed}")\n  .checkScenarios(...)`
    : "_not reported_";

  const occurrences = failure.occurrences
    .map(
      (occurrence) =>
        `- [#${occurrence.buildNumber}](${occurrence.buildUrl}) \`${occurrence.testName.split(".").pop()}\`` +
        `${occurrence.finishDate ? ` - ${occurrence.finishDate}` : ""}`,
    )
    .join("\n");

  return `# Property test failure ${failure.fingerprint}

- **Test that reported it**: \`${failure.testName}\`
- **Summary**: ${summarizeFailure(failure)}
- **Newest TeamCity build**: [#${failure.buildNumber}](${failure.buildUrl}) (branch \`${failure.branchName}\`${
    failure.finishDate ? `, finished ${failure.finishDate}` : ""
  })
- **Times seen in the window**: ${failure.occurrences.length}
- **Branch to use for the fix**: \`${branchNameFor(failure.fingerprint)}\`

## Failure

- **Exception**: \`${parsed.exceptionClass ?? "unknown"}\`
- **Message**: ${parsed.exceptionMessage ?? "_none_"}
- **Shrinking**: ${parsed.shrinkInfo ?? "_not reported_"}

## Minimal scenario

Caret start position: ${parsed.caretPosition ?? "_not reported_"}

Keys and actions, in order:

${commands}

## IdeaVim frames (closest to the failure first)

${engineFrames}

## Reproduction seeds

Minimal failing case:

\`\`\`kotlin
${rechecking}
\`\`\`

- With shrinking steps: ${
    parsed.recheckingIteration
      ? `\`recheckingIteration(${parsed.recheckingIteration.seed}L, ${parsed.recheckingIteration.iteration})\``
      : "_not reported_"
  }
- All iterations: ${parsed.seed ? `\`withSeed(${parsed.seed}L)\`` : "_not reported_"}

## Where it was seen

${occurrences}

## Stack trace excerpt

Only the head of the trace is kept here. The complete output (megabytes, including the full
nested trace) is one request away:

\`\`\`bash
curl -sS "${failure.fullOutputUrl}" | jq -r .details | head -300
\`\`\`

\`\`\`
${parsed.stackExcerpt}
\`\`\`
`;
}
