<!--
  SYNC IMPACT REPORT
  ==================
  Version change: 0.0.0 → 1.0.0

  Added sections:
  - Core Principles (4 principles)
  - Development Standards
  - Testing Requirements
  - Governance

  Templates requiring updates:
  - .specify/templates/plan-template.md ✅ (Constitution Check section compatible)
  - .specify/templates/spec-template.md ✅ (Requirements section compatible)
  - .specify/templates/tasks-template.md ✅ (Test-first pattern compatible)

  Follow-up TODOs: None
-->

# IdeaVim Constitution

## Core Principles

### I. Vim Compatibility (IDE-First)

IdeaVim MUST match Vim functionality where feasible for JetBrains IDEs, while preserving IDE integrity.

**Priority Order** (when behaviors conflict):

1. JetBrains IDE behavior takes precedence over Vim behavior
2. Vim behavior is implemented where it does not conflict with the IDE

**Core Philosophy**: IdeaVim exists to **extend** IDE functionality with Vim capabilities, not to **change** IDE
behavior. Modifications to default IDE behavior are permitted but discouraged; when necessary, they MUST be explicitly
documented and ideally configurable.

**Implementation Rules**:

- Commands MUST behave like Vim unless it conflicts with IDE behavior or is technically infeasible
- Motions MUST follow Vim's inclusive/exclusive/linewise semantics
- Key mappings MUST support standard Vim syntax

**Rationale**: IdeaVim is a plugin for JetBrains IDEs. Users expect seamless IDE integration first, with Vim
capabilities layered on top. Breaking IDE behavior undermines the primary product experience.

### II. IntelliJ Platform Integration

IdeaVim MUST integrate seamlessly with IntelliJ Platform conventions and APIs.

- Code MUST be written in Kotlin unless working in existing Java areas
- Plugin architecture MUST follow IntelliJ Platform SDK patterns
- Configuration MUST support `~/.ideavimrc` and XDG standard locations
- Status bar, notifications, and UI elements MUST follow IntelliJ UX guidelines

**Rationale**: IdeaVim is a JetBrains plugin; it must behave like a first-class IDE citizen.

### III. vim-engine Separation

The vim-engine module MUST remain IntelliJ Platform-independent.

- Core Vim logic belongs in `vim-engine/` for reuse (e.g., Fleet)
- IdeaVim-specific code belongs in the main module
- Changes to vim-engine MUST NOT introduce IntelliJ Platform dependencies
- Common commands are defined in `vim-engine/src/main/resources/ksp-generated/`
- IdeaVim-only commands are defined in `src/main/resources/ksp-generated/`

**Rationale**: The vim-engine powers multiple products; platform independence enables code reuse.

### IV. Code Quality Standards

Code contributions MUST meet quality and documentation standards.

- All changes MUST include appropriate tests
- Public APIs SHOULD include KDoc/JavaDoc documentation
- Issue tracking uses YouTrack (VIM-XXXX tickets), not GitHub Issues

**Rationale**: Maintainable code with proper testing ensures long-term project health.

## Development Standards

### Language and Dependencies

- **Primary Language**: Kotlin (Java for existing Java areas only)
- **Platform**: IntelliJ Platform SDK
- **Build System**: Gradle

### Project Structure

- `vim-engine/`: Platform-independent Vim engine (shared with Fleet)
- `src/`: IdeaVim plugin code (IntelliJ-specific)
- `tests/`: Test suites including property-based and long-running tests

### Commands Reference

- `./gradlew runIde` — Start dev IntelliJ with IdeaVim
- `./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test` — Run standard tests
- `./gradlew buildPlugin` — Build distributable plugin

## Testing Requirements

### Test Categories

1. **Unit Tests**: Standard behavior verification
2. **Neovim Integration Tests**: Automatic comparison with Neovim state
3. **Property-Based Tests**: Randomized input testing (may be flaky)
4. **Long-Running Tests**: Extended test scenarios

### Test Annotations

- `@VimBehaviorDiffers`: Documents intentional deviation from Vim
- `@TestWithoutNeovim`: Excludes test from Neovim comparison

### Corner Cases Checklist

All behavioral tests SHOULD verify:

- Position-based: line start/end, file start/end, empty lines
- Content-based: whitespace, trailing spaces, Unicode, multi-byte characters
- Selection-based: multiple carets, visual modes (character/line/block)
- Motion-based: dollar motion, counts with motions, zero-width motions
- Buffer state: empty files, single-line files, read-only files

## Governance

This constitution establishes the non-negotiable principles for IdeaVim development.

### Amendment Process

1. Propose changes via GitHub Discussion or YouTrack ticket
2. Document rationale and migration impact
3. Obtain maintainer approval
4. Update constitution with version increment

### Versioning Policy

- **MAJOR**: Backward-incompatible principle changes or removals
- **MINOR**: New principles or materially expanded guidance
- **PATCH**: Clarifications, wording improvements, typo fixes

### Compliance

- All PRs MUST comply with Core Principles
- Code reviewers SHOULD verify constitution adherence
- Violations require explicit justification and documentation

**Version**: 1.0.0 | **Ratified**: 2026-01-30 | **Last Amended**: 2026-01-30
