# Environment Variable Expansion in File Commands

What can be more interesting than environment variable expansion rules in Vim? Probably anything. Yet, here is what we learned about it from Vim.

Commands like `:source $HOME/.vimrc` or `:split ~/notes.txt` use environment variables and tilde in file paths. Vim expands these before opening files, but the exact rules are more nuanced than the documentation suggests.

## Vim's File Argument Expansion

In Vim's source code (`src/ex_cmds.h`), commands that accept file arguments are marked with special flags:

- **`EX_FILE1`** - Single file argument with expansion
- **`EX_FILES`** - Multiple file arguments with expansion
- **`EX_XFILE`** - Enable wildcard and environment variable expansion

When these flags are set, Vim automatically expands:
- Environment variables: `$VAR`, `${VAR}`
- Tilde: `~`, `~/path`
- Wildcards: `*`, `?`
- Special chars: `%` (current file), `#` (alternate file)

## Two Different Expansion Behaviors

Vim has **two different behaviors** for environment variable expansion:

### 1. File Commands (`:source`, `:split`, etc.)

Non-existent variables expand to **empty string**:

```vim
:source $NONEXISTENT/file.vim  → :source /file.vim
```

### 2. Option Settings (`:set` command)

The `:help expand-env` documentation describes expansion for the `:set` command. Only **39 specific options** support expansion, controlled by the `P_EXPAND` flag (`0x10`) defined in `src/option.h`.

Options with `P_EXPAND` include: `shell`, `path`, `backupdir`, `makeprg`, `grepprg`, `runtimepath`, and others.

Non-existent variables are **left as-is**:

```vim
:set shell=$NONEXISTENT  → shell=$NONEXISTENT  (kept literally)
:set shell=$HOME/bash    → shell=/Users/you/bash  (expanded)
```

**Note**: Setting options via `:let` does **not** perform expansion:

```vim
:let &shell = "$HOME/bash"  → shell=$HOME/bash  (literal string, not expanded)
```

This distinction was verified in both Vim 9.1 and Nvim 0.11.4.

## Vim Commands with File Argument Expansion

In Vim's source code (`src/ex_cmds.h`), **92 commands** are marked with `EX_FILE1`, `EX_FILES`, or `EX_XFILE` flags to enable file argument expansion:

- **File Editing (24)**: `:edit`, `:split`, `:vsplit`, `:new`, `:vnew`, `:find`, `:tabedit`, `:read`, `:write`, `:saveas`, etc.
- **Exit/Write-Quit (7)**: `:exit`, `:xit`, `:wq`, `:wqall`, `:wnext`, etc.
- **Argument List (8)**: `:args`, `:argadd`, `:next`, `:argedit`, etc.
- **Directory (6)**: `:cd`, `:lcd`, `:tcd`, `:chdir`, etc.
- **Build/Search (12)**: `:make`, `:grep`, `:vimgrep`, `:cscope`, etc.
- **Quickfix (6)**: `:cfile`, `:cgetfile`, `:lfile`, etc.
- **Session (5)**: `:mksession`, `:mkview`, `:loadview`, etc.
- **Scripting (9)**: `:source`, `:runtime`, `:luafile`, `:pyfile`, `:rubyfile`, etc.
- **Diff (2)**: `:diffpatch`, `:diffsplit`
- **Undo/Viminfo (4)**: `:wundo`, `:rundo`, `:wviminfo`, `:rviminfo`
- **Miscellaneous (9)**: `:redir`, `:helptags`, `:mkspell`, `:packadd`, `:terminal`, etc.
