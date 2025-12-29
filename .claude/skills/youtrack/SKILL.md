---
name: youtrack
description: Interacts with YouTrack issue tracker for IdeaVim. Use when working with tickets (VIM-XXXX), adding comments, managing tags, setting status, or release management. This project does NOT use GitHub Issues - use YouTrack instead.
---

# YouTrack Integration

This project uses **YouTrack** for issue tracking, NOT GitHub Issues. Commands like `gh issue` will NOT work.

- YouTrack URL: https://youtrack.jetbrains.com/issues/VIM
- Tickets are prefixed with `VIM-` (e.g., VIM-1234)

## Environment Setup

All scripts require `YOUTRACK_TOKEN` environment variable to be set.

## CLI Scripts

All scripts are in `scripts-ts/src/youtrack-cli/` and run via `npx tsx`:

### Add Comment
```bash
npx tsx scripts-ts/src/youtrack-cli/add-comment.ts <ticket-id> "<comment-text>" [--private]
```
Examples:
```bash
npx tsx scripts-ts/src/youtrack-cli/add-comment.ts VIM-1234 "This is a public comment"
npx tsx scripts-ts/src/youtrack-cli/add-comment.ts VIM-1234 "@Aleksei.Plate This needs review" --private
```

### Add Tag
```bash
npx tsx scripts-ts/src/youtrack-cli/add-tag.ts <ticket-id> <tag-id>
```
Example:
```bash
npx tsx scripts-ts/src/youtrack-cli/add-tag.ts VIM-1234 68-507582
```

### Remove Tag
```bash
npx tsx scripts-ts/src/youtrack-cli/remove-tag.ts <ticket-id> <tag-id>
```
Example:
```bash
npx tsx scripts-ts/src/youtrack-cli/remove-tag.ts VIM-1234 68-507582
```

### Set Status
```bash
npx tsx scripts-ts/src/youtrack-cli/set-status.ts <ticket-id> "<status>"
```
Example:
```bash
npx tsx scripts-ts/src/youtrack-cli/set-status.ts VIM-1234 "Ready To Release"
```

### Get Ticket Details
```bash
npx tsx scripts-ts/src/youtrack-cli/get-ticket.ts <ticket-id> [--json]
```
Example:
```bash
npx tsx scripts-ts/src/youtrack-cli/get-ticket.ts VIM-1234
npx tsx scripts-ts/src/youtrack-cli/get-ticket.ts VIM-1234 --json
```

### Set Fix Version
```bash
npx tsx scripts-ts/src/youtrack-cli/set-fix-version.ts <ticket-id> "<version>"
```
Example:
```bash
npx tsx scripts-ts/src/youtrack-cli/set-fix-version.ts VIM-1234 "2.28.0"
```

### Create Release Version
```bash
npx tsx scripts-ts/src/youtrack-cli/create-version.ts "<version-name>"
```
Example:
```bash
npx tsx scripts-ts/src/youtrack-cli/create-version.ts "2.29.0"
```

### Delete Release Version
```bash
npx tsx scripts-ts/src/youtrack-cli/delete-version.ts "<version-name>"
```
Example:
```bash
npx tsx scripts-ts/src/youtrack-cli/delete-version.ts "2.29.0"
```

## Common Tag IDs

| Tag Name | Tag ID |
|----------|--------|
| claude-analyzed | `68-507461` |
| claude-pending-clarification | `68-507582` |
| IdeaVim Released In EAP | `68-385032` |

## Private Comments

Private comments are only visible to the JetBrains team. Use `--private` flag to make a comment private:
```bash
npx tsx scripts-ts/src/youtrack-cli/add-comment.ts VIM-1234 "@Aleksei.Plate Need review" --private
```

## Common Workflows

### Mark ticket as analyzed by Claude
```bash
npx tsx scripts-ts/src/youtrack-cli/add-tag.ts VIM-1234 68-507461
```

### Ask for clarification (add pending tag + private comment)
```bash
npx tsx scripts-ts/src/youtrack-cli/add-tag.ts VIM-1234 68-507582
npx tsx scripts-ts/src/youtrack-cli/add-comment.ts VIM-1234 "@Aleksei.Plate I need clarification: ..." --private
```

### Remove pending clarification tag (after owner responds)
```bash
npx tsx scripts-ts/src/youtrack-cli/remove-tag.ts VIM-1234 68-507582
```

### Mark ticket as ready to release
```bash
npx tsx scripts-ts/src/youtrack-cli/set-status.ts VIM-1234 "Ready To Release"
```
