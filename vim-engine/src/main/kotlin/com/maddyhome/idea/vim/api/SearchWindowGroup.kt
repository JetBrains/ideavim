/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.common.Direction

/**
 * Owns Vim's command-line window (`:help cmdwin`) — the editable buffer popped open by
 * `q:`, `q/` and `q?` that lists history one line per entry and runs the line on `<CR>`.
 */
interface SearchWindowGroup {
  /** `q:` — open the cmdwin populated with `:` command history. */
  fun openCommandHistoryWindow(editor: VimEditor, context: ExecutionContext)

  /** `q/` and `q?` — open the cmdwin populated with `/` search history. */
  fun openSearchHistoryWindow(editor: VimEditor, context: ExecutionContext, direction: Direction)

  /** `c_CTRL-F` — open the cmdwin with the history of [commandLine]'s type, and what was typed in it as the last line. */
  fun openFromCommandLine(editor: VimEditor, context: ExecutionContext, commandLine: Argument.ExString)

  /**
   * Closes the cmdwin and runs the line at the caret against the editor that was active
   * when the cmdwin was opened (`:help cmdwin-execute`). No-op on a blank line beyond
   * the close. Must be called on the cmdwin editor.
   */
  fun executeCurrentLineAndClose(cmdwin: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext)
}
