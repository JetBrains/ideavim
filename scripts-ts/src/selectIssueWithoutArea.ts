#!/usr/bin/env tsx
/**
 * Selects a random unresolved YouTrack ticket that has no Area field set.
 *
 * Query: Area is not set AND state is Unresolved AND ticket is NOT tagged with
 * "ai-area-processed"
 *
 * A ticket id may be passed as an argument to process that ticket instead of
 * picking one. The ticket is then used as given, even if it already has an Area.
 *
 * Outputs (via GITHUB_OUTPUT):
 * - ticket_id: id of the selected ticket, empty when there is nothing to do
 * - has_ticket: "true" or "false"
 */

import { appendFileSync } from "fs";
import { getTicketsByQuery, getTicketDetails } from "./tools/youtrack.js";

const UNTRIAGED_QUERY =
  "Area: NOT_DEFINED State: Unresolved tag: -ai-area-processed";

function writeGitHubOutput(name: string, value: string): void {
  const outputFile = process.env.GITHUB_OUTPUT;
  if (outputFile) {
    appendFileSync(outputFile, `${name}=${value}\n`);
  } else {
    console.log(`OUTPUT: ${name}=${value}`);
  }
}

function selectNothing(): void {
  writeGitHubOutput("ticket_id", "");
  writeGitHubOutput("has_ticket", "false");
}

async function main(): Promise<void> {
  const requestedTicket = process.argv[2]?.trim();

  let selectedTicketId: string;

  if (requestedTicket) {
    console.log(`Ticket given explicitly: ${requestedTicket}`);
    selectedTicketId = requestedTicket;
  } else {
    console.log("Searching for unresolved tickets without an Area...");
    const tickets = await getTicketsByQuery(UNTRIAGED_QUERY);

    console.log(`Found ${tickets.length} tickets without an Area`);

    if (tickets.length === 0) {
      console.log("Nothing to process");
      selectNothing();
      return;
    }

    selectedTicketId = tickets[Math.floor(Math.random() * tickets.length)];
    console.log(`Selected random ticket: ${selectedTicketId}`);
  }

  // Fail early on a typo in the requested id, instead of letting the agent
  // discover it
  const details = await getTicketDetails(selectedTicketId);

  console.log(`Ticket: ${details.id}`);
  console.log(`Summary: ${details.summary}`);
  console.log(`State: ${details.state}`);
  console.log(`URL: https://youtrack.jetbrains.com/issue/${details.id}`);

  writeGitHubOutput("ticket_id", details.id);
  writeGitHubOutput("has_ticket", "true");
}

main().catch((error) => {
  console.error("Error:", error.message);
  process.exit(1);
});
