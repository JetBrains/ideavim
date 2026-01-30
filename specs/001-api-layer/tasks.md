# Tasks: IdeaVim Extension API Layer

**Input**: Design documents from `/specs/001-api-layer/`
**Prerequisites**: plan.md, spec.md, research.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **API module**: `api/src/main/kotlin/com/intellij/vim/api/`
- **Implementation**: `src/main/java/com/maddyhome/idea/vim/`
- **Extensions**: `src/main/java/com/maddyhome/idea/vim/extension/`
- **Tests**: `tests/java-tests/src/test/kotlin/`

---

## Phase 1: Setup

**Purpose**: Verify current state and prepare for implementation

- [ ] T001 Verify API module structure in api/src/main/kotlin/com/intellij/vim/api/
- [ ] T002 [P] Document current VimApi interface methods in api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T003 [P] Identify all VimExtensionFacade usages across extension/ directory

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core API infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Audit all suspend functions in api/ module for coroutine safety (K3)
- [ ] T005 [P] Review VimApiImpl implementation in src/main/java/com/maddyhome/idea/vim/thinapi/VimApiImpl.kt
- [ ] T006 [P] Create list of all built-in extensions and their current migration status
- [ ] T007 Document VimApi test accessibility requirements for src/test/ environments (K4)

**Checkpoint**: Foundation ready - API finalization can now begin

---

## Phase 3: User Story 4 - API Design Finalization (Priority: P1) 🎯

**Goal**: Resolve open design questions in the existing API implementation so we can finalize and stabilize the API before encouraging adoption

**Independent Test**: Can be tested by reviewing each open question, making a decision, implementing the resolution, and documenting the final design

### K1: State Update Safety

- [ ] T008 [US4] Review mode-changing operations in api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T009 [US4] Implement enterInsertMode() safe combined operation in api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T010 [US4] Implement enterNormalMode() safe combined operation in api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T011 [US4] Implement enterVisualMode(type) safe combined operation in api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T012 [US4] Mark or remove raw mode setter in VimApi (deprecate var mode: Mode setter)
- [ ] T013 [US4] Implement combined operations in VimApiImpl in src/main/java/com/maddyhome/idea/vim/thinapi/VimApiImpl.kt

### K2: Editor Context Fix

- [ ] T014 [US4] Add withEditor(editor) scope method to api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T015 [US4] Add editorOrNull() method for safe focused editor access in VimApi.kt
- [ ] T016 [US4] Add globalOption() scope for editor-independent option access in VimApi.kt
- [ ] T017 [US4] Implement withEditor, editorOrNull, globalOption in VimApiImpl.kt
- [ ] T018 [US4] Update EditorScope to support explicit editor context in api/src/main/kotlin/com/intellij/vim/api/scopes/

### K3: Coroutine Audit

- [ ] T019 [US4] List all suspend functions in api/ module and categorize by lock context
- [ ] T020 [US4] Remove suspend from mapping handlers that run inside locks
- [ ] T021 [US4] Remove suspend from operator functions that run on EDT
- [ ] T022 [US4] Document which API callbacks support suspend vs require sync in VimApi.kt

### K4: Test Accessibility

- [ ] T023 [US4] Create VimApiTestFactory in tests/java-tests/src/test/kotlin/ for test environments
- [ ] T024 [US4] Implement create(testName) factory method in VimApiTestFactory
- [ ] T025 [US4] Implement createWithEditor(testName, editor) factory method in VimApiTestFactory
- [ ] T026 [US4] Add test utilities for common extension testing scenarios

### G1-G4: API Gaps

- [ ] T027 [P] [US4] Add findBlockTagRange(count, isInner) to VimApi.kt (G1)
- [ ] T028 [P] [US4] Add deleteText(range: Range) overload to Transaction scope (G2)
- [ ] T029 [P] [US4] Add editor property to CaretRead interface for parent access (G3)
- [ ] T030 [P] [US4] Add CaretId type and id property to CaretRead for caret tracking (G4)
- [ ] T031 [US4] Implement G1-G4 in VimApiImpl.kt

**Checkpoint**: API Design Finalization complete - all critical issues resolved, API ready for migration

---

## Phase 4: User Story 1 - Complete API Module for Extensions (Priority: P1)

**Goal**: Ensure all extension functionality is exposed through the dedicated API module so extensions depend only on this module

**Independent Test**: Build an extension that uses only the API module dependency, verifying it can implement all common extension use cases (mappings, text objects, operators, editor access, registers) without importing from internal modules

**Note**: API backward compatibility (FR-003) is verified by TeamCity CI automatically; no manual verification task needed.

