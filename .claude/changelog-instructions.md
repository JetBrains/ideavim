# Changelog Maintenance Instructions

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
- Note the date of the last entry

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

### 4. What to Include
- **Features**: New functionality with [VIM-XXXX] ticket numbers if available
- **Bug Fixes**: Fixed issues with [VIM-XXXX] ticket references
- **Breaking Changes**: Any backwards-incompatible changes
- **Deprecations**: Features marked for future removal
- **Merged PRs**: Reference significant PRs like "Implement vim-surround (#123)"

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
  - Example: "Fixed `ci"` command in empty strings" or "Added support for `gn` text object"
- **Add helpful links** for context:
  - When mentioning IntelliJ features, search for official JetBrains documentation or blog posts
  - When referencing Vim commands, link to Vim documentation if helpful
  - Example: "Added support for [Next Edit Suggestion](https://blog.jetbrains.com/ai/2025/08/introducing-next-edit-suggestions-in-jetbrains-ai-assistant/)"
  - Use web search to find the most relevant official sources
- **Include references**: Add [VIM-XXXX] for YouTrack tickets, (#XXX) for PRs
- **Group logically**: Features, Fixes, Changes, Merged PRs
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
- With ticket: Description followed by pipe and ticket link:
```
* Feature description | [VIM-XXXX](https://youtrack.jetbrains.com/issue/VIM-XXXX)
```

## Process

1. Read the current CHANGES.md
2. Check previous changelog PRs from GitHub:
   - Review the last few changelog update PRs (use `gh pr list --search "Update changelog" --state all --limit 5`)
   - Look for any comments or instructions about what NOT to log this time
   - Previous PRs may contain specific exclusions or special handling instructions
3. Check git tags for any undocumented releases
4. Review commits and PRs since last entry
5. Group changes by release or under [To Be Released]
6. Update CHANGES.md maintaining existing format
7. Update the `changeNotes` section in `build.gradle.kts`:
   - Copy the content from the `[To Be Released]` section of CHANGES.md
   - Format it as HTML (use `<br>` for line breaks, `<b>` for headers)
   - **IMPORTANT**: Keep any existing information about the reward program in changeNotes
   - This appears in the plugin description on JetBrains Marketplace
8. Create a PR only if there are changes to document

## Important Notes

- **Don't create a PR if changelog is already up to date**
- **Preserve existing format and structure**
- **Maintain chronological order (newest first)**
- **Keep the historical gap note between 2.9.0 and 2.28.0**