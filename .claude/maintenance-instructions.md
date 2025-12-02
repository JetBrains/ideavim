# Codebase Maintenance Instructions

## Goal

Perform routine maintenance on random parts of the IdeaVim codebase to ensure code quality, consistency, and catch potential issues early. This is not about being overly pedantic or making changes for the sake of changes - it's about keeping an eye on the codebase and identifying genuine issues.

## Approach

### 1. Select Random Area

Choose a random part of the codebase to inspect. Use one of these strategies:

```bash
# Get a random Kotlin file
find . -name "*.kt" -not -path "*/build/*" -not -path "*/.gradle/*" | shuf -n 1

# Get a random package/directory
find . -type d -name "*.kt" -not -path "*/build/*" | shuf -n 1 | xargs dirname

# Pick from core areas randomly
# - vim-engine/src/main/kotlin/com/maddyhome/idea/vim/
# - src/main/java/com/maddyhome/idea/vim/
# - tests/
```

**Important**: You're not limited to the file you randomly selected. If investigating reveals related files that need attention, follow the trail. The random selection is just a starting point.

## 2. What to Check

### Code Style & Formatting
- **Kotlin conventions**: Proper use of data classes, sealed classes, when expressions
- **Naming consistency**: Follow existing patterns in the codebase
- **Import organization**: Remove unused imports, prefer explicit imports over wildcards (wildcard imports are generally not welcome)
- **Code structure**: Proper indentation, spacing, line breaks
- **Documentation**: KDoc comments where needed (public APIs, complex logic)
- **Copyright years**: Do NOT update copyright years unless you're making substantive changes to the file. It's perfectly fine for copyright to show an older year. Don't mention copyright year updates in commit messages or change summaries

### Code Quality Issues
- **Null safety**: Proper use of nullable types, safe calls, Elvis operator
- **Error handling**: Appropriate exception handling, meaningful error messages
- **Code duplication**: Identify repeated code that could be extracted
- **Dead code**: Unused functions, parameters, variables
- **TODOs/FIXMEs**: Check if old TODOs are still relevant or can be addressed
- **Magic numbers/strings**: Should be named constants
- **Complex conditionals**: Can they be simplified or extracted?

### Potential Bugs
- **Off-by-one errors**: Especially in loops and range operations
- **Edge cases**: Empty collections, null values, boundary conditions
- **Type safety**: Unnecessary casts, unchecked casts
- **Resource handling**: Proper cleanup, try-with-resources
- **Concurrency issues**: Thread safety if applicable
- **State management**: Proper initialization, mutation patterns
- **IdeaVim enablement checks**: Verify that `injector.enabler.isEnabled()` or `Editor.isIdeaVimDisabledHere` are not missed in places where they should be checked. These functions determine if IdeaVim is active and should be called before performing Vim-specific operations

### Architecture & Design
- **Separation of concerns**: Does the code have a single responsibility?
- **Dependency direction**: Are dependencies pointing the right way?
- **Abstraction level**: Consistent level of abstraction within methods
- **Vim architecture alignment**: Does it match Vim's design philosophy?
- **IntelliJ Platform conventions**: Proper use of platform APIs

### Testing
- **Test coverage**: Are there tests for the code you're reviewing?
  - If checking a specific command or function, verify that tests exist for it
  - If tests exist, check if they cover the needed cases (edge cases, error conditions, typical usage)
  - If tests don't exist or coverage is incomplete, consider creating comprehensive test coverage
- **Test quality**: Do tests cover edge cases?
- **Test naming**: Clear, descriptive test names
- **Flaky tests**: Any potentially unstable tests?
- **Regression tests for bug fixes**: When fixing a bug, always write a test that:
  - Would fail with the old (buggy) implementation
  - Passes with the fixed implementation
  - Clearly documents what bug it's testing (include comments explaining the issue)
  - Tests the specific boundary condition or edge case that exposed the bug
  - This ensures the bug doesn't resurface in future refactorings

