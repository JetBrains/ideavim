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

- [X] T001 Verify API module structure in api/src/main/kotlin/com/intellij/vim/api/
- [X] T002 [P] Document current VimApi interface methods in api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [X] T003 [P] Identify all VimExtensionFacade usages across extension/ directory

---

## Phase 2: User Story 4 - API Design Finalization (Priority: P1) 🎯

**Goal**: Resolve open design questions in the existing API implementation so we can finalize and stabilize the API before encouraging adoption

**Independent Test**: Can be tested by reviewing each open question, making a decision, implementing the resolution, and documenting the final design

### K1: Editor Context Fix

- [X] T004 [US4] Define if the API works everywhere in IDE, or in editor context only (decision document)
- [X] T005 [US4] Understand how to obtain the editor: focused approach is unreliable (focus can change mid-operation, is global state). Usually editor is captured at shortcut entry point. Edge cases: macros that change editors via :wincmd, API commands that switch windows. Document proper handling strategy. — Documented in VIM-4122 ADR
- [X] T005a [US4] Pre-construct VimApi and pass it to the init method instead of manual object creation in extension handlers
- [X] T005b [US4] Pass project id to VimApi to properly get the editor using getSelectedEditor(project) API
- [X] T005c [US4] Add EditorContextTest to verify getSelectedEditor tracks active editor changes after window switching
- [X] T006 [US4] Design two-phase API: init phase (no editor) vs execute phase (editor in context) — Implemented VimInitApi delegation wrapper, ADR in VIM-4137
- [X] T007 [US4] DROPPED — Lazy getter is correct per VIM-4122; capturing editor at construction breaks selectNextWindow()
- [X] T008 [US4] DROPPED — projectId IS the capture mechanism (passed at VimApi construction, used in getSelectedEditor)
- [X] T009 [US4] Update handler infrastructure to pass captured editor to handlers — Already done in MappingScopeImpl:284 and TextObjectScopeImpl:149

### K1a: Window Switching Threading

- [X] T005d [US4] Investigate selectNextWindow() behavior:
  - **Async gap**: Investigated. setAsCurrentWindow updates `_currentWindowFlow` synchronously, but `getSelectedTextEditor` reads `currentCompositeFlow` which is derived via `flatMapLatest` + `stateIn(Lazily)` — async, with no way to observe propagation. No IJ platform code uses the "switch then read" pattern. Filed IJPL-235369 for a platform fix. Window management APIs commented out in VimApi until resolved (VIM-4138).
  - **EDT requirement**: in tests, `selectNextWindow()` must be wrapped in `invokeAndWait` to avoid NPE (`getCurrentWindow` returns null on BGT). This is documented in the test and matches platform requirements (`@RequiresEdt` on related APIs).

### K2: State Update Safety

- [X] T010-T014 [US4] RESOLVED: Removed enterInsertMode/enterNormalMode/enterVisualMode — mode changes use normal() instead (matching Vim plugin patterns). Mode property is val (read-only). See VIM-4143 for future proper mode-changing API design.

### K3: Coroutine Audit

- [X] T015-T017 [US4] Per-group coroutine audit completed — see VIM-4144 ADR for decisions. Handler lambdas, scope openers, flat VimApi methods, and non-locking scope methods are suspend. Lock-bound code (read/change blocks, caret scopes, CommandLineTransaction) stays non-suspend. Init and registration stays non-suspend. Added command() to VimInitApi.

### K4: Test Accessibility

- [ ] T018 [US4] Create VimApiTestFactory in tests/java-tests/src/test/kotlin/ for test environments
- [ ] T019 [US4] Implement create(testName) factory method in VimApiTestFactory
- [ ] T020 [US4] Implement createWithEditor(testName, editor) factory method in VimApiTestFactory
- [ ] T021 [US4] Add test utilities for common extension testing scenarios

### G1-G4: API Gaps

- [ ] T022 [P] [US4] Add findBlockTagRange(count, isInner) to VimApi.kt (G1)
- [ ] T023 [P] [US4] Add deleteText(range: Range) overload to Transaction scope (G2)
- [ ] T024 [P] [US4] Add editor property to CaretRead interface for parent access (G3)
- [ ] T025 [P] [US4] Add CaretId type and id property to CaretRead for caret tracking (G4)
- [ ] T026 [US4] Implement G1-G4 in VimApiImpl.kt

**Checkpoint**: API Design Finalization complete - all critical issues resolved, API ready for migration

