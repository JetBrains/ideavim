/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

/**
 * There are some double-character commands like `cc`, `dd`, `yy`.
 * During the execution these commands are replaced with `c_`, `d_`, `y_`, etc.
 *
 * This is not any kind of workaround, this is exactly how the original vim works.
 *   The `dd` command (and others) should not be processed as a monolith command, or it will lead to problems
 *   like this: https://youtrack.jetbrains.com/issue/VIM-1189
 *
 * If some command implements this interface, and the user enters motion operator that is the same as the command itself, the
 *   motion operator will be replaced with `_`.
 */
interface DuplicableOperatorAction {
  val duplicateWith: Char
}
