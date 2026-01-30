# Implementation Plan: IdeaVim Extension API Layer

**Development**: `master` (trunk-based) | **Date**: 2026-01-30 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-api-layer/spec.md`

## Summary

Create a stable, well-defined API layer for IdeaVim extensions by:

1. Finalizing the existing VimApi implementation (resolving open design questions)
2. Migrating all built-in extensions from VimExtensionFacade to VimApi
3. Ensuring external plugins can migrate to the new API
4. Hiding internal implementation details from external access

The API already exists in `api/` module with scopes (EditorScope, MappingScope, TextObjectScope, etc.) and needs
completion, not a rewrite.

## Technical Context

**Language/Version**: Kotlin (JVM 21)
**Primary Dependencies**: IntelliJ Platform SDK, IdeaVim vim-engine
**Storage**: N/A
**Testing**: IdeaVim test framework (JUnit-based with Neovim comparison tests)
**Target Platform**: JetBrains IDEs (IntelliJ, PyCharm, WebStorm, etc.)
**Project Type**: IntelliJ Platform Plugin
**Performance Goals**: API operations should not add measurable overhead to existing operations
**Constraints**: Must maintain compatibility with IntelliJ Platform patterns; vim-engine must remain
platform-independent
**Scale/Scope**: 12+ built-in extensions to migrate, 20+ external plugins documented in `doc/IdeaVim Plugins.md`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle                          | Compliance            | Notes                                                                                                                         |
|------------------------------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------|
| I. Vim Compatibility (IDE-First)   | ✅ PASS                | API exposes Vim functionality, doesn't change IDE behavior                                                                    |
| II. IntelliJ Platform Integration  | ✅ PASS                | Uses IntelliJ Platform SDK patterns, XML extension points                                                                     |
| III. vim-engine Separation         | ⚠️ REQUIRES ATTENTION | API module should not depend on vim-engine internals; VimApi interface is in `api/` but implementation in IdeaVim main module |
| IV. Code Quality Standards         | ✅ PASS                | Will include tests and documentation                                                                                          |
| V. External Contributors           | ✅ PASS                | API designed for external extension developers                                                                                |
| VI. Documentation Goals            | ✅ PASS                | API documentation is a key deliverable                                                                                        |
| VII. Architecture Decision Records | ✅ PASS                | VIM-4063 already documents extension functions vs interface members decision                                                  |

## Project Structure

### Documentation (this feature)

```text
specs/001-api-layer/
├── plan.md              # This file
├── research.md          # Phase 0 output - design decisions
├── tasks.md             # Phase 2 output (/speckit.tasks command)
└── migration-guide.md   # Phase 1 output - extension migration guide
```

### Source Code (repository root)

```text
api/                              # Public API module (existing)
├── src/main/kotlin/
│   └── com/intellij/vim/api/
│       ├── VimApi.kt            # Main entry point interface
│       ├── VimPlugin.kt         # Plugin annotation (KSP-based, out of scope)
│       ├── models/              # API data models (Mode, Range, etc.)
│       └── scopes/              # DSL scopes for different functionality
│           ├── MappingScope.kt
│           ├── TextObjectScope.kt
│           ├── EditorScope/
│           │   ├── ReadScope.kt
│           │   └── Transaction.kt
│           └── ...

src/main/java/com/maddyhome/idea/vim/
├── extension/                   # Extension infrastructure
│   ├── VimExtension.kt         # Base interface for extensions
│   ├── VimExtensionFacade.kt   # OLD API (to be deprecated eventually)
│   ├── VimApi.kt               # api() helper function
│   └── <extension>/            # Built-in extensions (to migrate)
│       ├── argtextobj/         # ✅ MIGRATED to new API
│       ├── surround/           # ❌ Uses old VimExtensionFacade
│       ├── commentary/         # ❌ Uses old VimExtensionFacade
│       └── ...
└── thinapi/
    └── VimApiImpl.kt           # VimApi implementation

