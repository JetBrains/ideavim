# Feature Specification: IdeaVim Extension API Layer

**Feature Branch**: `001-api-layer`
**Created**: 2026-01-30
**Status**: Draft
**Input**: User description: "Help me building the API layer for the IdeaVim. IdeaVim has an extensibility features: we
can have extensions for IdeaVim. Some of them are embedded right into IdeaVIm itself. However, some of them are made in
a form of JetBrains IDE plugin. So, this is a JetBrains IDE plugin which is an extension for IdeaVim. Currently, we
don't have an explicit API layer: we just expose everything we use to the external developers. However, this brings an
limitation that it's quite hard for us to change the code and move on as we have to keep the compability. To solve this
issue, we'd like to establish the API layer for the IdeaVim and later hide the internal details. We already have API
implementation in some state that was done by an intern. However, it's not finished and there is a lot of questions to
clean up before we can release it. We'd like to clearify those questions and migrate our plugins to the new API first,
while then ask the others to perform the migration."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Complete API Module for Extensions (Priority: P1)

As an IdeaVim core developer, I want all extension functionality exposed through a dedicated API module, so that
extensions depend only on this module and internal IdeaVim code can evolve freely without breaking compatibility.

**Why this priority**: This is the primary motivation for the entire API layer effort. Without a complete API module,
the team cannot safely refactor internal code, leading to technical debt accumulation and inability to implement new
features efficiently.

**Independent Test**: Can be fully tested by building an extension that uses only the API module dependency, verifying
it can implement all common extension use cases (mappings, text objects, operators, editor access, registers) without
importing from internal modules.

**Acceptance Scenarios**:

1. **Given** the API module is published as a separate dependency, **When** an extension is built using only the API
   module, **Then** all functionality needed for common extension use cases (mappings, text objects, operators, editor
   access, registers) is available.
2. **Given** all internal IdeaVim plugins are migrated to use only the API module, **When** they are tested, **Then**
   they retain full functionality without accessing internal modules.
3. **Given** external plugins listed in `doc/IdeaVim Plugins.md` (re-researched before migration to ensure completeness),
   **When** they are migrated to the new API, **Then** they retain full functionality without significant complexity
   increase.
4. **Given** internal modules are hidden from external access, **When** an extension attempts to import from them, *
   *Then** the build fails, ensuring extensions use only the public API.

---

### User Story 2 - Internal Extension Migration to New API (Priority: P2)

As an IdeaVim developer, I want to migrate all built-in extensions (surround, commentary, multiple-cursors, etc.) from
the old VimExtensionFacade API to the new VimApi, so that the new API is validated through real-world usage and we can
identify gaps before asking external developers to migrate.

**Why this priority**: Migrating internal extensions first serves as proof-of-concept, validates the API design, and
identifies missing functionality before external developers encounter issues.

**Independent Test**: Can be tested by selecting one built-in extension (e.g., surround), migrating it fully to the new
API, and verifying all its functionality works identically to the old implementation.

**Acceptance Scenarios**:

1. **Given** a built-in extension using the old API, **When** it is migrated to use the new VimApi, **Then** all
   existing functionality continues to work as before.
2. **Given** a migrated extension, **When** its feature toggle is enabled via `.ideavimrc` (e.g., `set surround`),
   **Then** the extension initializes and registers its mappings correctly.
3. **Given** the new API is missing functionality needed by an extension, **When** the gap is identified during
   migration, **Then** the API is extended to support the required capability.
4. **Given** a developer familiar with the old API, **When** they migrate an extension to the new API, **Then** the
   migration feels natural - learning new concepts is acceptable, but the overall approach should not require
   re-learning everything from scratch.
5. **Given** a migration introduces friction or complexity, **When** evaluating API changes to reduce that friction,
   **Then** reducing friction is prioritized only if it does not conflict with sound API design principles.

---

### User Story 3 - External Extension Developer Uses Stable API (Priority: P3)

As an external plugin developer creating an IdeaVim extension, I want to use a well-documented, stable API with clear
contracts, so that my extension continues working across IdeaVim updates without constant maintenance.

