# Introduction to IdeaVim Plugin Development

This guide explains and gives examples on how to create plugins for IdeaVim, the Vim emulation plugin for IntelliJ-based IDEs.
Existing plugins can be found [here](IdeaVim%20Plugins.md).

## Table of Contents

- [Introduction](#introduction)
- [Plugin Architecture](#plugin-architecture)
- [Scopes](#scopes)
  - [Examples](#scopes-example)
  - [Read and write operations](#read-and-transaction-operations)

## Introduction

IdeaVim plugins aim to extend the functionality of the IdeaVim plugin, allowing you to add custom Vim-like features to your IntelliJ-based IDE.
These plugins can define new commands, mappings, operators, and more, just like Vim plugins do.

The IdeaVim API provides a Kotlin DSL that makes it easy to create new plugins.

## Plugin Architecture

IdeaVim plugins are built using a scope-based architecture.
Starting scope is the `VimScope`, which provides access to various aspects of the editor and Vim functionality.

An IdeaVim plugin written with this API consists of:

1. An entry point function with no parameters and return value annotated with `@VimPlugin`
2. One or more scope blocks that define the plugin's functionality
3. Mappings, commands, or other extensions that users can interact with

Here's a minimal plugin structure:

```kotlin
@VimPlugin(name = "MyPlugin")
fun VimScope.init() {
    // Plugin initialization code
    mappings {
        nmap(keys = "<leader>x", label = "MyPluginAction") {
            // Action implementation
        }
    }
}
```

## Scopes

IdeaVim plugins are written in scopes.
They provide a structured way to write code, improve readability and ensure that functions can be called only within a specific scope.

The base scope is `VimScope`, which provides access to general Vim functionality. From there, plugin writers can access more specialized scopes.
The list of all scopes and their functions is available in the API reference ([link](Plugin-API-reference.md)).

### Scopes example

```kotlin
editor {
  // Now in EditorScope
  change {
    // Make changes to the document
    withPrimaryCaret {
      insertText(offset, "New text")
    }
  }
}

mappings {
  // Now in MappingScope
  nmap(keys = "gx", label = "OpenURL") {
    // Action implementation
  }
}
```

### Read and Transaction Operations

In the IdeaVim API there is a distinction between read and write operations:

- **Read operations** access the state of the editor without modifying it
- **Transaction operations** modify the state of the editor

These operations must be executed under the appropriate locks to ensure thread safety:

```kotlin
// Read operation
val deferred: Deferred<CharSequence> = editor {
  read {
    text  // Get the text of the document
  }
}
runBlocking { println(deferred.await()) }

// Transaction operation
val job: Job = editor {
  change {
    forEachCaret {
      insertText(offset, "Hello, world!")
    }
  }
}
runBlocking { job.join() }
```