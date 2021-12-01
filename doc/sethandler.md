# Configuring conflicting keys via .ideavimrc

IdeaVim allows defining handlers for the shortcuts that exist for both IDE and Vim (e.g. `<C-C>`).

```vim
" Use ctrl-c as an ide shortcut in normal and visual modes
sethandler <C-C> n-v:ide i:vim
```

This option consist of an optional shortcut and a list of space separated list of handlers:
`mode-list:handler mode-list:handler ...`  
The `mode-list` is a dash separated list of modes that is similar to `guicursor` notation
and defines the following modes:
 - n - normal mode
 - i - insert mode
 - x - visual mode
 - v - visual and select modes
 - a - all modes

The `handler` is an argument that may accept the following values:
 - ide - use IDE handler
 - vim - use Vim handler

Examples:
 - `n:ide` - use IDE handler in normal mode
 - `i-v:vim` - use Vim handler in normal, visual, and select modes
 - `a:ide` - use IDE handler in all modes

By using `sethandler` you can define handlers:
 - For a single shortcut: `sethandler <C-A> n:vim i-x:ide` - use Vim handler in normal mode and IDE handler in insert and visual modes,
 - For all shortcuts: `sethandler n:vim i:ide` - use Vim handlers in normal mode and IDE handlers in insert mode.

If the definition of the handler is missing for some mode, it defaults to `vim`:
`sethandler <C-X> i:ide` - use IDE handler in insert mode and Vim handler in all other modes.
