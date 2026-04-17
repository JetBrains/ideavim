Autocommands
============

IdeaVim supports Vim's `:autocmd` for running commands on editor events.
Handlers are registered from `~/.ideavimrc` or interactively in Command-line mode.
Every effort is made to match Vim's behaviour, but some differences are inevitable
because the IDE's event model doesn't map 1:1 onto Vim's.

Syntax
------

```
autocmd [group] {event}[,{event}...] {pattern} {command}
autocmd!
autocmd! {group}
```

- `{event}` — one or more comma-separated event names (see below).
- `{pattern}` — file pattern (see "Patterns" below). For `FileType`, the pattern matches the filetype name, not the file
  path.
- `{command}` — any Ex command or Vimscript expression.
- `autocmd!` — clears all registered handlers, or all handlers in the given augroup.

```vim
augroup my_group
  autocmd!
  autocmd BufWritePre *.py echo "saving python"
augroup END
```

Patterns
--------

Autocmd file patterns support the following glob syntax:

| Pattern     | Matches                                  |
|-------------|------------------------------------------|
| `*`         | Any characters except path separators    |
| `**`        | Any characters including path separators |
| `?`         | Any single non-separator character       |
| `[abc]`     | Any character in the set                 |
| `{foo,bar}` | Either `foo` or `bar`                    |

If the pattern contains `/` or `\`, it matches against the full path;
otherwise it matches against the filename only.

`FileType` is special: its pattern matches against the filetype name
(e.g. `python`, `java`) rather than the file path.

Supported events
----------------

### Insert mode

| Event         | Fires when           |
|---------------|----------------------|
| `InsertEnter` | Entering Insert mode |
| `InsertLeave` | Leaving Insert mode  |

### Buffers

| Event          | Fires when                                                        |
|----------------|-------------------------------------------------------------------|
| `BufEnter`     | A buffer becomes active (every switch)                            |
| `BufLeave`     | A buffer stops being active                                       |
| `BufRead`      | A file is loaded into a buffer for the first time                 |
| `BufReadPost`  | Alias of `BufRead` (same event, two names)                        |
| `BufNewFile`   | Editing a file that was just created (fires instead of `BufRead`) |
| `BufWrite`     | Alias of `BufWritePre`                                            |
| `BufWritePre`  | Before the buffer is written to disk                              |
| `BufWritePost` | After the buffer has been written to disk                         |

### Windows

| Event      | Fires when                             |
|------------|----------------------------------------|
| `WinEnter` | A window becomes active (every switch) |
| `WinLeave` | A window stops being active            |

### Files

| Event      | Fires when                                                                                       |
|------------|--------------------------------------------------------------------------------------------------|
| `FileType` | A buffer's filetype is determined (typically once per file load). Pattern matches filetype name. |

### Focus

| Event         | Fires when                 |
|---------------|----------------------------|
| `FocusGained` | The IDE window gains focus |
| `FocusLost`   | The IDE window loses focus |

### Event order

When opening a file for the first time:

```
BufRead/BufReadPost → FileType → BufEnter
```

When opening a just-created file:

```
BufNewFile → FileType → BufEnter
```

When switching buffers:

```
BufLeave → WinLeave → WinEnter → BufEnter
```

When saving:

```
BufWrite/BufWritePre → (write) → BufWritePost
```

Differences from Vim
--------------------

**`FileType` names.** IdeaVim maps IntelliJ's file type name to a Vim-style
filetype. For most languages the lowercased IJ name matches Vim's filetype
(`Python`→`python`, `JAVA`→`java`). A small override table handles cases where
Vim's convention differs: `PLAIN_TEXT`→`text`, `C++`→`cpp`, `C#`→`cs`,
`Shell Script`→`sh`, `ObjectiveC`→`objc`, `JavaScript`→`javascript`,
`TypeScript`→`typescript`, `Vue.js`→`vue`, `CMakeLists.txt`→`cmake`,
`Handlebars/Mustache`→`handlebars`.

**`BufNewFile` detection.** IdeaVim tracks files created during the session
via the VFS. When such a file is opened in an editor, `BufNewFile` fires
instead of `BufRead`. Files created by VCS pulls, build tools, or external
processes that you later open in an editor will also be treated as new files.

**`BufWritePre` / `BufWritePost` frequency.** IntelliJ auto-saves on focus
loss, tab switch, build, and other events. These autocmds fire more often
than Vim's `:w`, so handlers should be idempotent.
