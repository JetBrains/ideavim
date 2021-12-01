# FAQ

## What is select mode?

This mode is where the selection works the same as system selection. When you start typing, the text in the selected area is removed and replaced by the new characters that are being typed in.

## Why is select mode enabled during refactoring?

With the help of the select mode, you can immediately enter the variable name during refactoring. You can go to the beginning or the end of a variable using the arrow keys. If you need to make more complex changes, you can always go back to normal mode with `<ESC>`.

## What if I want to use visual mode during refactoring?

Select mode is controlled by the `keymodel`, `selectmode` and `idearefactormode` options. Set `idearefactormode` to `visual` to adjust this behavior.  
`set idearefactormode=visual`

## What if I don't want to change the mode during refactoring?

`set idearefactormode=keep`

# See Also

* IdeaVim options: https://github.com/JetBrains/ideavim/blob/master/doc/set-commands.md  
* Vim documentation about select mode: https://vimhelp.org/visual.txt.html#Select-mode  
* Stackoverflow explanation: https://vi.stackexchange.com/questions/4891/what-is-the-select-mode-and-when-is-it-relevant-to-use-it