**Why this priority**: External developers are the ultimate consumers of this API, but their needs can only be properly
addressed after internal validation is complete.

**Independent Test**: Can be tested by an external developer building a simple extension (e.g., custom text object or
mapping) using only the public API, without accessing any internal classes.

**Acceptance Scenarios**:

1. **Given** the public API documentation, **When** an external developer creates a new extension, **Then** they can
   implement common use cases (mappings, text objects, operators) without accessing internal classes.
2. **Given** an extension built against the stable API, **When** IdeaVim releases a minor version update, **Then** the
   extension continues to work without recompilation.
3. **Given** the old VimExtensionFacade API coexists with the new API, **When** an extension uses the old API, **Then**
   documentation guides the developer toward the new API. (Note: Formal deprecation approach to be defined after
   successful external plugin migrations.)
4. **Given** external plugins listed in `doc/IdeaVim Plugins.md` exist, **When** the API is finalized and internal
   migrations complete, **Then** the IdeaVim team migrates these external plugins and provides pull requests to their
   maintainers, both validating the API against more use cases and reducing the burden on external developers.

---

### User Story 4 - API Design Finalization (Priority: P1)

As the IdeaVim team, we want to resolve open design questions in the existing API implementation, so that we can
finalize and stabilize the API before encouraging adoption.

**Why this priority**: Equal to P1 because unresolved design questions block all other work. The implementation
has gaps that must be addressed before migration can proceed.

**Independent Test**: Can be tested by reviewing each open question, making a decision, implementing the resolution, and
documenting the final design.

**Acceptance Scenarios**:

1. **Given** an incomplete API scope (e.g., ListenersScope is commented out), **When** the design question is resolved,
   **Then** the scope is either fully implemented or explicitly removed with documented rationale.
2. **Given** dual extension discovery mechanisms (XML + KSP annotation), **When** the approach is finalized, **Then**
   one mechanism is chosen as primary with clear migration path from the other.
3. **Given** the API is marked @ApiStatus.Experimental, **When** all design questions are resolved, **Then** the
   annotation is updated to indicate stability level.

---

### Edge Cases

- What happens when an extension uses both old and new APIs simultaneously during migration?
- How does the system handle extensions compiled against a newer API version running on older IdeaVim?
- What happens when an internal API change accidentally leaks through to the public API?
- How are extensions handled when they depend on functionality that gets removed from the public API?

## Requirements *(mandatory)*

### Functional Requirements

**API Stability & Versioning**

- **FR-001**: The system MUST provide a clearly separated public API module that external extensions can depend on.
- **FR-002**: The system MUST hide internal implementation classes from external access through appropriate visibility
  modifiers.
- **FR-003**: The system MUST maintain backward compatibility for the public API within major version releases.
- **FR-004**: The system MUST mark deprecated API elements clearly and provide migration guidance.

**Extension Registration**

- **FR-005**: The system MUST support a single, consistent mechanism for extension discovery and registration.
- **FR-006**: The system MUST allow extensions to register mappings for all Vim modes (normal, visual, operator-pending,
  insert, etc.).
- **FR-007**: The system MUST allow extensions to register custom text objects.
- **FR-008**: The system MUST allow extensions to register custom operators.
- **FR-009**: The system MUST support extension enable/disable via `.ideavimrc` configuration.

**Editor Operations**

- **FR-010**: The API MUST provide read access to editor state (text, caret position, selection, mode).
- **FR-011**: The API MUST provide write access to editor state with proper transaction/lock handling abstracted from
  the consumer.
- **FR-012**: The API MUST support operations across multiple carets (multi-cursor scenarios).

**Vim Integration**

- **FR-013**: The API MUST provide access to Vim registers for read and write operations.
- **FR-014**: The API MUST provide access to Vim options (get, set, append operations).
- **FR-015**: The API MUST allow execution of Vimscript commands.
- **FR-016**: The API MUST provide access to Vim variables (g:, v:, b:, w:, t: scopes).

**Migration Support**

- **FR-017**: The system MUST support running old API extensions alongside new API extensions during migration period.
- **FR-018**: The system MUST provide tooling or documentation to assist extension migration from old to new API.

