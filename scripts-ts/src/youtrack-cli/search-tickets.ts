#!/usr/bin/env npx tsx
/**
 * CLI wrapper to search VIM tickets and show their Area values
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/search-tickets.ts <query> [--json]
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/search-tickets.ts "Area: {Visual Mode}"
 *   npx tsx scripts-ts/src/youtrack-cli/search-tickets.ts "summary: undo" --json
 *
 * The query is a YouTrack search query. "project: VIM" is added automatically.
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { searchTickets } from "../tools/youtrack.js";

async function main() {
  const args = process.argv.slice(2);
  const jsonOutput = args.includes("--json");
  const query = args.filter((arg) => arg !== "--json").join(" ");

  if (!query) {
    console.error('Usage: search-tickets.ts <query> [--json]');
    console.error('Example: search-tickets.ts "Area: {Visual Mode}"');
    process.exit(1);
  }

  try {
    const tickets = await searchTickets(query);

    if (jsonOutput) {
      console.log(JSON.stringify(tickets, null, 2));
    } else {
      console.log(`Tickets (${tickets.length}):`);
      for (const ticket of tickets) {
        const areas = ticket.areas.length > 0 ? ticket.areas.join(", ") : "none";
        console.log(`  ${ticket.id} [${areas}] ${ticket.summary}`);
      }
    }
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
