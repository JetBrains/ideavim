# Quick Start Guide for IdeaVim Plugin Development

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

This guide will help you get started with developing plugins for IdeaVim.
We'll cover the essential concepts and show you how to create a simple plugin.

## Setting Up Your First Plugin

### 1. Project Setup

For now, you can create plugin in the IdeaVim extensions package - [link](https://github.com/JetBrains/ideavim/tree/4764ffbbf545607ad4a5c482d74e0219002a5aca/src/main/java/com/maddyhome/idea/vim/extension).

### 2. Create the Plugin Entry Point

The entry point for an IdeaVim plugin is a function annotated with `@VimPlugin`:

```kotlin
@VimPlugin(name = "MyFirstPlugin")
fun VimApi.init() {
    // Plugin initialization code goes here
}
```

Here we will register mappings, listeners, commands etc.

### 3. Add Functionality

Let's add a simple mapping that displays a message in the output panel:

```kotlin
@VimPlugin(name = "MyFirstPlugin")
fun VimApi.init() {
    mappings {
        nmap(keys = "<leader>h", label = "HelloWorld") {
            outputPanel {
                setText("Hello from my first IdeaVim plugin!")
            }
        }
    }
}
```

## Basic Functionality Examples

### Key Mappings

You can define mappings for different Vim modes:

```kotlin
mappings {
    // Normal mode mapping
    nmap(keys = "<leader>x", label = "MyNormalAction") {
        // Action implementation
    }
    
    // Visual mode mapping
    vmap(keys = "<leader>y", label = "MyVisualAction") {
        // Action implementation
    }
    
    // Insert mode mapping
    imap(keys = "<C-d>", label = "MyInsertAction") {
        // Action implementation
    }
}
```

### Working with Variables

You can get and set Vim variables:

```kotlin
// Get a variable
val count = getVariable<Int>("v:count1") ?: 1
val register = getVariable<String>("v:register") ?: "\""

// Set a variable
setVariable("g:my_plugin_enabled", true)
```

### Executing Commands

You can execute normal mode commands and Ex commands:

```kotlin
// Execute a normal mode command
normal("dd")

// Execute an Ex command
execute(":set number")
```

### Text Manipulation

You can manipulate text in the editor:

```kotlin
editor {
    change {
        forEachCaret {
            // Insert text at the current caret position
            insertText(offset, "Hello, world!")
            
            // Replace text in a range
            replaceText(startOffset, endOffset, "New text")
            
            // Delete text in a range
            deleteText(startOffset, endOffset)
        }
    }
}
```

### Working with Registers

Since JetBrains IDEs have multiple-caret support, in IdeaVim every caret has its own registers and marks.
You can read from and write to registers like this:

```kotlin
// Read from register 'a'
val text = editor {
    read {
        withPrimaryCaret { getReg('a') }
    }
}
runBlocking { println(text.await()) }

// Write to register 'b'
val job = editor {
    change {
        withPrimaryCaret {
            setReg('b', "New content", TextType.CHARACTER_WISE)
        }
    }
}
runBlocking { job.join() }
```

## A Simple Plugin Example

Here's a simple plugin that adds a mapping to uppercase the selected text:

```kotlin
@VimPlugin(name = "ToUppercase")
fun VimApi.init() {
    mappings {
        vmap(keys = "<leader>ll", label = "ToUpperCase") {
            editor {
                val job = change {
                    forEachCaret {
                        // Get the current selection
                        val selectionStart = (selection as Range.Simple).start
                        val selectionEnd = (selection as Range.Simple).end

                        // Get the selected text
                        val selectedText = text.substring(selectionStart, selectionEnd)

                        // Replace with uppercase version
                        replaceText(selectionStart, selectionEnd, selectedText.uppercase())
                    }
                }
            }
        }
    }

}
```