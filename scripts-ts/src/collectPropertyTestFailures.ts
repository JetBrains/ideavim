#!/usr/bin/env tsx
/**
 * Collects property test failures from the TeamCity `Ideavim_PropertyBased` configuration
 * and turns them into one report file per distinct bug, plus a GitHub Actions job matrix.
 *
 * Reads TeamCity with guest auth, so it runs locally without any credentials:
 *
 *   cd scripts-ts && npm install
 *   npx tsx src/collectPropertyTestFailures.ts --hours 72 --out ../property_failures
 *   npx tsx src/collectPropertyTestFailures.ts --build 18998 --out /tmp/failures   # one known build
 *
 * Outputs (in --out):
 * - `<fingerprint>.md`   report Claude works from
 * - `failures.json`      machine-readable summary of everything found
 *
 * GitHub Actions outputs: has_failures, count, matrix
 */

import { appendFileSync, mkdirSync, writeFileSync } from "node:fs";
import { join, relative, resolve } from "node:path";
import { fetchBuild, fetchFailedTests, fetchFinishedBuilds, type TeamCityBuild } from "./tools/teamcity.js";
import {
  affectedTests,
  branchNameFor,
  dedupeFailures,
  renderReport,
  selectFailedBuildsInWindow,
  summarizeFailure,
  toFailure,
  type PropertyFailure,
} from "./propertyTestFailures.js";

const DEFAULT_BUILD_TYPE = "Ideavim_PropertyBased";

interface Options {
  buildTypeId: string;
  hours: number;
  max: number;
  out: string;
  buildId: number | null;
  allBranches: boolean;
  skipExisting: boolean;
}

function parseArgs(argv: string[]): Options {
  const options: Options = {
    buildTypeId: process.env.TEAMCITY_BUILD_TYPE ?? DEFAULT_BUILD_TYPE,
    hours: 24,
    max: 5,
    out: "property_failures",
    buildId: null,
    allBranches: false,
    skipExisting: true,
  };

  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    const value = () => {
      const next = argv[++i];
      if (next === undefined) throw new Error(`Missing value for ${arg}`);
      return next;
    };

    switch (arg) {
      case "--build-type": options.buildTypeId = value(); break;
      case "--hours": options.hours = Number(value()); break;
      case "--max": options.max = Number(value()); break;
      case "--out": options.out = value(); break;
      case "--build": options.buildId = Number(value()); break;
      case "--all-branches": options.allBranches = true; break;
      case "--no-skip-existing": options.skipExisting = false; break;
      default: throw new Error(`Unknown argument: ${arg}`);
    }
  }

  if (!Number.isFinite(options.hours) || options.hours <= 0) throw new Error("--hours must be a positive number");
  if (!Number.isFinite(options.max) || options.max <= 0) throw new Error("--max must be a positive number");

  return options;
}

function writeGitHubOutput(name: string, value: string): void {
  const outputFile = process.env.GITHUB_OUTPUT;
  if (outputFile) {
    appendFileSync(outputFile, `${name}=${value}\n`);
  } else {
    console.log(`OUTPUT: ${name}=${value}`);
  }
}

