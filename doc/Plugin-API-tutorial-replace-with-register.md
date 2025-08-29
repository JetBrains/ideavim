# Tutorial: Creating an IdeaVim Plugin with the New API

> **⚠️ EXPERIMENTAL API WARNING**
> 
> The Plugin API is currently in an **experimental stage** and is not yet recommended for production use.
> 
> - The API is subject to breaking changes without notice
> - Features may be added, modified, or removed in future releases
> - Documentation may not fully reflect the current implementation
> - Use at your own risk for experimental purposes only
> 
> We welcome feedback and bug reports to help improve the API, but please be aware that stability is not guaranteed at this time.

This tutorial will guide you through the process of creating a plugin for IdeaVim using the new API. We'll implement a "Replace with Register" plugin that allows you to replace text with the contents of a register.

## Table of Contents

- [Introduction](#introduction)
- [Prerequisites](#prerequisites)
- [Project Setup](#project-setup)
- [Plugin Structure](#plugin-structure)
- [Implementing the Plugin](#implementing-the-plugin)
  - [Step 1: Create the init function](#step-1-create-the-init-function)
  - [Step 2: Define Mappings](#step-2-define-mappings)
  - [Step 3: Implement Core Functionality](#step-3-implement-core-functionality)
  - [Step 4: Handle Different Selection Types](#step-4-handle-different-selection-types)
- [Testing Your Plugin](#testing-your-plugin)

## Introduction

The "Replace with Register" plugin ([link](https://github.com/vim-scripts/ReplaceWithRegister) to the original Vim plugin) demonstrates several important concepts in IdeaVim plugin development:

- Creating custom mappings for different Vim modes
- Working with registers
- Manipulating text in the editor
- Handling different types of selections (character-wise, line-wise, block-wise)
- Creating operator functions

This tutorial will walk you through each part of the implementation, explaining the concepts and techniques used.

## Project Setup

1. Clone the IdeaVim repo. (Todo: update)

## Plugin Structure

IdeaVim plugins using the new API are typically structured as follows:

1. An `init` function that sets up mappings and functionality
2. Helper functions that implement specific features

Let's look at how to implement each part of our "Replace with Register" plugin.

## Implementing the Plugin

### Step 1: Create the init function

First, create a Kotlin file for your plugin:

```kotlin
@VimPlugin(name = "ReplaceWithRegister")
fun VimApi.init() {
  // We'll add mappings and functionality here
}
```

The `init` function has a responsibility to set up our plugin within the `VimApi`.

### Step 2: Define Mappings

Now, let's add mappings to our plugin. We'll define three mappings:

1. `gr` + motion: Replace the text covered by a motion with register contents
2. `grr`: Replace the current line with register contents
3. `gr` in visual mode: Replace the selected text with register contents

Add this code to the `init` function:

```kotlin
@VimPlugin(name = "ReplaceWithRegister", shortPath = "username/ReplaceWithRegister")
fun VimApi.init() {
    mappings {
        nmap(keys = "gr", label = "ReplaceWithRegisterOperator", isRepeatable = true) {
            rewriteMotion()
        }
        nmap(keys = "grr", label = "ReplaceWithRegisterLine", isRepeatable = true) {
            rewriteLine()
        }
        vmap(keys = "gr", label = "ReplaceWithRegisterVisual", isRepeatable = true) {
            rewriteVisual()
        }
    }

    exportOperatorFunction("ReplaceWithRegisterOperatorFunc") {
        operatorFunction()
    }
}
```

Let's break down what's happening:

- The `mappings` block gives us access to the `MappingScope`
- `nmap` defines a normal mode mapping, `vmap` defines a visual mode mapping
- Each mapping has:
  - `keys`: The key sequence to trigger the mapping
  - `label`: A unique identifier for the mapping
  - `isRepeatable`: Whether the mapping can be repeated with the `.` command
- The lambda for each mapping calls a function that we'll implement next
- `exportOperatorFunction` registers a function that will be called when the operator is used with a motion

### Step 3: Implement Core Functionality

Now, let's implement the functions we referenced in our mappings:

```kotlin
private fun VimApi.rewriteMotion() {
    setOperatorFunction("ReplaceWithRegisterOperatorFunc")
    normal("g@")
}

private suspend fun VimApi.rewriteLine() {
    val count1 = getVariable<Int>("v:count1") ?: 1
    val job: Job
    editor {
        job = change {
            forEachCaret {
              val endOffset = getLineEndOffset(line.number + count1 - 1, true)
              val lineStartOffset = line.start
              val registerData = prepareRegisterData() ?: return@forEachCaret
              replaceText(lineStartOffset, endOffset, registerData.first)
              updateCaret(offset = lineStartOffset)
            }
        }
    }
    job.join()
}

private suspend fun VimApi.rewriteVisual() {
    val job: Job
    editor {
        job = change {
            forEachCaret {
                val selectionRange = selection
                val registerData = prepareRegisterData() ?: return@forEachCaret
                replaceTextAndUpdateCaret(this@rewriteVisual, selectionRange, registerData)
            }
        }
    }
    job.join()
    mode = Mode.NORMAL()
}

private suspend fun VimApi.operatorFunction(): Boolean {
    fun CaretTransaction.getSelection(): Range? {
        return when (this@operatorFunction.mode) {
            is Mode.NORMAL -> changeMarks
            is Mode.VISUAL -> selection
            else -> null
        }
    }

    val job: Job
    editor {
        job = change {
            forEachCaret {
                val selectionRange = getSelection() ?: return@forEachCaret
                val registerData = prepareRegisterData() ?: return@forEachCaret
                replaceTextAndUpdateCaret(this@operatorFunction, selectionRange, registerData)
            }
        }
    }
    job.join()
    return true
}
```

Let's examine each function:

- `rewriteMotion()`: Sets up an operator function and triggers it with `g@`
- `rewriteLine()`: Replaces one or more lines with register contents
- `rewriteVisual()`: Replaces the visual selection with register contents
- `operatorFunction()`: Implements the operator function

Notice the use of scopes:
- `editor { }` gives us access to the editor
- `change { }` creates a transaction for modifying text
- `forEachCaret { }` iterates over all carets (useful for multi-cursor editing)

### Step 4: Handle Different Selection Types

Now, let's implement the helper functions that prepare register data and handle different types of selections:

```kotlin
private suspend fun CaretTransaction.prepareRegisterData(): Pair<String, TextType>? {
    val lastRegisterName: Char = lastSelectedReg
    var registerText: String = getReg(lastRegisterName) ?: return null
    var registerType: TextType = getRegType(lastRegisterName) ?: return null

    if (registerType.isLine && registerText.endsWith("\n")) {
        registerText = registerText.removeSuffix("\n")
        registerType = TextType.CHARACTER_WISE
    }

    return registerText to registerType
}

private suspend fun CaretTransaction.replaceTextAndUpdateCaret(
    vimApi: VimApi,
    selectionRange: Range,
    registerData: Pair<String, TextType>,
) {
    val (text, registerType) = registerData

    if (registerType == TextType.BLOCK_WISE) {
        val lines = text.lines()

        if (selectionRange is Range.Simple) {
            val startOffset = selectionRange.start
            val endOffset = selectionRange.end
            val startLine = getLine(startOffset)
            val diff = startOffset - startLine.start

            lines.forEachIndexed { index, lineText ->
                val offset = getLineStartOffset(startLine.number + index) + diff
                if (index == 0) {
                    replaceText(offset, endOffset, lineText)
                } else {
                    insertText(offset, lineText)
                }
            }

            updateCaret(offset = startOffset)
        } else if (selectionRange is Range.Block) {
            val selections: Array<Range.Simple> = selectionRange.ranges

            selections.zip(lines).forEach { (range, lineText) ->
                replaceText(range.start, range.end, lineText)
            }
        }
    } else {
        if (selectionRange is Range.Simple) {
            val textLength = this.text.length
            if (textLength == 0) {
              insertText(0, text)
            } else {
              replaceText(selectionRange.start, selectionRange.end, text)
            }
        } else if (selectionRange is Range.Block) {
            val selections: Array<Range.Simple> = selectionRange.ranges.sortedByDescending { it.start }.toTypedArray()
            val lines = List(selections.size) { text }

            replaceTextBlockwise(selectionRange, lines)

            vimApi.mode = Mode.NORMAL()
            updateCaret(offset = selections.last().start)
        }
    }
}
```

These functions handle:

1. `prepareRegisterData()`: Gets the content and type of the last used register
2. `replaceTextAndUpdateCaret()`: Handles the replacement logic for different types of selections and register contents

## Testing Your Plugin

For the "Replace with Register" plugin, you can test it by:

1. Yanking some text with `y`
2. Moving to different text and using `gr` followed by a motion
3. Selecting text in visual mode and using `gr`
4. Using `grr` to replace a whole line

For more information, check out the [API Reference](Plugin-API-reference.md) and the [Quick Start Guide](Plugin-API-quick-start-guide.md).
