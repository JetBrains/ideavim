#!/usr/bin/env tsx
/**
 * Updates AUTHORS.md with new contributors from recent commits.
 *
 * Iterates commits since SUCCESS_COMMIT, looks up author info via GitHub API,
 * and adds new contributors to the appropriate section in AUTHORS.md.
 *
 * Environment:
 * - SUCCESS_COMMIT: Git hash of last successful workflow run
 * - GITHUB_TOKEN: GitHub token for API access
 */

import { execSync } from "child_process";
import { readFileSync, writeFileSync, appendFileSync } from "fs";
import { join } from "path";

export interface Author {
  name: string;
  url: string;
  mail: string;
}

export function isJetBrainsEmployee(author: Author): boolean {
  return author.mail.endsWith("@jetbrains.com");
}

const CONTRIBUTORS_HEADER = "Contributors:";
const JETBRAINS_IP_HEADER = "Contributors with JetBrains IP:";
const PREVIOUS_CONTRIBUTORS_HEADER = "Previous contributors:";

const uncheckedEmails = new Set([
  "aleksei.plate@jetbrains.com",
  "aleksei.plate@teamcity",
  "aleksei.plate@TeamCity",
  "alex.plate@192.168.0.109",
  "nikita.koshcheev@TeamCity",
  "TeamCity@TeamCity",
]);

function getCommitEmailsAndHashes(projectDir: string): Map<string, string> {
  const lastSuccessfulCommit = process.env.SUCCESS_COMMIT;
  if (!lastSuccessfulCommit) {
    throw new Error("SUCCESS_COMMIT environment variable is not set");
  }

  console.log(`Last successful commit: ${lastSuccessfulCommit}`);

  // Get commits with email and hash
  const gitOutput = execSync(
    `git log --format="%ae %H" ${lastSuccessfulCommit}..HEAD`,
    { cwd: projectDir, encoding: "utf-8" }
  );

  const result = new Map<string, string>();
  const lines = gitOutput.trim().split("\n").filter(Boolean);

  for (const line of lines) {
    const spaceIndex = line.indexOf(" ");
    if (spaceIndex !== -1) {
      const email = line.substring(0, spaceIndex);
      const hash = line.substring(spaceIndex + 1);
      if (!result.has(email)) {
        result.set(email, hash);
      }
    }
  }

  console.log(`Amount of commits: ${lines.length}`);
  console.log(`Unique emails: ${Array.from(result.keys()).join(", ")}`);

  return result;
}

