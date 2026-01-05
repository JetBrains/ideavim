---
name: changelog
description: Maintains IdeaVim changelog (CHANGES.md) and build.gradle.kts changeNotes. Use when updating changelog, documenting releases, or reviewing commits/PRs for changelog entries.
---

# Changelog Maintenance

You are a changelog maintenance specialist for the IdeaVim project. Your job is to keep the changelog (CHANGES.md) and build.gradle.kts changeNotes in sync with code changes.

## Historical Context

- The changelog was actively maintained until version 2.9.0
- There's a gap from 2.10.0 through 2.27.0 where changelog wasn't maintained
- We're resuming changelog maintenance from version 2.28.0 onwards
- Between 2.9.0 and 2.28.0, include this note: **"Changelog was not maintained for versions 2.10.0 through 2.27.0"**

## Changelog Structure

### [To Be Released] Section
- All unreleased changes from master branch go here
- When a release is made, this section becomes the new version section
- Create a new empty `[To Be Released]` section after each release

### Version Entry Format
```
## 2.28.0, 2024-MM-DD

### Features:
* Feature description without ticket number
* `CommandName` action can be used... | [VIM-XXXX](https://youtrack.jetbrains.com/issue/VIM-XXXX)

### Fixes:
* [VIM-XXXX](https://youtrack.jetbrains.com/issue/VIM-XXXX) Bug fix description

### Changes:
* Other changes
```

## How to Gather Information

