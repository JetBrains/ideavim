#!/usr/bin/env tsx
/**
 * Selects a random open YouTrack ticket for Claude analysis.
 *
 * Query: State is Open AND ticket is NOT tagged with "claude-analyzed"
 *
 * Outputs:
 * - ticket_details.md: Markdown file with ticket info for Claude to read
 * - GitHub Actions outputs: ticket_id, ticket_summary (via GITHUB_OUTPUT)
 */

import { writeFileSync, appendFileSync } from "fs";
import { getTicketsByQuery, getTicketDetails } from "./youtrack.js";

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

  console.log("Searching for open YouTrack tickets not yet analyzed by Claude...");

  // Query: Open state, excluding tickets with "claude-analyzed" tag
  const query = "State: Open tag: -claude-analyzed";
  const tickets = await getTicketsByQuery(query);

  console.log(`Found ${tickets.length} unanalyzed open tickets`);

  if (tickets.length === 0) {
    console.log("No unanalyzed tickets found");
    writeGitHubOutput("ticket_id", "");
    writeGitHubOutput("ticket_summary", "");
    return;
  }

  // Pick a random ticket
  const randomTicketId = tickets[Math.floor(Math.random() * tickets.length)];
  console.log(`Selected random ticket: ${randomTicketId}`);

  // Fetch ticket details
  const details = await getTicketDetails(randomTicketId);
  console.log(`Ticket summary: ${details.summary}`);
  console.log(`Ticket state: ${details.state}`);

  // Write ticket details to file for Claude to read
  const ticketDetailsPath = `${projectDir}/ticket_details.md`;
  const ticketDetailsContent = `# YouTrack Ticket: ${details.id}

## Summary
${details.summary}

## Description
${details.description ?? "No description provided"}

## Current State
${details.state}

## URL
https://youtrack.jetbrains.com/issue/${details.id}
`;

  writeFileSync(ticketDetailsPath, ticketDetailsContent);
  console.log(`Wrote ticket details to ${ticketDetailsPath}`);

  // Write GitHub Actions outputs
  writeGitHubOutput("ticket_id", details.id);
  writeGitHubOutput("ticket_summary", details.summary);
}

main().catch((error) => {
  console.error("Error:", error.message);
  process.exit(1);
});
