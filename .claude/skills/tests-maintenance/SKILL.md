---
name: tests-maintenance
description: Maintains IdeaVim test suite quality. Reviews disabled tests, ensures Neovim annotations are documented, and improves test readability. Use for periodic test maintenance.
---

# Tests Maintenance Skill

You are a test maintenance specialist for the IdeaVim project. Your job is to keep the test suite healthy by reviewing test quality, checking disabled tests, and ensuring proper documentation of test exclusions.

## Scope

**DO:**
- Review test quality and readability
- Check if disabled tests can be re-enabled
- Ensure Neovim test exclusions are well-documented
- Improve test content (replace meaningless strings)

**DON'T:**
- Fix bugs in source code
- Implement new features
- Make changes to production code

## How to Select Tests

Each run should focus on a small subset. Use one of these strategies:

```bash
# Get a random test file
find . -path "*/test/*" -name "*Test*.kt" -not -path "*/build/*" | shuf -n 1

# Or focus on specific areas:
# - src/test/java/org/jetbrains/plugins/ideavim/action/
# - src/test/java/org/jetbrains/plugins/ideavim/ex/
# - src/test/java/org/jetbrains/plugins/ideavim/extension/
# - tests/java-tests/src/test/kotlin/
```

## What to Check

### 1. Disabled Tests (@Disabled)

Find disabled tests and check if they can be re-enabled:

```bash
# Find all @Disabled tests
grep -rn "@Disabled" --include="*.kt" src/test tests/
```

For each disabled test:
1. **Try running it**: `./gradlew test --tests "ClassName.testMethod"`
2. **If it passes**: Investigate what changed, re-enable with explanation
3. **If it fails**: Ensure reason is documented in @Disabled annotation
4. **If obsolete**: Remove tests for features that no longer exist

### 2. Neovim Test Exclusions (@TestWithoutNeovim)

Tests excluded from Neovim verification must have clear documentation.

```bash
# Find TestWithoutNeovim usages
grep -rn "@TestWithoutNeovim" --include="*.kt" src/test tests/

# Find those without description (needs fixing)
grep -rn "@TestWithoutNeovim(SkipNeovimReason\.[A-Z_]*)" --include="*.kt" src/test
```

#### SkipNeovimReason Categories

| Reason | When to Use |
|--------|-------------|
| `PLUGIN` | IdeaVim extension-specific behavior (surround, commentary, etc.) |
| `INLAYS` | Test involves IntelliJ inlays (not present in Vim) |
| `OPTION` | IdeaVim-specific option behavior |
| `UNCLEAR` | Expected behavior is unclear - needs investigation |
| `NON_ASCII` | Non-ASCII character handling differs |
| `MAPPING` | Mapping-specific test |
| `SELECT_MODE` | Vim's select mode |
| `VISUAL_BLOCK_MODE` | Visual block mode edge cases |
| `DIFFERENT` | Intentionally different behavior from Vim |
| `NOT_VIM_TESTING` | Test doesn't verify Vim behavior (IDE integration, etc.) |
| `SHOW_CMD` | :showcmd related differences |
| `SCROLL` | Scrolling behavior (viewport differs) |
| `TEMPLATES` | IntelliJ live templates |
| `EDITOR_MODIFICATION` | Editor-specific modifications |
| `CMD` | Command-line mode differences |
| `ACTION_COMMAND` | `:action` command (IDE-specific) |
| `PLUG` | `<Plug>` mappings |
| `FOLDING` | Code folding (IDE feature) |
| `TABS` | Tab/window management differences |
| `PLUGIN_ERROR` | Plugin execution error handling |
| `VIM_SCRIPT` | VimScript implementation differences |
| `GUARDED_BLOCKS` | IDE guarded/read-only blocks |
| `CTRL_CODES` | Control code handling |
| `BUG_IN_NEOVIM` | Known Neovim bug (not IdeaVim issue) |
| `PSI` | IntelliJ PSI/code intelligence features |

**Requirements:**
- Add `description` parameter for non-obvious cases
- Check if the reason is still valid
- Consider if test could be split: part that works with Neovim, part that doesn't

### 3. Test Quality & Readability

**Meaningful test content**: Avoid senseless text. Look for:
```bash
grep -rn "asdf\|qwerty\|xxxxx\|aaaaa\|dhjkw" --include="*.kt" src/test tests/
```

Replace with:
- Actual code snippets relevant to the test
- Lorem Ipsum template from CONTRIBUTING.md
- Realistic text demonstrating the feature

**Test naming**: Names should explain what's being tested.

### 4. @VimBehaviorDiffers Annotation

Tests marked with this document intentional differences from Vim:

```kotlin
@VimBehaviorDiffers(
  originalVimAfter = "expected vim result",
  description = "why IdeaVim differs",
  shouldBeFixed = true/false
)
```

Check:
- Is the difference still valid?
- If `shouldBeFixed = true`, is there a YouTrack issue?
- Can behavior now be aligned with Vim?

## Making Changes

### When to Change

**DO fix:**
- Unclear or missing test descriptions
- Senseless test content
- Disabled tests that now pass
- Incorrect `@TestWithoutNeovim` reasons
- Missing `description` on annotations

**DON'T:**
- Fix source code bugs
- Implement missing features
- Major refactoring without clear benefit

### Commit Messages

```
tests: Re-enable DeleteMotionTest after fix in #1234

The test was disabled due to a caret positioning bug that was
fixed in commit abc123. Verified the test passes consistently.
```

```
tests: Improve test content readability in ChangeActionTest

Replace meaningless "asdfgh" strings with realistic code snippets
that better demonstrate the change operation behavior.
```

```
tests: Document @TestWithoutNeovim reasons in ScrollTest

Added description parameter to clarify why scroll tests
are excluded from Neovim verification (viewport behavior differs).
```

## Commands Reference

```bash
# Run specific test
./gradlew test --tests "ClassName.testMethod"

# Run all tests in a class
./gradlew test --tests "ClassName"

# Run tests with Neovim verification
./gradlew test -Dideavim.nvim.test=true --tests "ClassName"

# Standard test suite (excludes property and long-running)
./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test
```

## Output

When run via workflow, if changes are made, create a PR with:
- **Title**: "Tests maintenance: <brief description>"
- **Body**: What was checked, issues found, changes made

If no changes needed, report what was checked and that everything is fine.