tests/                           # Test suites
├── src/test/kotlin/            # Extension tests
└── ...
```

**Structure Decision**: Existing structure is appropriate. The `api/` module contains the public API interfaces,
implementations live in the main IdeaVim module, and extensions will migrate from VimExtensionFacade to VimApi
incrementally.

## Complexity Tracking

> No constitution violations requiring justification.

## Known Issues to Resolve (from Prior Analysis)

These issues were documented in the logseq notes and spec, and must be addressed:

### Critical Issues (Block Migration)

| ID | Issue                       | Current State                                                                                                                           | Resolution Path                                                                                                                |
|----|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| K1 | **State Update Complexity** | Mode changes/caret updates require multiple coordinated state changes. API had atomic setters that leave IdeaVim in inconsistent state. | Expose only safe combined operations (e.g., `enterMode()` that handles selection, marks, state machine) instead of raw setters |
| K2 | **Editor Context**          | Always using "focused editor" is error-prone. Can't get option values during initialization when no editor is focused.                  | Add explicit editor context passing; provide API methods that work without focused editor                                      |
| K3 | **Coroutine Usage**         | Suspend functions inside read/write locks problematic. IntelliJ Platform doesn't support suspend in these contexts.                     | Audit all suspend usages; remove suspend from lock-sensitive paths                                                             |
| K4 | **Test Accessibility**      | API must work in test environments                                                                                                      | Ensure VimApi can be obtained in test context, not just runtime plugin context                                                 |

### API Gaps (Need Implementation)

| ID | Gap                                               | Use Case                                                               | Priority      |
|----|---------------------------------------------------|------------------------------------------------------------------------|---------------|
| G1 | Missing `findBlockTagRange`                       | Surround extension needs to find XML tag ranges                        | P1            |
| G2 | `deleteText` doesn't accept Range                 | Common text manipulation operation                                     | P1            |
| G3 | Can't get EditorRead from CaretRead               | Need to access editor properties while iterating carets                | P2            |
| G4 | No caret ID for tracking across read/write scopes | After collecting caret info in read scope, can't apply in change scope | P2            |
| G5 | ListenersScope commented out                      | Event subscription not available for extensions                        | P3 (Deferred) |
| G6 | `getLineEndOffset` has flag parameter             | Clean API should not expose internal flags                             | P3            |

### Design Questions (Need Decision)

| ID | Question                          | Options                                   | Current Thinking                                   |
|----|-----------------------------------|-------------------------------------------|----------------------------------------------------|
| D1 | How to request API in extension?  | `api()` function hack vs proper injection | Keep `api()` for now; injection can be added later |
| D2 | Block selection representation    | Start/end corners vs list of selections   | Need more use cases before deciding                |
| D3 | Set of Mode representation in API | Enum vs sealed class                      | Current Mode sealed class works                    |

## Migration Status (Built-in Extensions)

**Criteria**: An extension is "fully migrated" when it only imports `VimExtension` and `api()` from `com.maddyhome` package, with all other functionality using `com.intellij.vim.api`.

| Extension           | Status             | Notes                                                                                         |
|---------------------|--------------------|-----------------------------------------------------------------------------------------------|
| textobjentire       | ✅ Fully migrated   | Uses `api.textObjects { }`, `api.getVariable<T>()`                                            |
| textobjindent       | ✅ Fully migrated   | Uses `api.textObjects { }`, `editor { read { } }`                                             |
| paragraphmotion     | ✅ Fully migrated   | Uses `api.mappings { nmapPluginAction() }`, `editor { change { } }`                           |
| miniai              | ✅ Fully migrated   | Uses `api.textObjects { }`, full API for editor operations                                    |
| argtextobj          | ⚠️ Partial          | Uses `api.textObjects { }` but still uses `VimPlugin.getVariableService()`, `VimPlugin.showMessage()`, `MessageHelper` |
| ReplaceWithRegister | ⚠️ Partial          | Has both old (`ReplaceWithRegister.kt`) and new (`ReplaceWithRegisterNewApi.kt`) implementations |
| surround            | ❌ Not started      | Uses VimExtensionFacade - complex: inputString, operators, tag handling                       |
| commentary          | ❌ Not started      | Uses VimExtensionFacade                                                                       |
| exchange            | ❌ Not started      | Uses VimExtensionFacade                                                                       |
| multiple-cursors    | ❌ Not started      | Uses VimExtensionFacade                                                                       |
| sneak               | ❌ Not started      | Uses VimExtensionFacade - external plugin dependency (AceJump)                                |
| matchit             | ❌ Not started      | Uses VimExtensionFacade                                                                       |
| NERDTree            | ❌ Not started      | Uses VimExtensionFacade - IDE integration heavy                                               |
| highlightedyank     | ❌ Not started      | Uses internal listeners (`VimYankListener`, `ModeChangeListener`) - blocked on G5             |

### Remaining Work for Partial Migrations

**argtextobj**: Remove these internal API usages:
- `VimPlugin.getVariableService()` → Use `api.getVariable<T>()` (already exists in API)
- `VimPlugin.showMessage()` → Need to add to API
- `MessageHelper` → Need to add i18n support to API or use direct strings
- `VimString` → Need to handle in variable service abstraction

**ReplaceWithRegister**: Complete migration by:
- Removing old `ReplaceWithRegister.kt`
- Ensuring `ReplaceWithRegisterNewApi.kt` covers all functionality

## External Plugins (from doc/IdeaVim Plugins.md)

These external plugins should be migrated by the IdeaVim team after internal migrations complete:

| Plugin          | Repository          | Complexity                   |
|-----------------|---------------------|------------------------------|
| anyobject       | Vim AnyObject       | Medium - text objects        |
| dial            | Vim Dial            | Medium - increment/decrement |
| easymotion      | IdeaVim-EasyMotion  | High - external dependency   |
| FunctionTextObj | vim-functiontextobj | Medium - text objects        |
| Peekaboo        | vim-peekaboo        | Medium - register display    |
| quick-scope     | IdeaVim-Quickscope  | Medium - external dependency |
| Switch          | vim-switch          | Medium - text manipulation   |
| Which-Key       | idea-which-key      | High - external dependency   |
| mini.ai         | mini.ai             | Low - text objects           |

## Implementation Phases

### Phase 1: API Finalization (P1 Priority)

Resolve critical issues and API gaps before migration:

1. **State Update Safety (K1)**
    - Review all mode-changing operations in VimApi
    - Ensure combined operations handle: selection, marks, state machine, caret shape
    - Mark raw setters as internal or remove them

2. **Editor Context Fix (K2)**
    - Add optional editor parameter to operations that currently assume focused editor
    - Provide explicit `withEditor(editor) { ... }` scope
    - Fix option access during initialization

3. **Coroutine Audit (K3)**
    - List all suspend functions in API
    - Identify which are called inside read/write locks
    - Replace with non-suspend alternatives where needed

4. **Test Support (K4)**
    - Ensure `api()` function works in test environment
    - Add test utilities for extension testing

5. **Implement Missing APIs (G1-G4)**
    - Add `findBlockTagRange` to API
    - Add `deleteText(Range)` overload
    - Add EditorRead access from CaretRead context
    - Add caret identification mechanism

### Phase 2: Internal Extension Migration (P2 Priority)

#### Already Completed ✅

These extensions are fully migrated and can serve as reference implementations:
- `textobjentire` - TextObjectScope + `getVariable<T>()`
- `textobjindent` - TextObjectScope + complex editor read operations
- `paragraphmotion` - MappingScope with `nmapPluginAction()` + `editor { change { } }`
- `miniai` - TextObjectScope + Range model usage

#### Finish Partial Migrations

1. **argtextobj** - Add missing API methods then remove internal imports:
   - Add `showMessage(text)` to VimApi or OutputPanelScope
   - Use `api.getVariable<T>()` instead of `VimPlugin.getVariableService()`
   - Handle `VimString` type conversion in variable API

2. **ReplaceWithRegister** - Consolidate implementations:
   - Verify `ReplaceWithRegisterNewApi.kt` covers all functionality
   - Remove old `ReplaceWithRegister.kt`
   - Update any tests

#### Remaining Migrations (easiest to hardest)

3. `commentary` - Operator, validates operator function API
4. `exchange` - Complex operator, validates multi-caret + operator
5. `matchit` - Motion with search, validates search API
6. `multiple-cursors` - Multi-caret, validates caret iteration
7. `surround` - Complex: input, tags, operators, validates modal input + command line
8. `NERDTree` - IDE integration, validates project/file APIs
9. `highlightedyank` - Requires ListenersScope (blocked on G5, may defer)
10. `sneak` - External dependency (AceJump), complex

**Migration Pattern**:

```kotlin
// Before (old API)
class MyExtension : VimExtension {
  override fun init() {
    VimExtensionFacade.putKeyMappingIfMissing(...)
    VimExtensionFacade.putExtensionHandlerMapping(...)
  }
}

