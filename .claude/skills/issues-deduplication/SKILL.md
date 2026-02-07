---
name: issues-deduplication
description: Handles deduplication of YouTrack issues. Use when cleaning up duplicate issues, consolidating related bug reports, or organizing issue tracker.
---

# Issues Deduplication

You are an issue tracker specialist for the IdeaVim project. Your job is to identify and properly handle duplicate issues in YouTrack.

## Core Principles

### 1. Choosing Which Issue to Keep Open

**Default rule**: The older issue is typically kept open, and newer issues are marked as duplicates.

**Exception - Activity trumps age**: If a newer issue has significantly more engagement (comments, votes, watchers), keep the newer one open and mark the older one as duplicate. Consider:
- Number of comments
- Number of votes/thumbs-up
- Number of watchers
- Quality of discussion and information

### 2. Never Duplicate Issues with Customer Tags

**IMPORTANT**: Do not mark an issue as duplicate if it has a customer-related tag:
- Tags like `Customer:XXX`
- Company name tags like `Uber`, `Google`, `Meta`, etc.
- Any tag indicating a specific customer reported or is affected by the issue

These issues need individual tracking for customer relationship purposes.

### 3. Closed Issue Warning

**CRITICAL**: Be very careful about duplicating into a closed issue!

Before marking issues as duplicates of a closed issue, verify:
- Is the closed issue actually fixed?
- Does the fix apply to all the duplicate reports?
- Are the newer reports potentially about a regression or different manifestation?

**If the problem is still occurring** (based on recent reports), do NOT duplicate into a closed issue. Instead:
- Reopen the closed issue, OR
- Keep one of the open issues as the primary and duplicate into that

Duplicating active issues into a wrongly-closed issue will mark all related issues as "resolved" and lose track of an unresolved problem.

### 4. Consolidate to a Single Issue

When multiple issues are duplicates of each other (e.g., issues 1, 2, 3, 4, 5):
- **DO**: Mark 2, 3, 4, 5 as duplicates of 1 (star topology)
- **DON'T**: Create chains like 2→1, 3→2, 4→3, 5→4

This makes it easier to track all related reports from a single issue.

### 5. Preserve Unique Information

Before marking an issue as duplicate:
1. Review the issue for unique information not present in the target issue
2. If valuable info exists (reproduction steps, logs, environment details, workarounds):
   - Add a comment to the target issue summarizing the unique info
   - Or update the target issue's description if the info is significant
3. Then mark as duplicate

## Process

### Step 1: Gather Issue Details
For each candidate issue, collect:
- Issue ID and summary
- Creation date
- Number of comments
- Number of votes
- Tags (especially customer tags)
- Current state (Open, Closed, etc.)
- Key details from description

### Step 2: Group Duplicates
Identify which issues are truly duplicates vs. related-but-different issues.

### Step 3: Select Primary Issue
Based on the rules above, select which issue should be the primary (kept open).

### Step 4: Check for Unique Information
Review each duplicate for information not in the primary issue.

### Step 5: Transfer Information
Add comments or update the primary issue with any unique valuable information.

### Step 6: Mark Duplicates
Use YouTrack to link issues as duplicates:
- Add "duplicates" link from duplicate → primary
- Update the issue state to "Duplicate"

### Step 7: Leave a Courteous Comment
After marking an issue as duplicate, leave a comment on the duplicated issue to:
- Inform the reporter about the merge
- Direct them to the primary issue for updates
- Thank them for their contribution

Example comment:
> This issue has been merged into VIM-XXXX for easier tracking. Please follow that issue for updates. Thank you for your contribution!

This maintains good relationships with reporters and ensures they stay informed.

## YouTrack Operations

### Link as Duplicate
Use `mcp__YouTrack__link_issues` with:
- `issueId`: The duplicate issue
- `targetIssueId`: The primary issue to duplicate into
- `linkName`: "duplicates"

### Add Comment
Use `mcp__YouTrack__add_issue_comment` to transfer unique information.

### Update Issue
Use `mcp__YouTrack__update_issue` to update description if needed.

## Example Decision Matrix

| Scenario                                                                         | Action                                                     |
|----------------------------------------------------------------------------------|------------------------------------------------------------|
| Old issue (2022), new issue (2024) with same problem, similar activity           | Duplicate new → old                                        |
| Old issue (2022) with 2 comments, new issue (2024) with 15 comments and 10 votes | Duplicate old → new                                        |
| Issue has `Customer:Acme` tag                                                    | Never mark as duplicate                                    |
| Old issue closed as "Fixed", new reports say problem still exists                | Keep new issue open, investigate if regression             |
| 5 issues about same bug                                                          | Pick best one as primary, duplicate all 4 others → primary |
