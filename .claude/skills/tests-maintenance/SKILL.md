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

## Change Granularity (Important for CI/GitHub Actions)

**One logical change per run.** This ensures granular, reviewable Pull Requests.

**Rules:**
1. **One test per run**: Focus on a single test file or test method
2. **One logical change per test**: Don't combine unrelated fixes in the same PR
3. **Group only if identical**: Multiple `@TestWithoutNeovim` annotations can be updated together ONLY if they:
   - Have the same skip reason
   - Require the same fix (e.g., all need the same description added)
   - Are part of the same logical issue

**Examples:**

✅ **Good** (pick ONE of these per PR):
- Update one `DIFFERENT` → `IDEAVIM_API_USED` with description
- Add descriptions to 3 tests that all use `SCROLL` reason (same fix pattern)
- Re-enable one `@Disabled` test that now passes

❌ **Bad** (too many changes):
- Update `DIFFERENT` to `SCROLL` in one test AND `PLUGIN` in another (different reasons)
- Fix test content AND update annotations in the same PR
- Re-enable multiple unrelated disabled tests

**Why this matters:**
- Each PR can be reviewed independently
- Easy to revert if something breaks
- Clear git history of what changed and why

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
| `SEE_DESCRIPTION` | Case-specific difference that doesn't fit other categories (description required) |
| `PLUGIN` | IdeaVim extension-specific behavior (surround, commentary, etc.) |
| `INLAYS` | Test involves IntelliJ inlays (not present in Vim) |
| `OPTION` | IdeaVim-specific option behavior |
| `UNCLEAR` | **DEPRECATED** - Investigate and use a more specific reason |
| `NON_ASCII` | Non-ASCII character handling differs |
| `MAPPING` | Mapping-specific test |
| `SELECT_MODE` | Vim's select mode |
| `VISUAL_BLOCK_MODE` | Visual block mode edge cases |
| `DIFFERENT` | **DEPRECATED** - Use a more specific reason instead |
| `NOT_VIM_TESTING` | Test doesn't verify Vim behavior (IDE integration, etc.) |
| `SHOW_CMD` | :showcmd related differences |
| `SCROLL` | Scrolling behavior (viewport differs) |
| `TEMPLATES` | IntelliJ live templates |
| `EDITOR_MODIFICATION` | Editor-specific modifications |
| `CMD` | Command-line mode differences |
| `ACTION_COMMAND` | `:action` command (IDE-specific) |
| `FOLDING` | Code folding (IDE feature) |
| `TABS` | Tab/window management differences |
| `PLUGIN_ERROR` | Plugin execution error handling |
| `VIM_SCRIPT` | VimScript implementation differences |
| `GUARDED_BLOCKS` | IDE guarded/read-only blocks |
| `CTRL_CODES` | Control code handling |
| `BUG_IN_NEOVIM` | Known Neovim bug (not IdeaVim issue) |
| `PSI` | IntelliJ PSI/code intelligence features |
| `IDEAVIM_API_USED` | Test uses IdeaVim API that prevents Neovim state sync |
| `IDEAVIM_WORKS_INTENTIONALLY_DIFFERENT` | IdeaVim intentionally deviates from Neovim for better UX or IntelliJ integration |
| `INTELLIJ_PLATFORM_INHERITED_DIFFERENCE` | Behavior difference inherited from IntelliJ Platform constraints |

**Requirements:**
- Add `description` parameter for non-obvious cases
- Check if the reason is still valid
- Consider if test could be split: part that works with Neovim, part that doesn't

**Special requirement for `IDEAVIM_WORKS_INTENTIONALLY_DIFFERENT`:**
- **ONLY use when you find clear evidence** of intentional deviation:
  - Explicit commit messages explaining the intentional difference
  - Code comments documenting why IdeaVim deviates from Vim/Neovim
  - Absolutely obvious cases (e.g., IntelliJ-specific features not in Neovim)
- **DO NOT use based on guesswork or assumptions**
- If uncertain, use `DIFFERENT` or `UNCLEAR` instead and investigate git history/comments
- The `description` parameter is **mandatory** and must explain what exactly differs and why

**Special requirement for `INTELLIJ_PLATFORM_INHERITED_DIFFERENCE`:**
- Use when behavior difference is due to IntelliJ Platform's underlying implementation
- Common cases include:
  - Empty buffer handling (Platform editors can be empty, Neovim buffers always have a newline)
  - Position/offset calculations for newline characters
  - Line/column indexing differences
- The `description` parameter is **mandatory** and must explain:
  - What Platform behavior causes the difference
  - How it manifests in the test
- Evidence can be found in Platform API documentation, IdeaVim code comments, or obvious Platform limitations

**Special requirement for `SEE_DESCRIPTION`:**
- Use as a last resort when the difference doesn't fit any standard category
- The `description` parameter is **mandatory** and must provide a clear, specific explanation
- Use sparingly - if multiple tests share similar reasons, consider creating a new dedicated reason
- Always check existing reasons first before using this catch-all

**Handling `DIFFERENT` and `UNCLEAR` (DEPRECATED):**

Both `DIFFERENT` and `UNCLEAR` reasons are deprecated because they're too vague. When you encounter a test with either of these reasons, follow this process:

1. **First, try removing the annotation and running with Neovim:**
   ```bash
   # Comment out or remove @TestWithoutNeovim, then run:
   ./gradlew test -Dnvim --tests "ClassName.testMethodName"
   ```

   **IMPORTANT:** Verify the output contains `NEOVIM TESTING ENABLED` to confirm Neovim testing is active.
   If this message is not present, the test ran without Neovim verification.

2. **If the test passes with Neovim:**
   - The annotation is outdated and should be removed
   - IdeaVim and Neovim now behave identically for this case

3. **If the test fails with Neovim:**
   - Analyze the failure to understand WHY the behavior differs
   - Replace `DIFFERENT` with a more specific reason:
     - `IDEAVIM_API_USED` - if test uses VimPlugin.* or injector.* APIs directly
     - `IDEAVIM_WORKS_INTENTIONALLY_DIFFERENT` - if IdeaVim intentionally deviates (need evidence)
     - `INTELLIJ_PLATFORM_INHERITED_DIFFERENCE` - if difference comes from Platform constraints
     - `SEE_DESCRIPTION` - for unique cases that don't fit other categories (description required)
     - Or another appropriate reason from the table above
   - Always add a `description` parameter explaining the specific difference

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

# Run tests with Neovim verification (look for "NEOVIM TESTING ENABLED" in output)
./gradlew test -Dnvim --tests "ClassName"

# Standard test suite (excludes property and long-running)
./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test
```

## Output

When run via workflow, if changes are made, create a PR with:
- **Title**: "Tests maintenance: <brief description>"
- **Body**: What was checked, issues found, changes made

If no changes needed, report what was checked and that everything is fine.