// After (new API)
class MyExtension : VimExtension {
  override fun init() {
    val api = api()
    api.mappings {
      nmap("keys", "action")
    }
    api.textObjects {
      register("ia") { count -> ... }
    }
  }
}
```

### Phase 3: External Plugin Migration (P3 Priority)

After internal migrations validate the API:

1. Re-research external plugins (verify list from `doc/IdeaVim Plugins.md`)
2. Create migration PRs for each external plugin
3. Document API usage patterns discovered
4. Finalize deprecation approach based on feedback

### Phase 4: API Stabilization

1. Remove `@ApiStatus.Experimental` annotation
2. Document deprecation path for VimExtensionFacade
3. Update all documentation
4. Announce API in release notes and Slack

## Decision Log

| Date       | Decision                                           | Rationale                            | ADR Link       |
|------------|----------------------------------------------------|--------------------------------------|----------------|
| 2025-11-28 | Extension functions over interface members         | Flexibility, backwards compatibility | VIM-4063       |
| 2026-01-02 | UI-robot style scopes (optional lambdas, chaining) | Developer ergonomics                 | -              |
| 2026-01-16 | argTextObj migration validates approach            | Real-world validation                | -              |
| 2026-01-30 | Defer ListenersScope to future version             | Not enough use cases, complex design | spec.md FR-020 |
| 2026-01-30 | XML registration over KSP annotation               | IntelliJ Platform standard, simpler  | spec.md FR-019 |

## Open Questions (Require Resolution Before Implementation)

1. **Caret ID mechanism**: RemDev might provide caret IDs - needs investigation
2. **Read scope escalation**: Should we support upgrading read lock to write lock?
3. **Job return from change operations**: Is returning Job for async changes good API design?

## Next Steps

1. Create `research.md` documenting resolution for each K1-K4 issue
2. Create `tasks.md` with detailed implementation tasks
3. Begin Phase 1 implementation with K2 (editor context) as it unblocks option access
