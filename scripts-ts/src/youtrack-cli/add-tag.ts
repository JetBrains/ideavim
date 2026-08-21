#!/usr/bin/env npx tsx
/**
 * CLI wrapper to add a tag to a YouTrack ticket
 *
 * Usage:
 *   npx tsx scripts-ts/src/youtrack-cli/add-tag.ts <ticket-id> <tag-id-or-name>
 *
 * Examples:
 *   npx tsx scripts-ts/src/youtrack-cli/add-tag.ts VIM-1234 68-507461
 *   npx tsx scripts-ts/src/youtrack-cli/add-tag.ts VIM-1234 ai-area-processed
 *
 * A tag id has the form "68-507461". Anything else is resolved as a tag name,
 * which must already exist in YouTrack.
 *
 * Common tag IDs:
 *   68-507461 - claude-analyzed
 *   68-507582 - claude-pending-clarification
 *   68-385032 - IdeaVim Released In EAP
 *
 * Environment:
 *   YOUTRACK_TOKEN - Required. YouTrack API token.
 */

import { setTag, setTagByName } from "../tools/youtrack.js";

const TAG_ID_PATTERN = /^\d+-\d+$/;

async function main() {
  const args = process.argv.slice(2);

  if (args.length !== 2) {
    console.error("Usage: add-tag.ts <ticket-id> <tag-id-or-name>");
    console.error("Example: add-tag.ts VIM-1234 68-507461");
    console.error("Example: add-tag.ts VIM-1234 ai-area-processed");
    console.error("");
    console.error("Common tag IDs:");
    console.error("  68-507461 - claude-analyzed");
    console.error("  68-507582 - claude-pending-clarification");
    console.error("  68-385032 - IdeaVim Released In EAP");
    process.exit(1);
  }

  const [ticketId, tag] = args;

  try {
    if (TAG_ID_PATTERN.test(tag)) {
      await setTag(ticketId, tag);
    } else {
      await setTagByName(ticketId, tag);
    }
  } catch (error) {
    console.error(`Error: ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
}

main();
