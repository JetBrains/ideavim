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

### K1: State Update Safety

- [ ] T004 [US4] Implement enterInsertMode() safe combined operation in api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T005 [US4] Implement enterNormalMode() safe combined operation in api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T006 [US4] Implement enterVisualMode(type) safe combined operation in api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T007 [US4] Deprecate var mode: Mode setter in VimApi.kt (add @Deprecated annotation)
- [ ] T008 [US4] Implement combined operations in VimApiImpl in src/main/java/com/maddyhome/idea/vim/thinapi/VimApiImpl.kt

### K2: Editor Context Fix

- [ ] T009 [US4] Add withEditor(editor) scope method to api/src/main/kotlin/com/intellij/vim/api/VimApi.kt
- [ ] T010 [US4] Add editorOrNull() method for safe focused editor access in VimApi.kt
- [ ] T011 [US4] Add globalOption() scope for editor-independent option access in VimApi.kt
- [ ] T012 [US4] Implement withEditor, editorOrNull, globalOption in VimApiImpl.kt
- [ ] T013 [US4] Update EditorScope to support explicit editor context in api/src/main/kotlin/com/intellij/vim/api/scopes/

### K3: Coroutine Audit

- [ ] T014 [US4] Remove suspend from mapping handlers in MappingScope.kt that run inside locks
- [ ] T015 [US4] Remove suspend from operator functions in VimApi.kt that run on EDT
- [ ] T016 [US4] Add KDoc to VimApi.kt clarifying which callbacks support suspend vs require sync

### K4: Test Accessibility

- [ ] T017 [US4] Create VimApiTestFactory in tests/java-tests/src/test/kotlin/ for test environments
- [ ] T018 [US4] Implement create(testName) factory method in VimApiTestFactory
- [ ] T019 [US4] Implement createWithEditor(testName, editor) factory method in VimApiTestFactory
- [ ] T020 [US4] Add test utilities for common extension testing scenarios

### G1-G4: API Gaps

- [ ] T021 [P] [US4] Add findBlockTagRange(count, isInner) to VimApi.kt (G1)
- [ ] T022 [P] [US4] Add deleteText(range: Range) overload to Transaction scope (G2)
- [ ] T023 [P] [US4] Add editor property to CaretRead interface for parent access (G3)
- [ ] T024 [P] [US4] Add CaretId type and id property to CaretRead for caret tracking (G4)
- [ ] T025 [US4] Implement G1-G4 in VimApiImpl.kt

**Checkpoint**: API Design Finalization complete - all critical issues resolved, API ready for migration

---

## Phase 3: User Story 1 - Complete API Module for Extensions (Priority: P1)

**Goal**: Ensure all extension functionality is exposed through the dedicated API module so extensions depend only on this module

**Independent Test**: Build an extension that uses only the API module dependency, verifying it can implement all common extension use cases (mappings, text objects, operators, editor access, registers) without importing from internal modules

**Note**: API backward compatibility (FR-003) is verified by TeamCity CI automatically; no manual verification task needed.

### API Completeness - Write Tests

- [ ] T026 [P] [US1] Write test: MappingScope supports all Vim modes (normal, visual, insert, operator-pending)
- [ ] T027 [P] [US1] Write test: TextObjectScope registers and invokes custom text objects
- [ ] T028 [P] [US1] Write test: custom operator registration and invocation via OperatorScope
- [ ] T029 [P] [US1] Write test: EditorScope read/write access to editor state (text, caret, selection)
- [ ] T030 [P] [US1] Write test: register access API read/write operations
- [ ] T031 [P] [US1] Write test: option access API (get, set, append)
- [ ] T032 [P] [US1] Write test: execute("normal! dd"), execute("set number"), execute("let g:var = 1")
- [ ] T033 [P] [US1] Write test: getVariable/setVariable for each scope (g:, v:, b:, w:, t:)

### API for Missing Functionality

- [ ] T034 [US1] Add showMessage(text) to VimApi or OutputPanelScope for user feedback
- [ ] T035 [US1] Add VimString type handling in variable service abstraction
- [ ] T036 [US1] Implement showMessage in VimApiImpl.kt

### Module Visibility

- [ ] T037 [US1] Add Gradle check: api/ module has no imports from vim-engine internal packages
- [ ] T038 [US1] Configure build.gradle.kts to expose only api/ module to external plugins
- [ ] T039 [US1] Add README section documenting module dependency configuration for plugin developers

**Checkpoint**: API Module is complete - all extension functionality available through public API

---

## Phase 4: User Story 2 - Internal Extension Migration to New API (Priority: P2)

**Goal**: Migrate all built-in extensions from VimExtensionFacade API to VimApi, validating the API through real-world usage

**Independent Test**: Select one built-in extension (e.g., surround), migrate it fully to the new API, verify all functionality works identically

### Finish Partial Migrations