**Design Decisions**

- **FR-019**: Extension discovery MUST use XML-based registration (via plugin.xml `<vimExtension>` tags). The
  experimental @VimPlugin annotation approach is out of scope for this effort.
- **FR-020**: Listener/event API is deferred to a future version. Extensions will not be able to subscribe to IdeaVim
  events (mode changes, text changes, etc.) in the initial API release.

**API Safety**

- **FR-021**: State-changing operations (mode changes, caret updates) MUST be implemented as safe combined operations
  that maintain internal consistency, not as raw atomic setters.
- **FR-022**: The API MUST provide proper editor context handling, not rely solely on "focused editor" which may not
  exist during initialization or in certain execution contexts.
- **FR-023**: The API MUST be usable in test environments, not just runtime plugin contexts.

### Key Entities

- **VimApi**: The main entry point interface providing access to all IdeaVim functionality for extensions.
- **Extension**: A unit of functionality that enhances IdeaVim's capabilities (mappings, text objects, operators).
- **Scope**: A DSL context object providing access to specific functionality (EditorScope, MappingScope,
  TextObjectScope, etc.).
- **Mapping**: A key sequence bound to an action in a specific Vim mode.
- **TextObject**: A range selection mechanism triggered by operator commands (e.g., `diw` - delete inner word).
- **Operator**: A command that operates on a text range (delete, change, yank, etc.).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 12+ built-in extensions are migrated to the new API and function identically to their old
  implementations.
- **SC-002**: Zero internal classes are accessible to extensions using only the public API module dependency.
- **SC-003**: Extension developers can implement the 5 most common use cases (simple mapping, mode-specific mapping,
  custom text object, custom operator, option-controlled behavior) using only public API.
- **SC-004**: Internal refactoring changes (class renames, method signature changes, package reorganization) require
  zero changes to extensions using the public API.
- **SC-005**: Migration documentation enables an experienced extension developer to migrate an average-complexity
  extension within a single work session.
- **SC-006**: The public API passes compatibility testing across at least 3 consecutive minor IdeaVim releases without
  breaking changes.

## Assumptions

- The existing VimApi implementation provides a solid foundation and the design direction is correct; we are refining
  and completing it, not starting over.
- External extension developers are willing to migrate to a new API if it provides clear benefits and migration
  guidance.
- The IntelliJ Platform's extension point system will continue to support the registration mechanisms we choose.
- Built-in extensions represent a comprehensive sample of the functionality external extensions will need.
- Prior analysis (documented in internal notes) has identified key issues including: state update complexity, editor
  access patterns, and coroutine usage that must be addressed during API finalization.

## Known Issues to Address

The following issues have been identified from prior analysis and must be resolved as part of API finalization:

1. **State Update Complexity**: Mode changes and caret updates require multiple coordinated state changes (selection,
   marks, state machine). The API must expose safe combined operations, not dangerous atomic setters.
2. **Editor Context**: The current approach of always using "focused editor" is error-prone. Some operations occur when
   no editor is focused (e.g., during initialization).
3. **Coroutine Usage**: Suspend functions inside read/write locks need review. IntelliJ Platform doesn't support suspend
   inside these contexts.
4. **API Gaps**: Missing functionality includes `findBlockTagRange`, `deleteText` with Range parameter, and ability to
   access EditorRead from CaretRead context.
5. **Test Accessibility**: API must work in test environments, not just runtime plugin contexts.

## Clarifications

### Session 2026-01-30

- Q: Which external plugins should be migrated? → A: Use the list from `doc/IdeaVim Plugins.md`. Before starting migration phase, re-research external plugins to ensure nothing is missed.
- Q: Old API deprecation strategy? → A: No harsh deprecation. Deprecation approach will be defined after successful implementation and migration of external plugins.

## Out of Scope

- Automatic migration tooling (IDE refactoring actions) - manual migration with documentation is acceptable for initial
  release.
- GraphQL or REST API for remote/non-JVM extensions.
- Runtime API version negotiation - extensions must be compiled against compatible API version.
- Changes to the `.ideavimrc` configuration syntax or extension enable/disable mechanism.
