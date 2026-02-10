---
name: git-workflow
description: IdeaVim git workflow conventions covering commits, branches, PRs, and CI. Use when creating commits, managing branches, creating pull requests, reviewing git history, or any git-related activity in the IdeaVim project.
---

# Git Workflow

## Branching

- **Master** is the trunk and MUST always be in a "ready to release" state
- Use **feature branches** for development work
  - Naming: `VIM-XXXX/short-description` (e.g., `VIM-3948/editor`)
  - Rebase to master frequently to avoid large conflicts
- Small, isolated changes (bug fixes, minor tweaks) MAY go directly to master
- Unfinished changes MAY be committed to master only if they do NOT break functionality
- Use **rebase** for integration, not merge commits (linear history)

## Commits

**Standard format:**
```
VIM-XXXX Description of the change
```

- Start with the YouTrack ticket ID when the change relates to a ticket
- Example: `VIM-3948 Traverse vertical panes in ConfigurableEditor`

**Auto-closing format** (moves YouTrack ticket to "Ready To Release"):
```
fix(VIM-XXXX): Description of the fix
```

**Content rules:**
- Each commit MUST contain a single, focused, meaningful change
- MUST NOT include unrelated changes (formatting, unrelated refactoring)
- Include appropriate tests with behavioral changes

## Pull Requests

- PRs target `master`
- CI runs standard tests automatically (`./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test`)
- PRs from external contributors are listed in the changelog under "Merged PRs"

## Issue Tracking

- Use **YouTrack** (not GitHub Issues) - tickets are `VIM-XXXX`
- URL: https://youtrack.jetbrains.com/issues/VIM