---

## Phase 3: User Story 1 - Complete API Module for Extensions (Priority: P1)

**Goal**: Ensure all extension functionality is exposed through the dedicated API module so extensions depend only on this module

**Independent Test**: Build an extension that uses only the API module dependency, verifying it can implement all common extension use cases (mappings, text objects, operators, editor access, registers) without importing from internal modules

**Note**: API backward compatibility (FR-003) is verified by TeamCity CI automatically; no manual verification task needed.

### API Completeness - Write Tests

- [ ] T027 [P] [US1] Write test: MappingScope supports all Vim modes (normal, visual, insert, operator-pending)
- [ ] T028 [P] [US1] Write test: TextObjectScope registers and invokes custom text objects
- [ ] T029 [P] [US1] Write test: custom operator registration and invocation via OperatorScope
- [ ] T030 [P] [US1] Write test: EditorScope read/write access to editor state (text, caret, selection)
- [ ] T031 [P] [US1] Write test: register access API read/write operations
- [ ] T032 [P] [US1] Write test: option access API (get, set, append)
- [ ] T033 [P] [US1] Write test: execute("normal! dd"), execute("set number"), execute("let g:var = 1")
- [ ] T034 [P] [US1] Write test: getVariable/setVariable for each scope (g:, v:, b:, w:, t:)

### API for Missing Functionality

- [ ] T035 [US1] Add showMessage(text) to VimApi or OutputPanelScope for user feedback
- [ ] T036 [US1] Add VimString type handling in variable service abstraction
- [ ] T037 [US1] Implement showMessage in VimApiImpl.kt

### Module Visibility

- [ ] T038 [US1] Add Gradle check: api/ module has no imports from vim-engine internal packages
- [ ] T039 [US1] Configure build.gradle.kts to expose only api/ module to external plugins
- [ ] T040 [US1] Add README section documenting module dependency configuration for plugin developers

**Checkpoint**: API Module is complete - all extension functionality available through public API

---

## Phase 4: User Story 2 - Internal Extension Migration to New API (Priority: P2)

**Goal**: Migrate all built-in extensions from VimExtensionFacade API to VimApi, validating the API through real-world usage

**Independent Test**: Select one built-in extension (e.g., surround), migrate it fully to the new API, verify all functionality works identically

### Finish Partial Migrations

- [ ] T041 [US2] Complete argtextobj migration: replace VimPlugin.getVariableService() with api.getVariable<T>() in src/main/java/com/maddyhome/idea/vim/extension/argtextobj/
- [ ] T042 [US2] Complete argtextobj migration: replace VimPlugin.showMessage() with api.showMessage()
- [ ] T043 [US2] Complete argtextobj migration: remove MessageHelper usage
- [ ] T044 [US2] Complete argtextobj migration: handle VimString type in variable API
- [ ] T045 [US2] Run argtextobj tests and fix any failures

- [ ] T046 [US2] Remove old ReplaceWithRegister.kt, keep only ReplaceWithRegisterNewApi.kt
- [ ] T047 [US2] Run ReplaceWithRegister tests and fix any failures

### Extension Migrations (ordered by complexity)

- [ ] T048 [US2] Migrate commentary extension in src/main/java/com/maddyhome/idea/vim/extension/commentary/
- [ ] T049 [US2] Run commentary tests and fix any failures

- [ ] T050 [US2] Migrate exchange extension in src/main/java/com/maddyhome/idea/vim/extension/exchange/
- [ ] T051 [US2] Run exchange tests and fix any failures

- [ ] T052 [US2] Migrate matchit extension in src/main/java/com/maddyhome/idea/vim/extension/matchit/
- [ ] T053 [US2] Run matchit tests and fix any failures

- [ ] T054 [US2] Migrate multiple-cursors extension in src/main/java/com/maddyhome/idea/vim/extension/multiplecursors/
- [ ] T055 [US2] Run multiple-cursors tests and fix any failures

- [ ] T056 [US2] Migrate surround extension in src/main/java/com/maddyhome/idea/vim/extension/surround/
- [ ] T057 [US2] Run surround tests and fix any failures

- [ ] T058 [US2] Migrate NERDTree extension in src/main/java/com/maddyhome/idea/vim/extension/nerdtree/
- [ ] T059 [US2] Run NERDTree tests and fix any failures

- [ ] T060 [US2] Skip highlightedyank migration (blocked on ListenersScope G5, deferred)
- [ ] T061 [US2] Skip sneak migration (external AceJump dependency, deferred)

