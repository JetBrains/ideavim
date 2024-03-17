# How Many Modes Does Vim Have?

When asked this question, many people will say three: Normal, Visual, and Insert. Some will have heard about One-command mode, while others will know about the two types of modes that are available during selection: Visual and Select. The official documentation, however, identifies seven main modes and seven additional ones (search for `:h vim-mode` [here](https://vimhelp.org/intro.txt.html#vim-modes)). What’s more, the documentation for the built-in `mode()` function states that up to 35 different mode values may be returned (search for `:h mode()` [here](https://vimhelp.org/builtin.txt.html#mode%28%29)).


## Modes in Vim

The `mode()` function returns a string. Each character in this string represents a mode or its sub-mode. For example:



* n          Normal
* i           Insert
* no        Operator-pending
* niI        Normal using |i_CTRL-O| in |Insert-mode|
* niR      Normal using |i_CTRL-O| in |Replace-mode|

The documentation for the function states that, if you check the mode, you should only compare by prefix rather than the whole string. Moreover, the list of modes can be extended.

During the development of IdeaVim, it turned out that even 35 modes were not enough to represent the full range of Vim states. For example, the `mode()` function does not cover the state when we enter Operator-pending mode after One-command mode. This information is important because we have to know that Vim should enter Insert mode after this particular Operator-pending command is finished.

All of the above shows that Vim itself doesn’t have a strictly bounded set of modes. Rather, the list can be extended with new ones, and the regular modes, like Visual or Normal, may have “submodes” that specify certain details of Vim’s state.

Inside the code, the state of Vim is defined by the `State` variable in the `globals.h` file ([source](https://github.com/neovim/neovim/blob/master/src/nvim/globals.h#L637)). This is an integer variable with a [list of possible values](https://github.com/neovim/neovim/blob/master/src/nvim/vim.h#L47). However, this list does not define the full range of modes, and other variables from the `globals.h `file store information about specifics. For example, <code>[restart_edit](https://github.com/neovim/neovim/blob/389165cac1596bf602c50904a789722d65ceaac7/src/nvim/globals.h#L670)</code> is set when we enter One-command mode, and <code>[VIsual_active](https://github.com/neovim/neovim/blob/389165cac1596bf602c50904a789722d65ceaac7/src/nvim/globals.h#L535)</code> defines whether Visual mode is enabled (check out [this README](https://github.com/JetBrains/ideavim#some-facts-about-vim) to learn why this variable starts with two uppercase letters). Meanwhile, the <code>[get_real_mode](https://github.com/neovim/neovim/blob/389165cac1596bf602c50904a789722d65ceaac7/src/nvim/state.c#L154)</code> function helps determine whether Vim is in Select or Visual mode.

The `mode()` function, which delegates to <code>[get_mode()](https://github.com/neovim/neovim/blob/389165cac1596bf602c50904a789722d65ceaac7/src/nvim/state.c#L173)</code>, is a big set of “if” commands that collect the information from different variables and combine them into a string representation.


## Modes in IdeaVim

For a long time, IdeaVim used a stack to store the information about the current mode:



* The default value is Normal mode
* When entering Insert mode, an “INSERT” command is added to the stack
* If the user presses _Ctrl-O_ to enter One-time command mode, a “NORMAL” command is added to the stack.
* On _Esc_, the top value of the stack is popped, activating Insert mode.
* On the second _Esc_, the top value is popped again, deactivating Insert mode.

This solution worked well for a long time. However, it sometimes led to programming mistakes, as this structure allowed incorrect states when the stack contained a senseless mix of modes. For the most part, these downsides were manageable, and we were generally able to keep the state consistent. Nevertheless, we recently decided to perform a refactoring.

Now IdeaVim has a single `Mode` interface ([source](https://github.com/JetBrains/ideavim/blob/98886cb269752a5f989c90b1da90fc624b3a381c/vim-engine/src/main/kotlin/com/maddyhome/idea/vim/state/mode/Mode.kt#L27)) that represents the mode and all possible states of it. This solution is type-safe and helps developers of IdeaVim avoid setting illegal states.