## 3. Investigation Strategy

Don't just look at surface-level issues. Dig deeper:

1. **Read the code**: Understand what it does before suggesting changes
2. **Check related files**: Look at callers, implementations, tests
3. **Look at git history**: `git log --oneline <file>` to understand context
4. **Find related issues**: Search for TODOs, FIXMEs, or commented code
5. **Run tests**: If you make changes, ensure tests pass
6. **Check YouTrack**: Look for related issues if you find bugs

## 4. When to Make Changes

**DO fix**:
- Clear bugs or logic errors
- Obvious code quality issues (unused imports, etc.)
- Misleading or incorrect documentation
- Code that violates established patterns
- Security vulnerabilities
- Performance issues with measurable impact

**DON'T fix**:
- Stylistic preferences if existing code is consistent
- Working code just to use "newer" patterns
- Minor formatting if it's consistent with surrounding code
- Things that are subjective or arguable
- Massive refactorings without clear benefit

**When in doubt**: Document the issue in your report but don't make changes.

## 5. Making Changes

If you decide to make changes:

1. **Make focused commits**: One logical change per commit
   - If the change affects many files or is complicated or has multiple logical changes, split it into multiple step-by-step commits
   - This makes it easier for reviewers to understand the changes
   - Example: First commit renames a function, second commit updates callers, third commit adds new functionality
   - This rule is important!
2. **Write clear commit messages**: Explain why, not just what
3. **Run tests**: `./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test`

## 6. Examples

### Good Maintenance Examples

**Example 1: Found and fixed null safety issue**
```
Inspected: vim-engine/.../motion/VimMotionHandler.kt

Issues found:
- Several nullable properties accessed without safe checks
- Could cause NPE in edge cases with cursor at document end

Changes:
- Added null checks with Elvis operator
- Added early returns for invalid state
- Added KDoc explaining preconditions
```

**Example 2: No changes needed**
```
Inspected: src/.../action/change/ChangeLineAction.kt

Checked:
- Code style and formatting ✓
- Null safety ✓
- Error handling ✓
- Tests present and comprehensive ✓

Observations:
- Code is well-structured and follows conventions
- Good test coverage including edge cases
- Documentation is clear
- No issues found
```

**Example 3: Found issues but didn't fix**
```
Inspected: tests/.../motion/MotionTests.kt

Issues noted:
- Some test names could be more descriptive
- Potential for extracting common setup code
- Tests are comprehensive but could add edge case for empty file

Recommendation: These are minor quality-of-life improvements.
Not critical, but could be addressed in future cleanup.
```

## IdeaVim-Specific Considerations

- **Vim compatibility**: Changes should maintain compatibility with Vim behavior
- **IntelliJ Platform**: Follow IntelliJ platform conventions and APIs
- **Property tests**: Can be flaky - verify if test failures relate to your changes
- **Action syntax**: Use `<Action>` in mappings, not `:action`
- **Architecture & Guidelines**: Refer to [CONTRIBUTING.md](../CONTRIBUTING.md) for:
  - Architecture overview and where to find specific code
  - Testing guidelines and corner cases to consider
  - Common patterns and conventions
  - Information about awards for quality contributions

## Commands Reference

```bash
# Run tests (standard suite)
./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test

# Run specific test class
./gradlew test --tests "ClassName"

# Check code style
./gradlew ktlintCheck

# Format code
./gradlew ktlintFormat

# Run IdeaVim in dev instance
./gradlew runIde
```

## Final Notes

- **Be thorough but practical**: Don't waste time on nitpicks
- **Context matters**: Understand why code is the way it is before changing
- **Quality over quantity**: One good fix is better than ten trivial changes
- **Document your process**: Help future maintainers understand your thinking
- **Learn from the code**: Use this as an opportunity to understand the codebase better

Remember: The goal is to keep the codebase healthy, not to achieve perfection. Focus on genuine improvements that make the code safer, clearer, or more maintainable.
