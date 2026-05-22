#!/usr/bin/env tsx
/**
 * Sets the TeamCity build number to the release version calculated earlier in the build.
 *
 * Usage:
 *   npx tsx scripts-ts/src/setTeamCityBuildNumber.ts [version]
 *
 * When no version is passed, reads ORG_GRADLE_PROJECT_version from the environment.
 */

function teamCityEscape(value: string): string {
  return value
    .replace(/\|/g, "||")
    .replace(/'/g, "|'")
    .replace(/\n/g, "|n")
    .replace(/\r/g, "|r")
    .replace(/\[/g, "|[")
    .replace(/]/g, "|]");
}

function main(): void {
  const version = process.argv[2] ?? process.env.ORG_GRADLE_PROJECT_version;

  if (!version) {
    console.error(
      "Usage: setTeamCityBuildNumber.ts [version]\n" +
        "Or set ORG_GRADLE_PROJECT_version in the environment.",
    );
    process.exit(1);
  }

  console.log(`##teamcity[buildNumber '${teamCityEscape(version)}']`);
}

main();

export {};