### API Completeness Verification

- [ ] T032 [US1] Verify MappingScope supports all Vim modes in api/src/main/kotlin/com/intellij/vim/api/scopes/MappingScope.kt
- [ ] T033 [US1] Verify TextObjectScope supports custom text object registration
- [ ] T034 [US1] Verify OperatorScope supports custom operator registration
- [ ] T035 [US1] Verify EditorScope provides read/write access to editor state
- [ ] T036 [US1] Verify register access API for read/write operations
- [ ] T037 [US1] Verify option access API (get, set, append operations)
- [ ] T038 [US1] Verify Vimscript command execution API: test execute("normal! dd"), execute("set number"), execute("let g:var = 1")
- [ ] T039 [US1] Verify variable access API: test getVariable/setVariable for each scope (g:global, v:count, b:buffer, w:window, t:tab)

### API for Missing Functionality

- [ ] T040 [US1] Add showMessage(text) to VimApi or OutputPanelScope for user feedback
- [ ] T041 [US1] Add VimString type handling in variable service abstraction
- [ ] T042 [US1] Implement showMessage in VimApiImpl.kt

### Module Visibility

- [ ] T043 [US1] Review api/ module dependencies to ensure no internal leakage
- [ ] T043a [US1] Verify api/ module has no imports from vim-engine internal packages (only public vim-engine APIs)
- [ ] T044 [US1] Verify build.gradle.kts exposes only api/ module to external plugins
- [ ] T045 [US1] Document module dependency configuration for external plugin developers

**Checkpoint**: API Module is complete - all extension functionality available through public API

---

## Phase 5: User Story 2 - Internal Extension Migration to New API (Priority: P2)

**Goal**: Migrate all built-in extensions from VimExtensionFacade API to VimApi, validating the API through real-world usage

**Independent Test**: Select one built-in extension (e.g., surround), migrate it fully to the new API, verify all functionality works identically

### Finish Partial Migrations

- [ ] T046 [US2] Complete argtextobj migration: replace VimPlugin.getVariableService() with api.getVariable<T>() in src/main/java/com/maddyhome/idea/vim/extension/argtextobj/
- [ ] T047 [US2] Complete argtextobj migration: replace VimPlugin.showMessage() with api.showMessage()
- [ ] T048 [US2] Complete argtextobj migration: remove MessageHelper usage
- [ ] T049 [US2] Complete argtextobj migration: handle VimString type in variable API
- [ ] T050 [US2] Verify argtextobj tests pass after migration

- [ ] T051 [US2] Consolidate ReplaceWithRegister: verify ReplaceWithRegisterNewApi.kt covers all functionality
- [ ] T052 [US2] Remove old ReplaceWithRegister.kt after verification
- [ ] T053 [US2] Update ReplaceWithRegister tests for new API

### Extension Migrations (ordered by complexity)

- [ ] T054 [US2] Migrate commentary extension in src/main/java/com/maddyhome/idea/vim/extension/commentary/
- [ ] T055 [US2] Verify commentary tests pass after migration

- [ ] T056 [US2] Migrate exchange extension in src/main/java/com/maddyhome/idea/vim/extension/exchange/
- [ ] T057 [US2] Verify exchange tests pass after migration

- [ ] T058 [US2] Migrate matchit extension in src/main/java/com/maddyhome/idea/vim/extension/matchit/
- [ ] T059 [US2] Verify matchit tests pass after migration

- [ ] T060 [US2] Migrate multiple-cursors extension in src/main/java/com/maddyhome/idea/vim/extension/multiplecursors/
- [ ] T061 [US2] Verify multiple-cursors tests pass after migration

- [ ] T062 [US2] Migrate surround extension in src/main/java/com/maddyhome/idea/vim/extension/surround/
- [ ] T063 [US2] Verify surround tests pass after migration (complex: input, tags, operators)

- [ ] T064 [US2] Migrate NERDTree extension in src/main/java/com/maddyhome/idea/vim/extension/nerdtree/
- [ ] T065 [US2] Verify NERDTree tests pass after migration (IDE integration heavy)

- [ ] T066 [US2] Evaluate highlightedyank extension migration feasibility (blocked on ListenersScope G5)
- [ ] T067 [US2] Document highlightedyank as deferred if ListenersScope not implemented

- [ ] T068 [US2] Evaluate sneak extension migration feasibility (external AceJump dependency)
- [ ] T069 [US2] Document sneak migration path if external dependencies require special handling

**Checkpoint**: All feasible internal extensions migrated - API validated through real-world usage