**Checkpoint**: All feasible internal extensions migrated - API validated through real-world usage

---

## Phase 5: User Story 3 - External Extension Developer Experience (Priority: P3)

**Goal**: Ensure external plugin developers can use a well-documented, stable API with clear contracts

**Independent Test**: External developer can build a simple extension (custom text object or mapping) using only the public API

### Documentation

- [ ] T062 [US3] Create doc/api/README.md with API reference documentation
- [ ] T063 [US3] Add example code: simple mapping, mode-specific mapping in doc/api/examples/
- [ ] T064 [US3] Add example code: custom text object, custom operator in doc/api/examples/
- [ ] T065 [US3] Add example code: option-controlled behavior in doc/api/examples/
- [ ] T066 [US3] Create specs/001-api-layer/migration-guide.md from VimExtensionFacade to VimApi

### External Plugin Migrations (IdeaVim team provides PRs)

- [ ] T067 [US3] Migrate mini.ai plugin and create PR (Low complexity)
- [ ] T068 [US3] Migrate anyobject plugin and create PR (Medium complexity - text objects)
- [ ] T069 [US3] Migrate dial plugin and create PR (Medium complexity - increment/decrement)
- [ ] T070 [US3] Migrate FunctionTextObj plugin and create PR (Medium complexity - text objects)
- [ ] T071 [US3] Migrate Peekaboo plugin and create PR (Medium complexity - register display)
- [ ] T072 [US3] Migrate Switch plugin and create PR (Medium complexity - text manipulation)
- [ ] T073 [US3] Migrate easymotion plugin and create PR (High complexity - external dependency)
- [ ] T074 [US3] Migrate quick-scope plugin and create PR (Medium complexity - external dependency)
- [ ] T075 [US3] Migrate Which-Key plugin and create PR (High complexity - external dependency)

### API Coexistence

- [ ] T076 [US3] Add @Deprecated annotations to VimExtensionFacade methods with migration hints
- [ ] T077 [US3] Update CONTRIBUTING.md with guidance on using new API

**Checkpoint**: External developers can successfully use the stable API

---

## Phase 6: Polish & API Stabilization

**Purpose**: Final stabilization and release preparation

- [ ] T078 [P] Remove @ApiStatus.Experimental from stable API methods in VimApi.kt
- [ ] T079 [P] Update CHANGES.md with API layer release notes
- [ ] T080 [P] Update README.md with extension development section
- [ ] T081 Create doc/extension-quickstart.md for new extension developers
- [ ] T082 Run full test suite to verify no regressions

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **US4 - API Finalization (Phase 2)**: Depends on Setup - BLOCKS US1, US2, US3
- **US1 - Complete API (Phase 3)**: Depends on US4 completion
- **US2 - Internal Migration (Phase 4)**: Depends on US1 completion (needs complete API)
- **US3 - External Experience (Phase 5)**: Depends on US2 completion (needs validated API)
- **Polish (Phase 6)**: Depends on US3 completion

### User Story Dependencies

```text
Setup → US4 (API Finalization) → US1 (Complete API) → US2 (Internal Migration) → US3 (External) → Polish
               P1                      P1                     P2                      P3
```

Note: US4 and US1 are both P1 but US4 must complete first as it resolves critical issues blocking US1.

### Within User Story 2 (Internal Migration)

- Finish partial migrations first (argtextobj, ReplaceWithRegister)
- Then migrate in complexity order (commentary → exchange → matchit → multiple-cursors → surround → NERDTree)
- highlightedyank and sneak are deferred (blocked on ListenersScope and external dependencies)

### Parallel Opportunities

- T002, T003 can run in parallel (Setup)
- T022, T023, T024, T025 (API gaps) can run in parallel
- T027-T034 (API completeness tests) can run in parallel
- T078, T079, T080 (Polish) can run in parallel

---

## Implementation Strategy

### MVP First (US4 + US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: US4 - API Design Finalization (resolve K1-K4, G1-G4)
3. Complete Phase 3: US1 - Complete API Module
4. **STOP and VALIDATE**: Test that extensions can use only API module
5. Deploy/demo if ready

### Incremental Delivery

1. Setup → Ready for implementation
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
- highlightedyank is deferred (blocked on ListenersScope G5)
- sneak is deferred (external AceJump dependency)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Total: 82 tasks across 6 phases
