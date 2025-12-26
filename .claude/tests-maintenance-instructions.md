# Tests Maintenance Instructions

## Goal

Perform routine maintenance on the IdeaVim test suite to ensure test quality, accuracy, and proper documentation. This workflow focuses exclusively on tests - not on fixing bugs or implementing features.

## Approach

### 1. Select Random Test Area

Choose a random test file or package to inspect. Use one of these strategies:

```bash
# Get a random test file
find . -path "*/test/*" -name "*Test*.kt" -not -path "*/build/*" | shuf -n 1

# Get a random test from specific areas
find ./src/test -name "*.kt" | shuf -n 1
find ./tests/java-tests -name "*.kt" | shuf -n 1

# Or pick from core test areas randomly:
# - src/test/java/org/jetbrains/plugins/ideavim/action/
# - src/test/java/org/jetbrains/plugins/ideavim/ex/
# - src/test/java/org/jetbrains/plugins/ideavim/extension/
# - tests/java-tests/src/test/kotlin/
```

**Important**: Focus on one file or a small set of related files per run. Don't try to review all tests at once.

## 2. What to Check

### Test Quality & Readability

- **Meaningful test content**: Avoid senseless text like "dhjkwaldjwa" or "asdasdasd". Prefer:
  - Actual code snippets relevant to the test
  - The Lorem Ipsum template from CONTRIBUTING.md
  - Realistic text that demonstrates the feature being tested
- **Test naming**: Clear, descriptive names that explain what's being tested
- **Test structure**: Proper setup, action, and assertion phases
- **Comments**: Add comments for complex test scenarios explaining the intent

### Disabled Tests (@Disabled)

Check if disabled tests can be re-enabled:

```bash
# Find all @Disabled tests
grep -rn "@Disabled" --include="*.kt" src/test tests/
```

For each disabled test:
1. **Try running it**: Does it pass now?
2. **If it passes**: Investigate what changed (git log, related commits)
3. **If it fails**: Is the reason documented? Update the @Disabled annotation with a clear explanation
4. **If obsolete**: Remove tests for features that no longer exist

### Neovim Testing Annotations

Tests can be excluded from Neovim verification using `@TestWithoutNeovim(reason, description)`.

#### SkipNeovimReason Categories

Review tests with `@TestWithoutNeovim` to ensure reasons are accurate and well-documented:

| Reason | When to Use |
|--------|-------------|
| `PLUGIN` | IdeaVim extension/plugin-specific behavior (surround, commentary, etc.) |
| `INLAYS` | Test involves IntelliJ inlays (not present in Vim) |
| `OPTION` | IdeaVim-specific option behavior |
| `UNCLEAR` | Expected behavior is unclear - needs investigation |
| `NON_ASCII` | Non-ASCII character handling differs |
| `MAPPING` | Mapping-specific test |
| `SELECT_MODE` | Vim's select mode (not commonly used) |
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

**When reviewing**:
- Ensure `description` parameter is used for non-obvious cases
- Check if the reason is still valid - behavior may have been fixed
- Consider if a test could be split: part that works with Neovim, part that doesn't

### @VimBehaviorDiffers Annotation

Tests marked with `@VimBehaviorDiffers` document intentional differences:

```kotlin
@VimBehaviorDiffers(
  originalVimAfter = "expected vim result",
  description = "why IdeaVim differs",
  shouldBeFixed = true/false
)
```

**Check**:
- Is the difference still valid?
- If `shouldBeFixed = true`, is there a related YouTrack issue?
- Can the behavior now be aligned with Vim?

## 3. Investigation Strategy

1. **Run the test**: `./gradlew test --tests "ClassName.testMethod"`
2. **Check git history**: `git log --oneline <test-file>` for context
3. **Find related source code**: What does this test actually exercise?
4. **Check YouTrack**: Are there related issues for failing/disabled tests?

## 4. When to Make Changes

**DO fix**:
- Unclear or missing test descriptions
- Senseless test content (replace with meaningful text)
- Disabled tests that now pass (re-enable with explanation)
- Incorrect or outdated `@TestWithoutNeovim` reasons
- Missing `description` on `@TestWithoutNeovim` annotations

**DON'T do**:
- Fix bugs in the source code (not the test's job)
- Implement missing features
- Major test refactoring without clear benefit
- Change test logic unless it's clearly incorrect

## 5. Making Changes

If you decide to make changes:

1. **Make focused commits**: One logical change per commit
2. **Write clear commit messages**: Explain the maintenance action
3. **Run affected tests**: Verify changes don't break anything
4. **Create a PR**: For any non-trivial changes

### Commit Message Examples

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

## 6. Specific Checks

### Finding Disabled Tests

```bash
# All @Disabled tests
grep -rn "@Disabled" --include="*.kt" src/test tests/

# Disabled with reason
grep -rn '@Disabled("' --include="*.kt" src/test tests/

# Disabled without reason (needs documentation)
grep -rn "@Disabled$" --include="*.kt" src/test tests/
```

### Finding Neovim Exclusions

```bash
# All TestWithoutNeovim usages
grep -rn "@TestWithoutNeovim" --include="*.kt" src/test tests/

# Find those without description
grep -rn "@TestWithoutNeovim(SkipNeovimReason\.[A-Z_]*)" --include="*.kt" src/test

# Find by specific reason
grep -rn "SkipNeovimReason.UNCLEAR" --include="*.kt" src/test
```

### Finding Poor Test Content

```bash
# Look for potentially meaningless test content
grep -rn "asdf\|qwerty\|xxxxx\|aaaaa" --include="*.kt" src/test tests/
```

## 7. Examples

### Good Maintenance Examples

**Example 1: Re-enabled a disabled test**
```
Inspected: src/test/.../DeleteMotionTest.kt

Found: @Disabled test `testDeleteWithCount`
Action: Ran the test - it passes now
Investigation: Related fix was merged in commit abc123
Change: Removed @Disabled, added comment referencing the fix
```

**Example 2: Improved Neovim annotation**
```
Inspected: src/test/.../ScrollActionTest.kt

Found: @TestWithoutNeovim(SkipNeovimReason.SCROLL) without description
Action: Added description explaining viewport behavior difference
```

**Example 3: Improved test readability**
```
Inspected: src/test/.../ChangeOperatorTest.kt

Found: Test uses "abc123xyz" as content
Action: Replaced with realistic code snippet:
  fun calculate() {
      return x + y
  }
```

**Example 4: No changes needed**
```
Inspected: src/test/.../MotionRightTest.kt

Checked:
- Test content is meaningful (uses realistic code) ✓
- Test names are descriptive ✓
- Neovim annotations have proper descriptions ✓
- No disabled tests ✓

No issues found.
```

## Commands Reference

```bash
# Run specific test
./gradlew test --tests "ClassName.testMethod"

# Run all tests in a class
./gradlew test --tests "ClassName"

# Run tests with Neovim verification
./gradlew test -Dideavim.nvim.test=true --tests "ClassName"

# Run standard test suite (excludes property and long-running)
./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test
```

## Final Notes

- **One file at a time**: Focus on quality over quantity
- **Document changes**: Explain why tests were modified
- **Preserve intent**: Don't change what a test verifies, only how it's written
- **Be conservative**: When in doubt, document the issue but don't change
- **Create PRs**: For any changes that need review
