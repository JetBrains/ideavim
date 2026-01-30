# Research: IdeaVim API Layer Design Decisions

**Feature Branch**: `001-api-layer`
**Date**: 2026-01-30
**Status**: In Progress

This document captures research findings and design decisions for resolving the critical issues identified in the API
layer implementation.

---

## K1: State Update Complexity

### Problem Statement

Mode changes and caret updates in IdeaVim require multiple coordinated state changes:

- Selection state
- Selection marks (VimSelectionStart, etc.)
- State machine (key trie)
- Caret shape
- Command builder mode

The original API exposed atomic operations (e.g., `mode = Mode.INSERT`) that could leave IdeaVim in an inconsistent
state because they only updated one aspect without coordinating the others.

### Current State Analysis

From the logseq notes:
> "Initially in API we had a function to set the mode. This function just assigned the mode to the new value. However,
> in IdeaVim changing the mode means that we have to reset the selection, update the selection marks, update the internal
> state of the state machine."

The current VimApi interface has:

```kotlin
@set:ApiStatus.Experimental
var mode: Mode
```

This setter is marked experimental because it's dangerous.

### Decision

**Use safe combined operations instead of atomic setters.**

Instead of:

```kotlin
api.mode = Mode.INSERT  // DANGEROUS: doesn't update selection, marks, etc.
```

Provide:

```kotlin
api.enterInsertMode()       // Safe: handles all state transitions
api.enterNormalMode()       // Safe: handles all state transitions
api.enterVisualMode(type)   // Safe: handles selection setup
```

### Rationale

1. **Prevents inconsistent state**: Users cannot accidentally leave IdeaVim broken
2. **Matches Vim semantics**: In Vim, mode changes are always accompanied by side effects
3. **Simpler API**: Users don't need to understand internal state coordination
4. **Extensible**: We can add parameters for specific use cases (e.g., where to place cursor after mode change)

### Alternatives Considered

| Alternative                           | Rejected Because                                             |
|---------------------------------------|--------------------------------------------------------------|
| Keep atomic setter with documentation | Users will make mistakes; documentation doesn't prevent bugs |
| Provide both atomic and combined      | Atomic operations will be misused; creates API confusion     |
| Builder pattern for mode changes      | Overengineered for this use case                             |

### Implementation Notes

- Keep `var mode: Mode` for reading current mode (read is safe)
- Mark the setter as `@Deprecated` or remove it
- Implement `enterInsertMode()`, `enterNormalMode()`, `enterVisualMode()`, etc.
- Each method should delegate to IdeaVim's internal combined operations

### Status

**Decision**: Approved
**Implementation**: Pending

---

## K2: Editor Context

### Problem Statement

The current API always uses "focused editor" for operations. This is problematic:

1. During IdeaVim initialization, no editor may be focused
2. When accessing options (e.g., `surround_no_mappings`), we need to read global options
3. Some operations should work on a specific editor, not whatever happens to be focused

From the logseq notes:
> "Couldn't get a value of `surround_no_mappings` because it required access to the editor, but there is no focused
> editor while IdeaVim loading."

### Current State Analysis

The `editor { }` scope in VimApi always gets the focused editor:

```kotlin
fun <T> editor(block: EditorScope.() -> T): T
```

This fails when:

- Called during plugin initialization (no editor yet)
- User wants to operate on a specific editor
- Running in test environment

### Decision

**Add explicit editor context methods alongside focused editor methods.**

1. Keep `editor { }` for focused editor (common case)
2. Add `withEditor(editor) { }` for explicit editor context
3. Add `globalOption()` for options that don't need editor context
4. Handle null focused editor gracefully

### API Design

```kotlin
interface VimApi {
  // Existing: uses focused editor (may throw if no editor)
  fun <T> editor(block: EditorScope.() -> T): T

  // New: explicit editor context
  fun <T> withEditor(editor: VimEditor, block: EditorScope.() -> T): T

  // New: optional focused editor (returns null if no editor)
  fun <T> editorOrNull(block: EditorScope.() -> T): T?

  // Options don't always need editor context
  fun <T> globalOption(block: OptionScope.() -> T): T
}
```

### Rationale

1. **Backwards compatible**: Existing `editor { }` calls continue to work
2. **Explicit is better than implicit**: When you need a specific editor, you can specify it
3. **Initialization support**: `editorOrNull` and `globalOption` work during init
4. **Test friendly**: Tests can provide mock editors via `withEditor`

### Alternatives Considered

| Alternative                         | Rejected Because                                         |
|-------------------------------------|----------------------------------------------------------|
| Always require explicit editor      | Breaks ergonomics for common case                        |
| Pass editor to VimApi constructor   | Creates multiple VimApi instances; complicates ownership |
| Global singleton for current editor | Same problem as focused editor; not thread-safe          |

### Implementation Notes

- `withEditor` should set an internal context that scopes use
- Consider using Kotlin context receivers when they stabilize
- Test environment should be able to create VimApi with a test editor

### Status

**Decision**: Approved
**Implementation**: Pending

---

## K3: Coroutine Usage

### Problem Statement

The API uses suspend functions in places where IntelliJ Platform doesn't support coroutines:

- Inside read locks
- Inside write locks
- In modal input handlers

From the logseq notes:
> "We should not have suspend functions inside of the read/write functions as IJ doesn't support them."

### Current State Analysis

Suspend functions appear in:

1. `ListenersScope` callbacks (e.g., `onModeChange: suspend VimApi.() -> Unit`)
2. Mapping handlers
3. Operator functions