async function githubJson<T>(path: string): Promise<T | null> {
  const repository = process.env.GITHUB_REPOSITORY;
  const token = process.env.GITHUB_TOKEN;
  if (!repository || !token) return null;

  const response = await fetch(`https://api.github.com/repos/${repository}${path}`, {
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${token}`,
      "X-GitHub-Api-Version": "2022-11-28",
    },
  });

  if (!response.ok) {
    console.warn(`GitHub request failed (${response.status}): ${path}`);
    return null;
  }
  return (await response.json()) as T;
}

/**
 * Fingerprints that already have a branch or a pull request, open or not.
 *
 * Without this the nightly run would re-open the same PR every day for as long
 * as the failure keeps happening - and it keeps happening until someone merges.
 */
async function alreadyHandledFingerprints(): Promise<Set<string>> {
  const handled = new Set<string>();
  const fromBranch = (ref: string) => {
    const match = /fix\/property-([0-9a-f]+)/.exec(ref);
    if (match) handled.add(match[1]);
  };

  const refs = await githubJson<Array<{ ref: string }>>("/git/matching-refs/heads/fix/property-");
  refs?.forEach((entry) => fromBranch(entry.ref));

  const pulls = await githubJson<Array<{ head: { ref: string } }>>("/pulls?state=all&per_page=100");
  pulls?.forEach((pull) => fromBranch(pull.head?.ref ?? ""));

  return handled;
}

async function collectFailures(options: Options): Promise<PropertyFailure[]> {
  let builds: TeamCityBuild[];

  if (options.buildId !== null) {
    builds = [await fetchBuild(options.buildId)];
    console.log(`Inspecting build ${options.buildId} (#${builds[0].number}, ${builds[0].status})`);
  } else {
    const finished = await fetchFinishedBuilds(options.buildTypeId, { count: 50, allBranches: options.allBranches });
    builds = selectFailedBuildsInWindow(finished, { now: new Date(), hours: options.hours });
    console.log(
      `${finished.length} finished builds of ${options.buildTypeId}, ` +
        `${builds.length} failed within the last ${options.hours}h`,
    );
  }

  const failures: PropertyFailure[] = [];
  for (const build of builds) {
    const tests = await fetchFailedTests(build.id);
    console.log(`  build #${build.number} (${build.id}): ${tests.length} failed test(s)`);
    for (const test of tests) {
      failures.push(toFailure(build, test));
    }
  }

  return failures;
}

async function main(): Promise<void> {
  const options = parseArgs(process.argv.slice(2));
  const outDir = resolve(options.out);

  const failures = await collectFailures(options);
  const distinct = dedupeFailures(failures);
  console.log(`${failures.length} failure(s) collapse into ${distinct.length} distinct bug(s)`);

  let selected = distinct;
  if (options.skipExisting) {
    const handled = await alreadyHandledFingerprints();
    selected = distinct.filter((failure) => {
      if (!handled.has(failure.fingerprint)) return true;
      console.log(`  skipping ${failure.fingerprint} - ${branchNameFor(failure.fingerprint)} already exists`);
      return false;
    });
  }

  if (selected.length > options.max) {
    const dropped = selected.slice(options.max);
    console.log(
      `Capping at --max ${options.max}; not handling this run: ` +
        dropped.map((failure) => `${failure.fingerprint} (${summarizeFailure(failure)})`).join(", "),
    );
    selected = selected.slice(0, options.max);
  }

  mkdirSync(outDir, { recursive: true });

  const include = selected.map((failure) => {
    const reportPath = join(outDir, `${failure.fingerprint}.md`);
    writeFileSync(reportPath, renderReport(failure));
    console.log(`  wrote ${reportPath}`);

    return {
      fingerprint: failure.fingerprint,
      branch: branchNameFor(failure.fingerprint),
      summary: summarizeFailure(failure),
      test_name: failure.testName,
      test_class: failure.testClass,
      test_method: failure.testMethod,
      affected_tests: affectedTests(failure).join(" "),
      occurrences: failure.occurrences.length,
      build_number: failure.buildNumber,
      build_url: failure.buildUrl,
      // Relative to the repository root, which is where the fix job runs.
      report: relative(resolve(outDir, ".."), reportPath),
    };
  });

  writeFileSync(
    join(outDir, "failures.json"),
    JSON.stringify(
      {
        collected_at: new Date().toISOString(),
        build_type: options.buildTypeId,
        window_hours: options.buildId === null ? options.hours : null,
        total_failures: failures.length,
        distinct_failures: distinct.length,
        selected: include,
        all: distinct.map((failure) => ({
          fingerprint: failure.fingerprint,
          summary: summarizeFailure(failure),
          test_name: failure.testName,
          affected_tests: affectedTests(failure),
          occurrences: failure.occurrences.length,
          build_number: failure.buildNumber,
          build_url: failure.buildUrl,
          exception: failure.parsed.exceptionClass,
          message: failure.parsed.exceptionMessage,
          rechecking_seed: failure.parsed.recheckingSeed,
        })),
      },
      null,
      2,
    ),
  );

  writeGitHubOutput("has_failures", include.length > 0 ? "true" : "false");
  writeGitHubOutput("count", String(include.length));
  writeGitHubOutput("matrix", JSON.stringify({ include }));

  console.log(`\n${include.length} failure(s) to fix:`);
  for (const entry of include) {
    console.log(
      `  ${entry.fingerprint}  ${entry.summary}  ` +
        `(${entry.test_method}, build #${entry.build_number}, seen ${entry.occurrences}x)`,
    );
  }
}

main().catch((error) => {
  console.error("Error:", error instanceof Error ? error.message : error);
  process.exit(1);
});
