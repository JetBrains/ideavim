# Some facts about Vim

Let’s relax and have some fun now! Here are a few things we've found interesting during development
and would like to share with you.

- There are no such commands as `dd`, `yy`, or `cc`. For example, `dd` is not a separate command for deleting the line,
  but a `d` command with a `d` motion.  
  Wait, but there isn't a `d` motion in Vim! That’s right, and that’s why Vim has a dedicated set of commands
  for which it checks whether the
  [command equals to motion](https://github.com/vim/vim/blob/759d81549c1340185f0d92524c563bb37697ea88/src/normal.c#L6468)
  and if so, it executes `_` motion instead.  
  `_` is an interesting motion that isn't even documented in vi, and it refers to the current line.
  So, commands like `dd`, `yy`, and similar ones are simply translated to `d_`, `y_`, etc.
  [Here](https://github.com/vim/vim/blob/759d81549c1340185f0d92524c563bb37697ea88/src/normal.c#L6502)
  is the source of this knowledge.

- `x`, `D`, and `&` are not separate commands either. They are synonyms of `dl`, `d$`, and `:s\r`, respectively.
  [Here](https://github.com/vim/vim/blob/759d81549c1340185f0d92524c563bb37697ea88/src/normal.c#L5365)
  is the full list of synonyms.

- You can read a [post](https://github.com/JetBrains/ideavim/wiki/how-many-modes-does-vim-have) about how modes work in Vim and IdeaVim.

- Have you ever used `U` after `dd`? [Don't even try](https://github.com/vim/vim/blob/759d81549c1340185f0d92524c563bb37697ea88/src/ops.c#L874).

- A lot of variables that refer to visual mode start with two uppercase letters, e.g. `VIsual_active`. [Some examples](https://github.com/vim/vim/blob/master/src/normal.c#L17).
  As mentioned [here](https://vi.stackexchange.com/a/42885/12441), this was done this way to avoid the clash with X11.

- Other [strange things](https://github.com/vim/vim/blob/759d81549c1340185f0d92524c563bb37697ea88/src/ex_docmd.c#L1845) from vi:
    * ":3"       jumps to line 3
    * ":3|..."   prints line 3
    * ":|"       prints current line

- Vim script doesn't skip white space before comma. `F(a ,b)` => E475.

- Fancy constants for [undolevels](https://vimhelp.org/options.txt.html#%27undolevels%27):
  > The local value is set to -123456 when the global value is to be used.

- Vi (not Vim) is a POSIX standard, and [has a spec](https://pubs.opengroup.org/onlinepubs/9699919799/utilities/vi.html)! Vim is mostly POSIX compliant when Vi compatibility is selected with the `'compatible'` option, but there are still some differences that can be changed with `'copoptions'`. The spec is interesting because it documents the behaviour of different commands in a stricter style than the user documentation, describing the current line and column after the command, for example. [More details can be found by reading `:help posix`](https://vimhelp.org/vi_diff.txt.html#posix).

- The Vim documentation contains many easter eggs. We encounter them occasionally, but GitHub user mikesmithgh has compiled a substantial collection [here](https://github.com/mikesmithgh/vimpromptu).
    - In addition to `:call err_teapot()`, which returns `E418: I'm a teapot`, there is also `:call err_teapot(1)`, which returns `E503: Coffee is currently not available`. Naturally, this is also supported in IdeaVim.

- Insert mode has all `Ctrl` keys mapped, except `Ctrl-B`. In the documentation, it is marked as **"CTRL-B in Insert
  mode gone"**. Call `:h i_CTRL-B-gone` in Vim to read why `Ctrl-B` was removed.
