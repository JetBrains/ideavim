/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode

/**
 * Runs a [CharSearch]. In operator-pending mode the search is handed to the command builder as a motion, so the
 * operator gets the right inclusive/exclusive range. Otherwise the carets are moved directly.
 */
internal class SearchExecutor(private val session: CleverFSession) {
  fun execute(editor: VimEditor, search: CharSearch, count: Int) {
    val commandBuilder = KeyHandler.getInstance().keyHandlerState.commandBuilder
    if (editor.mode is Mode.OP_PENDING) {
      commandBuilder.addAction(CleverFMotionAction(search, session))
      return
    }

    // The search completes the command, so the count must not leak into the next one. In operator-pending mode the
    // count is still needed by the motion action, and the command builder resets it once the command is done.
    commandBuilder.resetCount()
    moveCarets(editor, search, count)
    highlightCandidates(editor, search)
  }

  private fun moveCarets(editor: VimEditor, search: CharSearch, count: Int) {
    for (caret in editor.sortedCarets()) {
      val target = search.findTarget(editor, caret.offset, count) ?: continue
      if (caret.isPrimary) session.recordJump(search.state, editor, target)
      caret.moveToOffset(target)
    }
  }

  private fun highlightCandidates(editor: VimEditor, search: CharSearch) {
    if (!CleverFSettings.markChar) return
    val line = editor.primaryCaret().getLine()
    session.highlighter.show(editor.ij, search.candidatesOnLine(editor, line))
  }
}