The `addMapping` function calls suspend function from `runBlocking`:
> "`addMapping` function calls suspend function from the `runBlocking` – Why and how to fix it?"

### Research Findings

IntelliJ Platform threading model:

- Read actions run on any thread but block write actions
- Write actions run on EDT and are not reentrant
- Suspend inside these contexts can cause deadlocks

Kotlin coroutines + IntelliJ:

- `runBlocking` inside read/write action can deadlock
- Suspend functions should be called outside of locks
- Use `withContext(Dispatchers.Default)` for CPU-bound work

### Decision

**Remove suspend from lock-sensitive code paths; use callbacks instead.**

1. **Listener callbacks**: Keep suspend for background-safe listeners, but document constraints
2. **Mapping handlers**: Use regular functions, not suspend
3. **Operator functions**: Use regular functions; provide async variants if needed

### API Changes

```kotlin
// Before
fun onModeChange(callback: suspend VimApi.(Mode) -> Unit)

// After - for handlers that run on EDT
fun onModeChange(callback: VimApi.(Mode) -> Unit)

// For async operations, provide explicit async variant
fun onModeChangeAsync(callback: suspend VimApi.(Mode) -> Unit)
```

### Rationale

1. **Predictable behavior**: Sync callbacks run immediately, no deadlock risk
2. **Platform compatibility**: Matches IntelliJ Platform's threading model
3. **Explicit async**: When async is needed, it's explicitly requested

### Alternatives Considered

| Alternative                          | Rejected Because                              |
|--------------------------------------|-----------------------------------------------|
| Keep all suspend, document carefully | Users will still hit deadlocks                |
| Use `runBlocking` everywhere         | Can deadlock; poor performance                |
| Redesign around coroutines           | Too invasive; doesn't match IJ Platform model |

### Implementation Notes

- Audit all `suspend` in `api/` module
- Identify which are called from EDT or inside locks
- Replace with sync versions or move suspend outside lock scope

### Status

**Decision**: Approved
**Implementation**: Pending (requires audit)

---

## K4: Test Accessibility

### Problem Statement

The API must work in test environments, not just runtime plugin contexts. Currently, `api()` function requires extension
context which may not be available in tests.

### Current State Analysis

The `api()` function:

```kotlin
internal fun VimExtension.api(): VimApi = VimApiImpl(
  ListenerOwner.Plugin.get(this.name),
  MappingOwner.Plugin.get(this.name),
)
```

This is an extension function on `VimExtension`, so you need an extension instance to call it.

### Decision

**Provide test-specific VimApi factory.**

```kotlin
// For production extensions
fun VimExtension.api(): VimApi

// For tests
object VimApiTestFactory {
  fun create(testName: String): VimApi
  fun createWithEditor(testName: String, editor: VimEditor): VimApi
}
```

### Rationale

1. **Separation of concerns**: Production and test code have different needs
2. **Test isolation**: Each test can have its own VimApi instance
3. **Mock support**: Tests can provide mock editors

### Implementation Notes

- Create `VimApiTestFactory` in test sources
- Ensure it doesn't require plugin.xml registration
- Provide utilities for common test scenarios

### Status

**Decision**: Approved
**Implementation**: Pending

---

## API Gaps Resolution

### G1: Missing findBlockTagRange

**Need**: Surround extension needs to find XML tag ranges for `cst` (change surrounding tag)

**Solution**: Add to VimApi:

```kotlin
fun findBlockTagRange(count: Int, isInner: Boolean): Range?
```

**Implementation**: Delegate to `injector.searchHelper.findBlockTagRange`

### G2: deleteText doesn't accept Range

**Need**: Text manipulation with Range object

**Solution**: Add overload:

```kotlin
// In Transaction scope
fun deleteText(range: Range)
```

### G3: Can't get EditorRead from CaretRead

**Need**: Access editor properties while iterating carets

**Solution**: Add property to CaretRead:

```kotlin
interface CaretRead {
  val editor: EditorRead  // Access parent editor scope
  // ... existing properties
}
```

### G4: Caret ID for tracking

**Need**: Track caret across read/write scopes

**Solution**: Add caret identification:

```kotlin
interface CaretRead {
  val id: CaretId  // Unique identifier for this caret
}

// Usage:
val caretInfo = editor { read { carets().map { it.id to it.offset } } }
editor {
  transaction {
    caretInfo.forEach { (id, savedOffset) ->
      caret(id).moveTo(savedOffset)
    }
  }
}
```

**Investigation needed**: Check if RemDev provides caret IDs that can be reused.

---

## Open Questions

### Q1: Read Scope Escalation

**Question**: Should we support upgrading read lock to write lock?

```kotlin
editor {
  read {
    val data = collectData()
    // Can we do this?
    upgradeToWrite {
      applyChanges(data)
    }
  }
}
```

**Current thinking**: No. This is complex to implement correctly and can cause deadlocks. Better to exit read scope and
enter write scope separately.

### Q2: Job Return from Change Operations

**Question**: Is returning `Job` for async changes good API design?

**Current thinking**: No. Async operations should be explicit opt-in, not the default. Most changes should be
synchronous and complete before returning.

### Q3: RemDev Caret IDs

**Question**: Does RemDev provide caret IDs?

**Status**: Needs investigation with RemDev team.

---

## References

- [VIM-4063](https://youtrack.jetbrains.com/issue/VIM-4063): Extension functions vs Interface members ADR
- [VIM-2871](https://youtrack.jetbrains.com/issue/VIM-2871): API for plugins (main tracking ticket)
- Logseq notes: IdeaVim Mia Api.md
- Slack discussion on explicitApiMode (archived)
