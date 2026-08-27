/**
 * Minimal TeamCity REST client for ideavim.teamcity.com.
 *
 * Reads are done with guest auth, which needs no credentials at all
 * (the same access the build status badge in CONTRIBUTING.md uses).
 * Set TEAMCITY_TOKEN to use a bearer token instead, and TEAMCITY_URL to
 * point at another server.
 */

const DEFAULT_URL = "https://ideavim.teamcity.com";

export interface TeamCityBuild {
  id: number;
  number: string;
  status: string;
  state: string;
  branchName?: string;
  statusText?: string;
  startDate?: string;
  finishDate?: string;
  webUrl: string;
}

export interface TeamCityTestOccurrence {
  id: string;
  name: string;
  status: string;
  newFailure?: boolean;
  duration?: number;
  details?: string;
}

function serverUrl(): string {
  return (process.env.TEAMCITY_URL ?? DEFAULT_URL).replace(/\/+$/, "");
}

async function tcGet<T>(path: string): Promise<T> {
  const token = process.env.TEAMCITY_TOKEN;
  // Without a token the /guestAuth/ prefix grants read-only access.
  const url = `${serverUrl()}${token ? "" : "/guestAuth"}${path}`;

  const response = await fetch(url, {
    headers: {
      Accept: "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`TeamCity request failed (${response.status}): ${url}\n${body.slice(0, 500)}`);
  }

  return (await response.json()) as T;
}

/**
 * TeamCity timestamps look like `20260826T090213+0000`, which `new Date()` cannot parse.
 */
export function parseTeamCityDate(value: string): Date {
  const match = /^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})([+-]\d{4}|Z)?$/.exec(value.trim());
  if (!match) throw new Error(`Unrecognized TeamCity date: ${value}`);

  const [, year, month, day, hour, minute, second, zone] = match;
  const offset = !zone || zone === "Z" ? "+00:00" : `${zone.slice(0, 3)}:${zone.slice(3)}`;
  return new Date(`${year}-${month}-${day}T${hour}:${minute}:${second}${offset}`);
}

/**
 * Finished builds of a configuration, newest first.
 *
 * Without `allBranches` TeamCity returns default-branch builds only, which is what a
 * nightly sweep wants: failures on someone's feature branch are not our regressions.
 */
export async function fetchFinishedBuilds(
  buildTypeId: string,
  options: { count?: number; allBranches?: boolean } = {},
): Promise<TeamCityBuild[]> {
  const { count = 50, allBranches = false } = options;
  const locator = [
    `buildType:(id:${buildTypeId})`,
    "state:finished",
    "canceled:false",
    "personal:false",
    ...(allBranches ? ["branch:(policy:ALL_BRANCHES)"] : []),
    `count:${count}`,
  ].join(",");
  const fields = "build(id,number,status,state,branchName,statusText,startDate,finishDate,webUrl)";

  const result = await tcGet<{ build?: TeamCityBuild[] }>(
    `/app/rest/builds?locator=${encodeURIComponent(locator)}&fields=${encodeURIComponent(fields)}`,
  );
  return result.build ?? [];
}

export async function fetchBuild(buildId: number): Promise<TeamCityBuild> {
  const fields = "id,number,status,state,branchName,statusText,startDate,finishDate,webUrl";
  return tcGet<TeamCityBuild>(`/app/rest/builds/id:${buildId}?fields=${encodeURIComponent(fields)}`);
}

/**
 * Guest-auth URL of one occurrence's complete failure output - megabytes of it, so this is a
 * link to follow on demand rather than something to inline:
 *   curl -sS "<url>" | jq -r .details | head -300
 */
export function testOccurrenceDetailsUrl(occurrenceId: string): string {
  return `${serverUrl()}/guestAuth/app/rest/testOccurrences/${occurrenceId}?fields=details`;
}

/** Failed, non-muted test occurrences of a build, including their full failure output. */
export async function fetchFailedTests(buildId: number): Promise<TeamCityTestOccurrence[]> {
  const locator = `build:(id:${buildId}),status:FAILURE,muted:false,ignored:false,count:100`;
  const fields = "testOccurrence(id,name,status,newFailure,duration,details)";

  const result = await tcGet<{ testOccurrence?: TeamCityTestOccurrence[] }>(
    `/app/rest/testOccurrences?locator=${encodeURIComponent(locator)}&fields=${encodeURIComponent(fields)}`,
  );
  return result.testOccurrence ?? [];
}