- [ ] T040 [US2] Complete argtextobj migration: replace VimPlugin.getVariableService() with api.getVariable<T>() in src/main/java/com/maddyhome/idea/vim/extension/argtextobj/
- [ ] T041 [US2] Complete argtextobj migration: replace VimPlugin.showMessage() with api.showMessage()
- [ ] T042 [US2] Complete argtextobj migration: remove MessageHelper usage
- [ ] T043 [US2] Complete argtextobj migration: handle VimString type in variable API
- [ ] T044 [US2] Run argtextobj tests and fix any failures

- [ ] T045 [US2] Remove old ReplaceWithRegister.kt, keep only ReplaceWithRegisterNewApi.kt
- [ ] T046 [US2] Run ReplaceWithRegister tests and fix any failures

### Extension Migrations (ordered by complexity)

- [ ] T047 [US2] Migrate commentary extension in src/main/java/com/maddyhome/idea/vim/extension/commentary/
- [ ] T048 [US2] Run commentary tests and fix any failures

- [ ] T049 [US2] Migrate exchange extension in src/main/java/com/maddyhome/idea/vim/extension/exchange/
- [ ] T050 [US2] Run exchange tests and fix any failures

- [ ] T051 [US2] Migrate matchit extension in src/main/java/com/maddyhome/idea/vim/extension/matchit/
- [ ] T052 [US2] Run matchit tests and fix any failures

- [ ] T053 [US2] Migrate multiple-cursors extension in src/main/java/com/maddyhome/idea/vim/extension/multiplecursors/
- [ ] T054 [US2] Run multiple-cursors tests and fix any failures

- [ ] T055 [US2] Migrate surround extension in src/main/java/com/maddyhome/idea/vim/extension/surround/
- [ ] T056 [US2] Run surround tests and fix any failures

- [ ] T057 [US2] Migrate NERDTree extension in src/main/java/com/maddyhome/idea/vim/extension/nerdtree/
- [ ] T058 [US2] Run NERDTree tests and fix any failures

- [ ] T059 [US2] Skip highlightedyank migration (blocked on ListenersScope G5, deferred)
- [ ] T060 [US2] Skip sneak migration (external AceJump dependency, deferred)

**Checkpoint**: All feasible internal extensions migrated - API validated through real-world usage

---

## Phase 5: User Story 3 - External Extension Developer Experience (Priority: P3)

**Goal**: Ensure external plugin developers can use a well-documented, stable API with clear contracts

**Independent Test**: External developer can build a simple extension (custom text object or mapping) using only the public API

### Documentation

- [ ] T061 [US3] Create doc/api/README.md with API reference documentation
- [ ] T062 [US3] Add example code: simple mapping, mode-specific mapping in doc/api/examples/
- [ ] T063 [US3] Add example code: custom text object, custom operator in doc/api/examples/
- [ ] T064 [US3] Add example code: option-controlled behavior in doc/api/examples/
- [ ] T065 [US3] Create specs/001-api-layer/migration-guide.md from VimExtensionFacade to VimApi

### External Plugin Migrations (IdeaVim team provides PRs)

- [ ] T066 [US3] Migrate mini.ai plugin and create PR (Low complexity)
- [ ] T067 [US3] Migrate anyobject plugin and create PR (Medium complexity - text objects)
- [ ] T068 [US3] Migrate dial plugin and create PR (Medium complexity - increment/decrement)
- [ ] T069 [US3] Migrate FunctionTextObj plugin and create PR (Medium complexity - text objects)
- [ ] T070 [US3] Migrate Peekaboo plugin and create PR (Medium complexity - register display)
- [ ] T071 [US3] Migrate Switch plugin and create PR (Medium complexity - text manipulation)
- [ ] T072 [US3] Migrate easymotion plugin and create PR (High complexity - external dependency)
- [ ] T073 [US3] Migrate quick-scope plugin and create PR (Medium complexity - external dependency)
- [ ] T074 [US3] Migrate Which-Key plugin and create PR (High complexity - external dependency)

### API Coexistence

- [ ] T075 [US3] Add @Deprecated annotations to VimExtensionFacade methods with migration hints
- [ ] T076 [US3] Update CONTRIBUTING.md with guidance on using new API

**Checkpoint**: External developers can successfully use the stable API

---

## Phase 6: Polish & API Stabilization

**Purpose**: Final stabilization and release preparation

- [ ] T077 [P] Remove @ApiStatus.Experimental from stable API methods in VimApi.kt
- [ ] T078 [P] Update CHANGES.md with API layer release notes
- [ ] T079 [P] Update README.md with extension development section
- [ ] T080 Create doc/extension-quickstart.md for new extension developers
- [ ] T081 Run full test suite to verify no regressions

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
- T021, T022, T023, T024 (API gaps) can run in parallel
- T026-T033 (API completeness tests) can run in parallel
- T077, T078, T079 (Polish) can run in parallel

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
- Total: 81 tasks across 6 phases
