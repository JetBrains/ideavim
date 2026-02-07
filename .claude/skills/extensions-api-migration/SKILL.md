---
name: extensions-api-migration
description: Migrates IdeaVim extensions from the old VimExtensionFacade API to the new @VimPlugin annotation-based API. Use when converting existing extensions to use the new API patterns.
---

# Extensions API Migration

You are an IdeaVim extensions migration specialist. Your job is to help migrate existing IdeaVim extensions from the old API (VimExtensionFacade) to the new API (@VimPlugin annotation).

## Key Locations

- **New API module**: `api/` folder - contains the new plugin API
- Old API: `VimExtensionFacade` in vim-engine
- Extensions location: `src/main/java/com/maddyhome/idea/vim/extension/`

## How to Use the New API

### Getting Access to the API

To get access to the new API, call the `api()` function from `com.maddyhome.idea.vim.extension.api`:

```kotlin
val api = api()
```

Obtain the API at the start of the `init()` method - this is the entry point for all further work.

### Registering Text Objects

Use `api.textObjects { }` to register text objects:

```kotlin
// From VimIndentObject.kt
override fun init() {
  val api = api()
  api.textObjects {
    register("ai") { _ -> findIndentRange(includeAbove = true, includeBelow = false) }
    register("aI") { _ -> findIndentRange(includeAbove = true, includeBelow = true) }
    register("ii") { _ -> findIndentRange(includeAbove = false, includeBelow = false) }
  }
}
```

### Registering Mappings

Use `api.mappings { }` to register mappings:

```kotlin
// From ParagraphMotion.kt
override fun init() {
  val api = api()

  api.mappings {
    nmapPluginAction("}", "<Plug>(ParagraphNextMotion)", keepDefaultMapping = true) {
      moveParagraph(1)
    }
    nmapPluginAction("{", "<Plug>(ParagraphPrevMotion)", keepDefaultMapping = true) {
      moveParagraph(-1)
    }
    xmapPluginAction("}", "<Plug>(ParagraphNextMotion)", keepDefaultMapping = true) {
      moveParagraph(1)
    }
    // ... operator-pending mode mappings with omapPluginAction
  }
}
```

### Defining Helper Functions

The lambdas in text object and mapping registrations typically call helper functions. Define these functions with `VimApi` as a receiver - this makes the API available inside:

```kotlin
// From VimIndentObject.kt
private fun VimApi.findIndentRange(includeAbove: Boolean, includeBelow: Boolean): TextObjectRange? {
  val charSequence = editor { read { text } }
  val caretOffset = editor { read { withPrimaryCaret { offset } } }
  // ... implementation using API
}

// From ParagraphMotion.kt
internal fun VimApi.moveParagraph(direction: Int) {
  val count = getVariable<Int>("v:count1") ?: 1
  editor {
    change {
      forEachCaret {
        val newOffset = getNextParagraphBoundOffset(actualCount, includeWhitespaceLines = true)
        if (newOffset != null) {
          updateCaret(offset = newOffset)
        }
      }
    }
  }
}
```

### API Features

<!-- Fill in additional API features here -->

## How to Migrate Existing Extensions

### What Stays the Same

- The extension **still inherits VimExtensionFacade** - this does not change
- The extension **still registers in the XML file** - this does not change

### Migration Steps

#### Step 1: Ensure Test Coverage

Before starting migration, make sure tests exist for the extension:
- Tests should work and have good coverage
- If there aren't enough tests, create more tests first
- Verify tests pass on the existing version of the plugin

#### Step 2: Migrate in Small Steps

- Don't try to handle everything in one run
- Run tests on the plugin (just the single test class to speed up things) after making smaller changes
- This ensures consistency and makes it easier to identify issues
- **Do a separate commit for each small sensible change or migration** unless explicitly told not to

#### Step 3: Migrate Handlers One by One

If the extension has multiple handlers, migrate them one at a time rather than all at once.

#### Step 4: Handler Migration Process

For each handler, follow this approach:

1. **Inject the API**: Add `val api = api()` as the first line inside the `execute` function

2. **Extract to extension function**: Extract the content of the execute function into a separate function outside the `ExtensionHandler` class. The new function should:
   - Have `VimApi` as a receiver
   - Use the api that was obtained before
   - Keep the extraction as-is (no changes to logic yet)

3. **Verify tests pass**: Run tests to ensure the extraction didn't break anything

4. **Migrate function content**: Now start migrating the content of the extracted function to use the new API

5. **Verify tests pass again**: Run tests after each significant change

6. **Update registration**: Finally, change the registration of shortcuts from the existing approach to `api.mappings { }` where you call the newly created function

#### Example Migration Flow

```kotlin
// BEFORE: Old style handler
class MyHandler : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    // ... implementation
  }
}

// STEP 1: Inject API
class MyHandler : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val api = api()
    // ... implementation
  }
}

// STEP 2: Extract to extension function (as-is)
class MyHandler : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val api = api()
    api.doMyAction(/* pass needed params */)
  }
}

private fun VimApi.doMyAction(/* params */) {
  // ... same implementation, moved here
}

// STEP 3-5: Migrate content to new API inside doMyAction()

// STEP 6: Update registration to use api.mappings { }
override fun init() {
  val api = api()
  api.mappings {
    nmapPluginAction("key", "<Plug>(MyAction)") {
      doMyAction()
    }
  }
}
// Now MyHandler class can be removed
```

#### Handling Complicated Plugins

For more complicated plugins, additional steps may be required.

For example, there might be a separate large class that performs calculations. However, this class may not be usable as-is because it takes a `Document` - a class that is no longer directly available through the new API.

In this case, perform a **pre-refactoring step**: update this class to remove the `Document` dependency before starting the main migration. For instance, change it to accept `CharSequence` instead, which is available via the new API.

#### Final Verification: Check for Old API Usage

After migration, verify that no old API is used by checking imports for `com.maddyhome`.

**Allowed imports** (these are still required):
- `com.maddyhome.idea.vim.extension.VimExtension`
- `com.maddyhome.idea.vim.extension.api`

Any other `com.maddyhome` imports indicate incomplete migration.