---

## Phase 6: User Story 3 - External Extension Developer Experience (Priority: P3)

**Goal**: Ensure external plugin developers can use a well-documented, stable API with clear contracts

**Independent Test**: External developer can build a simple extension (custom text object or mapping) using only the public API

### Documentation

- [ ] T070 [US3] Create API reference documentation in doc/api/ or specs/001-api-layer/migration-guide.md
- [ ] T071 [US3] Document common use cases: simple mapping, mode-specific mapping
- [ ] T072 [US3] Document common use cases: custom text object, custom operator
- [ ] T073 [US3] Document common use cases: option-controlled behavior
- [ ] T074 [US3] Create migration guide from VimExtensionFacade to VimApi

### External Plugin Research

- [ ] T075 [US3] Re-research external plugins from doc/IdeaVim Plugins.md for completeness
- [ ] T076 [US3] Create migration priority list for external plugins

### External Plugin Migrations (IdeaVim team provides PRs)

- [ ] T077 [US3] Migrate mini.ai plugin and create PR (Low complexity)
- [ ] T078 [US3] Migrate anyobject plugin and create PR (Medium complexity - text objects)
- [ ] T079 [US3] Migrate dial plugin and create PR (Medium complexity - increment/decrement)
- [ ] T080 [US3] Migrate FunctionTextObj plugin and create PR (Medium complexity - text objects)
- [ ] T081 [US3] Migrate Peekaboo plugin and create PR (Medium complexity - register display)
- [ ] T082 [US3] Migrate Switch plugin and create PR (Medium complexity - text manipulation)
- [ ] T083 [US3] Migrate easymotion plugin and create PR (High complexity - external dependency)
- [ ] T084 [US3] Migrate quick-scope plugin and create PR (Medium complexity - external dependency)
- [ ] T085 [US3] Migrate Which-Key plugin and create PR (High complexity - external dependency)

### API Stability

- [ ] T086 [US3] Define coexistence behavior for old + new API during migration period
- [ ] T087 [US3] Document deprecation approach (post-external-migration, no harsh deprecation)

**Checkpoint**: External developers can successfully use the stable API

---

## Phase 7: Polish & API Stabilization

**Purpose**: Final stabilization and release preparation

- [ ] T088 [P] Review and update @ApiStatus.Experimental annotations based on stability level
- [ ] T089 [P] Update CHANGES.md with API layer release notes
- [ ] T090 [P] Update README.md with extension development section
- [ ] T091 Document deprecation path for VimExtensionFacade (not harsh, guidance-focused)
- [ ] T092 Create extension development quickstart guide
- [ ] T093 Announce API in release notes
- [ ] T094 Run full test suite to verify no regressions

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **US4 - API Finalization (Phase 3)**: Depends on Foundational - BLOCKS US1, US2, US3
- **US1 - Complete API (Phase 4)**: Depends on US4 completion
- **US2 - Internal Migration (Phase 5)**: Depends on US1 completion (needs complete API)
- **US3 - External Experience (Phase 6)**: Depends on US2 completion (needs validated API)
- **Polish (Phase 7)**: Depends on US3 completion

### User Story Dependencies

```text
Setup → Foundational → US4 (API Finalization) → US1 (Complete API) → US2 (Internal Migration) → US3 (External) → Polish
                              P1                      P1                     P2                      P3
```

Note: US4 and US1 are both P1 but US4 must complete first as it resolves critical issues blocking US1.

### Within User Story 2 (Internal Migration)

- Finish partial migrations first (argtextobj, ReplaceWithRegister)
- Then migrate in complexity order (commentary → exchange → matchit → multiple-cursors → surround → NERDTree)
- highlightedyank and sneak may be deferred/documented

### Parallel Opportunities

- T002, T003 can run in parallel
- T005, T006 can run in parallel
- T027, T028, T029, T030 (API gaps) can run in parallel
- T088, T089, T090 can run in parallel

---

## Implementation Strategy

### MVP First (US4 + US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: US4 - API Design Finalization (resolve K1-K4, G1-G4)
4. Complete Phase 4: US1 - Complete API Module
5. **STOP and VALIDATE**: Test that extensions can use only API module
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US4 → API issues resolved → Can proceed with confidence
3. US1 → API complete → External plugins can technically use it
4. US2 → Internal migrations done → API validated through real usage
5. US3 → External experience ready → Full adoption possible

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- US4 and US1 are both P1 but have dependency relationship
- highlightedyank is blocked on ListenersScope (G5, deferred)
- sneak has external dependency (AceJump) requiring special handling
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