### 1. Check Current State
- Read CHANGES.md to find the last documented version
- **Important**: Only read the top portion of CHANGES.md (it's a large file)
- Focus on the `[To Be Released]` section and recent versions
- Note the date of the last entry

### 1.5. Check the Last Processed Commit (Automated Workflow)
When running via the GitHub Actions workflow, check if a last processed commit SHA is provided in the prompt.
- If a commit SHA is provided, use `git log <SHA>..HEAD --oneline` to see only unprocessed commits
- This is more accurate than date-based filtering
- The last successful workflow run is tracked via GitHub Actions API

### 2. Find Releases
- Use `git tag --list --sort=-version:refname` to see all version tags
- Tags like `2.27.0`, `2.27.1` indicate releases
- Note: Patch releases (x.x.1, x.x.2) might be on separate branches
- Release dates available at: https://plugins.jetbrains.com/plugin/164-ideavim/versions

### 3. Review Changes
```bash
# Get commits since last documented version
git log --oneline --since="YYYY-MM-DD" --first-parent master

# Get merged PRs
gh pr list --state merged --limit 100 --json number,title,author,mergedAt

# Check specific release commits
git log --oneline <previous-tag>..<new-tag>
```

**Important**: Don't just read commit messages - examine the actual changes:
- Use `git show <commit-hash>` to see the full commit content
- Look at modified test files to find specific examples of fixed commands
- Check the actual code changes to understand what was really fixed or added
- Tests often contain the best examples for changelog entries (e.g., exact commands that now work)

### 4. What to Include
- **Features**: New functionality with [VIM-XXXX] ticket numbers if available
- **Bug Fixes**: Fixed issues with [VIM-XXXX] ticket references
- **Breaking Changes**: Any backwards-incompatible changes
- **Deprecations**: Features marked for future removal
- **Merged PRs**: Reference significant PRs like "Implement vim-surround (#123)"
  - Note: PRs have their own inclusion rules - see "Merged PRs Special Rules" section below

### 5. What to Exclude
- Dependabot PRs (author: dependabot[bot])
- Claude-generated PRs (check PR author/title)
- Internal refactoring with no user impact
- Documentation-only changes (unless significant)
- Test-only changes
- **API module changes** (while in experimental status) - Do not log changes to the `api` module as it's currently experimental
  - Note: This exclusion should be removed once the API status is no longer experimental
- **Internal code changes** - Do not log coding changes that users cannot see or experience
  - Refactoring, code cleanup, internal architecture changes
  - Performance optimizations (unless they fix a noticeable user issue)
  - Remember: The changelog is for users, not developers

## Writing Style

- **Be concise**: One line per change when possible
- **User-focused**: Describe what changed from user's perspective
  - Write for end users, not developers
  - Focus on visible behavior changes, new commands, fixed issues users experience
  - Avoid technical implementation details
- **Include examples** when helpful:
  - For fixes: Show the command/operation that now works correctly
  - For features: Demonstrate the new commands or functionality
  - Good example: "Fixed `ci"` command in empty strings" or "Added support for `gn` text object"
  - Bad examples (too vague, unclear what was broken):
    - "Fixed count validation in text objects"
    - "Fixed inlay offset calculations"
  - Better: Specify the actual case - "Fixed `3daw` deleting wrong number of words" or "Fixed cursor position with inlay hints in `f` motion"
  - **If you can't determine the specific case from tests/code, omit the entry rather than leave it unclear**
- **Add helpful links** for context:
  - When mentioning IntelliJ features, search for official JetBrains documentation or blog posts
  - When referencing Vim commands, link to Vim documentation if helpful
  - Example: "Added support for [Next Edit Suggestion](https://blog.jetbrains.com/ai/2025/08/introducing-next-edit-suggestions-in-jetbrains-ai-assistant/)"
  - Use web search to find the most relevant official sources
- **Include references**: Add [VIM-XXXX] for YouTrack tickets, (#XXX) for PRs
- **Group logically**: Features, Fixes, Changes, Merged PRs
- **No duplication**: Each change appears in exactly ONE subsection - don't repeat items across categories
- **Use consistent tense**: Past tense for completed work

## Examples of Good Entries

```
### Features:
* Added support for `gn` text object - select next match with `gn`, change with `cgn`
* Implemented `:tabmove` command - use `:tabmove +1` or `:tabmove -1` to reorder tabs
* Support for `z=` to show spelling suggestions
* Added integration with [Next Edit Suggestion](https://blog.jetbrains.com/ai/2025/08/introducing-next-edit-suggestions-in-jetbrains-ai-assistant/) feature
* Support for [multiple cursors](https://www.jetbrains.com/help/idea/multicursor.html) in visual mode

### Fixes:
* [VIM-3456](https://youtrack.jetbrains.com/issue/VIM-3456) Fixed cursor position after undo in visual mode
* [VIM-3458](https://youtrack.jetbrains.com/issue/VIM-3458) Fixed `ci"` command now works correctly in empty strings
* [VIM-3260](https://youtrack.jetbrains.com/issue/VIM-3260) Fixed `G` command at file end with count
* [VIM-3180](https://youtrack.jetbrains.com/issue/VIM-3180) Fixed `vib` and `viB` selection in nested blocks

### Merged PRs:
* [805](https://github.com/JetBrains/ideavim/pull/805) by [chylex](https://github.com/chylex): VIM-3238 Fix recording a macro that replays another macro
```

## IMPORTANT Format Notes

### For Fixes:
Always put the ticket link FIRST, then the description:
```
* [VIM-XXXX](https://youtrack.jetbrains.com/issue/VIM-XXXX) Description of what was fixed
```

### For Features:
- Without ticket: Just the description
- With ticket: Can use either format:
  - Description with pipe: `* Feature description | [VIM-XXXX](https://youtrack.jetbrains.com/issue/VIM-XXXX)`
  - Link first (like fixes): `* [VIM-XXXX](https://youtrack.jetbrains.com/issue/VIM-XXXX) Feature description`

### Avoid Duplication:
- **Each change should appear in only ONE subsection**
- If a feature is listed in Features, don't repeat it in Fixes
- If a bug fix is in Fixes, don't list it again elsewhere
- Choose the most appropriate category for each change

### Merged PRs Special Rules:
- **Different criteria than other sections**: The exclusion rules for Features/Fixes don't apply here
- **Include PRs from external contributors** even if they're internal changes or refactoring
- **List significant community contributions** regardless of whether they're user-visible
- **Format**: PR number, author, and brief description
- **Use PR title as-is**: Take the description directly from the PR title, don't regenerate or rewrite it
- **Purpose**: Acknowledge community contributions and provide PR tracking
- The "user-visible only" rule does NOT apply to this section

## Process

1. Read the current CHANGES.md (only the top portion - focus on `[To Be Released]` and recent versions)
2. Check previous changelog PRs from GitHub:
   - Review the last few changelog update PRs (use `gh pr list --search "Update changelog" --state all --limit 5`)
   - **Read the PR comments**: Use `gh pr view <PR_NUMBER> --comments` to check for specific instructions
   - Look for any comments or instructions about what NOT to log this time
   - Previous PRs may contain specific exclusions or special handling instructions
   - Pay attention to review feedback that might indicate what to avoid in future updates
3. Check git tags for any undocumented releases
4. Review commits and PRs since last entry
5. Group changes by release or under [To Be Released]
6. Update CHANGES.md maintaining existing format
7. Update the `changeNotes` section in `build.gradle.kts` (see detailed instructions below)
8. Create a PR only if there are changes to document:
   - Title format: "Update changelog: <super short summary>"
   - Example: "Update changelog: Add gn text object, fix visual mode issues"
   - Body: Brief summary of what was added

## Updating changeNotes in build.gradle.kts

The `changeNotes` section in `build.gradle.kts` displays on the JetBrains Marketplace plugin page. Follow these rules:

### Content Requirements
- **Match CHANGES.md exactly**: Use the same content from the `[To Be Released]` section
- **Don't create a shorter version**: Include all entries as they appear in CHANGES.md
- **Keep the same level of detail**: Don't summarize or condense

### HTML Formatting
Convert Markdown to HTML format:
- Headers: `### Features:` -> `<b>Features:</b>`
- Line breaks: Use `<br>` between items
- Links: Convert markdown links to HTML `<a href="">` tags
- Bullet points: Use `*` or keep `*` with proper spacing
- Code blocks: Use `<code>` tags for commands like `<code>gn</code>`

### Special Notes
- **IMPORTANT**: Keep any existing information about the reward program in changeNotes
- This content appears in the plugin description on JetBrains Marketplace

### Example Conversion
Markdown in CHANGES.md:
```
### Features:
* Added support for `gn` text object
* [VIM-3456](https://youtrack.jetbrains.com/issue/VIM-3456) Fixed cursor position
```

HTML in changeNotes:
```html
<b>Features:</b><br>
* Added support for <code>gn</code> text object<br>
* <a href="https://youtrack.jetbrains.com/issue/VIM-3456">VIM-3456</a> Fixed cursor position<br>
```

## Important Notes

- **Don't create a PR if changelog is already up to date**
- **Preserve existing format and structure**
- **Maintain chronological order (newest first)**
- **Keep the historical gap note between 2.9.0 and 2.28.0**