async function getGitHubAuthorInfo(
  hash: string
): Promise<{ name: string; url: string } | null> {
  const token = process.env.GITHUB_TOKEN;
  if (!token) {
    throw new Error("GITHUB_TOKEN environment variable is not set");
  }

  try {
    const response = await fetch(
      `https://api.github.com/repos/JetBrains/ideavim/commits/${hash}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "application/vnd.github.v3+json",
        },
      }
    );

    if (!response.ok) {
      console.log(`Failed to fetch commit ${hash}: ${response.status}`);
      return null;
    }

    const data = await response.json();
    const author = data.author;

    if (!author) {
      return null;
    }

    return {
      name: author.name || author.login,
      url: author.html_url,
    };
  } catch (error) {
    console.log(`Error fetching commit ${hash}: ${error}`);
    return null;
  }
}

export function extractExistingEmails(content: string): Set<string> {
  const regex = /mailto:([^)]+)\)/g;
  const emails = new Set<string>();
  let match;
  while ((match = regex.exec(content)) !== null) {
    emails.add(match[1]);
  }
  return emails;
}

export function extractExistingGitHubUrls(content: string): Set<string> {
  const regex = /\[!\[icon]\[github]]\((https:\/\/github\.com\/[^)]+)\)/g;
  const urls = new Set<string>();
  let match;
  while ((match = regex.exec(content)) !== null) {
    urls.add(match[1]);
  }
  return urls;
}

export function findSectionEndOffset(
  content: string,
  sectionHeader: string,
  nextSectionHeader: string
): number {
  const sectionStart = content.indexOf(sectionHeader);
  if (sectionStart === -1) return -1;

  const nextSectionStart = content.indexOf(nextSectionHeader, sectionStart);
  if (nextSectionStart === -1) return -1;

  // Find the last non-blank line before the next section
  let insertionPoint = nextSectionStart;
  while (insertionPoint > sectionStart && content[insertionPoint - 1] === "\n") {
    insertionPoint--;
  }

  return insertionPoint;
}

function authorsToMdString(authors: Author[], isJetBrainsSection: boolean): string {
  return authors
    .map((author) => {
      const nameWithNote = isJetBrainsSection
        ? `${author.name} (JetBrains employee)`
        : author.name;
      return `
* [![icon][mail]](mailto:${author.mail})
  [![icon][github]](${author.url})
  &nbsp;
  ${nameWithNote}`;
    })
    .join("");
}

export interface AddAuthorsResult {
  content: string;
  newAuthors: Author[];
}

export function addAuthorsToContent(
  authorsContent: string,
  authors: Author[]
): AddAuthorsResult {
  const existingEmails = extractExistingEmails(authorsContent);
  const existingGitHubUrls = extractExistingGitHubUrls(authorsContent);

  const newAuthors = authors.filter(
    (a) => !existingEmails.has(a.mail) && !existingGitHubUrls.has(a.url)
  );

  if (newAuthors.length === 0) {
    return { content: authorsContent, newAuthors: [] };
  }

  const jetBrainsAuthors = newAuthors.filter(isJetBrainsEmployee);
  const regularAuthors = newAuthors.filter((a) => !isJetBrainsEmployee(a));

  let result = authorsContent;

  // Add JetBrains employees to JetBrains IP section
  if (jetBrainsAuthors.length > 0) {
    const insertionPoint = findSectionEndOffset(
      result,
      JETBRAINS_IP_HEADER,
      PREVIOUS_CONTRIBUTORS_HEADER
    );
    if (insertionPoint !== -1) {
      const insertionString = authorsToMdString(jetBrainsAuthors, true);
      result =
        result.slice(0, insertionPoint) +
        insertionString +
        result.slice(insertionPoint);
    }
  }

  // Add regular contributors to Contributors section
  if (regularAuthors.length > 0) {
    const insertionPoint = findSectionEndOffset(
      result,
      CONTRIBUTORS_HEADER,
      JETBRAINS_IP_HEADER
    );
    if (insertionPoint !== -1) {
      const insertionString = authorsToMdString(regularAuthors, false);
      result =
        result.slice(0, insertionPoint) +
        insertionString +
        result.slice(insertionPoint);
    }
  }

  return { content: result, newAuthors };
}

function writeGitHubOutput(name: string, value: string): void {
  const outputFile = process.env.GITHUB_OUTPUT;
  if (outputFile) {
    appendFileSync(outputFile, `${name}=${value}\n`);
  } else {
    console.log(`OUTPUT: ${name}=${value}`);
  }
}

async function main(): Promise<void> {
  const projectDir = process.argv[2] || ".";

  console.log("Start update authors");
  console.log(`Project directory: ${projectDir}`);

  const emailsAndHashes = getCommitEmailsAndHashes(projectDir);

  const users: Author[] = [];

  console.log("Start emails processing");
  for (const [email, hash] of emailsAndHashes) {
    console.log(`Processing '${email}'...`);

    if (uncheckedEmails.has(email)) {
      console.log(`Email '${email}' is in unchecked emails. Skip it`);
      continue;
    }

    if (email.includes("[bot]@users.noreply.github.com")) {
      console.log(`Email '${email}' is from a bot. Skip it`);
      continue;
    }

    if (email.includes("tcuser")) {
      console.log(`Email '${email}' is from teamcity. Skip it`);
      continue;
    }

    const authorInfo = await getGitHubAuthorInfo(hash);
    if (!authorInfo) {
      console.log(`Can't get the commit author. Email: ${email}. Commit: ${hash}`);
      continue;
    }

    users.push({
      name: authorInfo.name,
      url: authorInfo.url,
      mail: email,
    });
  }

  console.log("Emails processed");

  const authorsFilePath = join(projectDir, "AUTHORS.md");
  const authorsContent = readFileSync(authorsFilePath, "utf-8");

  const result = addAuthorsToContent(authorsContent, users);

  if (result.newAuthors.length > 0) {
    const authorNames = result.newAuthors.map((a) => a.name).join(", ");
    console.log(`New authors: ${authorNames}`);
    writeGitHubOutput("authors", authorNames);
    writeFileSync(authorsFilePath, result.content);
    console.log("AUTHORS.md updated");
  } else {
    console.log("No new authors to add");
  }
}

// Only run main when executed directly, not when imported
const isMainModule = import.meta.url === `file://${process.argv[1]}`;
if (isMainModule) {
  main().catch((error) => {
    console.error("Error:", error.message);
    process.exit(1);
  });
}
