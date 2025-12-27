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

import { writeFileSync, appendFileSync, mkdirSync, existsSync } from "fs";
import { getTicketsByQuery, getTicketDetails, getTicketComments, getTicketAttachments, downloadAttachment } from "./youtrack.js";
import { join } from "path";

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

  // Query: Open state, excluding:
  // - tickets with "claude-analyzed" tag
  // - tickets with Area "Remote Dev" or "Gateway" (not relevant to IdeaVim core)
  const query = "State: Open tag: -claude-analyzed Area: -{Remote Dev} Area: -Gateway";
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

  // Fetch ticket details, comments, and attachments
  const details = await getTicketDetails(randomTicketId);
  const comments = await getTicketComments(randomTicketId);
  const attachments = await getTicketAttachments(randomTicketId);

  console.log(`Ticket summary: ${details.summary}`);
  console.log(`Ticket state: ${details.state}`);
  console.log(`Found ${comments.length} comments`);
  console.log(`Found ${attachments.length} attachments`);

  // Format comments section
  let commentsSection = "";
  if (comments.length > 0) {
    commentsSection = "\n## Comments\n\n";
    for (const comment of comments) {
      commentsSection += `### ${comment.author} (${comment.created})\n\n${comment.text}\n\n---\n\n`;
    }
  }

  // Download image attachments and format attachments section
  let attachmentsSection = "";
  if (attachments.length > 0) {
    const attachmentsDir = join(projectDir, "attachments");

    // Create attachments directory if needed
    if (!existsSync(attachmentsDir)) {
      mkdirSync(attachmentsDir, { recursive: true });
    }

    attachmentsSection = "\n## Attachments\n\n";

    for (const attachment of attachments) {
      const mimeInfo = attachment.mimeType ? ` (${attachment.mimeType})` : "";
      const isImage = attachment.mimeType?.startsWith("image/") ?? false;

      if (isImage) {
        // Download image attachment locally
        const localPath = join(attachmentsDir, attachment.name);
        const relativePath = `./attachments/${attachment.name}`;
        const downloaded = await downloadAttachment(attachment.url, localPath);

        if (downloaded) {
          attachmentsSection += `- [${attachment.name}](${relativePath})${mimeInfo} - **LOCAL FILE: Use Read tool to view**\n`;
        } else {
          attachmentsSection += `- [${attachment.name}](${attachment.url})${mimeInfo} (download failed)\n`;
        }
      } else {
        // Non-image attachments: keep as URL
        attachmentsSection += `- [${attachment.name}](${attachment.url})${mimeInfo}\n`;
      }
    }
  }

  // Write ticket details to file for Claude to read
  const ticketDetailsPath = `${projectDir}/ticket_details.md`;
  const ticketDetailsContent = `# YouTrack Ticket: ${details.id}

## Summary
${details.summary}

## Description
${details.description ?? "No description provided"}

## Current State
${details.state}

## Created
${details.created}

## URL
https://youtrack.jetbrains.com/issue/${details.id}
${commentsSection}${attachmentsSection}`;

  writeFileSync(ticketDetailsPath, ticketDetailsContent);
  console.log(`Wrote ticket details to ${ticketDetailsPath}`);

  // Write initial analysis state JSON for multi-step workflow
  const analysisStatePath = `${projectDir}/analysis_state.json`;
  const analysisState = {
    ticket_id: details.id,
    ticket_summary: details.summary,
    ticket_type: null,
    triage_result: null,
    triage_reason: null,
    triage_attention_reason: null,
    implementation: {
      status: "pending",
      changed_files: [],
      test_files: [],
      notes: null,
      attention_reason: null
    },
    review: {
      status: "pending",
      notes: null,
      attention_reason: null
    },
    changelog: {
      status: "pending",
      attention_reason: null
    },
    pr: {
      url: null,
      branch: null,
      attention_reason: null
    },
    final_result: null
  };
  writeFileSync(analysisStatePath, JSON.stringify(analysisState, null, 2));
  console.log(`Wrote analysis state to ${analysisStatePath}`);

  // Write GitHub Actions outputs
  writeGitHubOutput("ticket_id", details.id);
  writeGitHubOutput("ticket_summary", details.summary);
}

main().catch((error) => {
  console.error("Error:", error.message);
  process.exit(1);
});
