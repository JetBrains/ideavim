/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.autocmd

enum class AutoCmdEvent(val runsInNormalMode: Boolean) {
  InsertEnter(runsInNormalMode = false),
  InsertLeave(runsInNormalMode = false),
  BufEnter(runsInNormalMode = false),
  BufLeave(runsInNormalMode = false),
  BufRead(runsInNormalMode = true),
  BufReadPost(runsInNormalMode = true),
  BufNewFile(runsInNormalMode = true),
  BufWrite(runsInNormalMode = true),
  BufWritePre(runsInNormalMode = true),
  BufWritePost(runsInNormalMode = true),
  WinEnter(runsInNormalMode = false),
  WinLeave(runsInNormalMode = false),
  FocusGained(runsInNormalMode = false),
  FocusLost(runsInNormalMode = false),
  FileType(runsInNormalMode = true),
  ;

  val canonical: AutoCmdEvent
    get() = when (this) {
      BufRead -> BufReadPost
      BufWrite -> BufWritePre
      else -> this
    }
}
