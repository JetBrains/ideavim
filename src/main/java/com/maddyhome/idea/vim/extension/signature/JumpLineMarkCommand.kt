/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.signature

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.mark.Mark

/**
 * `]'` and `['`: to the next or previous line holding a mark, on its first non-blank character.
 *
 * The exact-position variants of these, `` ]` `` and `` [` ``, are IdeaVim's own motions and are left alone.
 */
internal class JumpLineMarkCommand(private val forward: Boolean) : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val editorMarks = SignatureExtension.getEditorMarks(editor)
    if (editorMarks.isEmpty()) return

    var line = getNextLineMark(editorMarks, editor.primaryCaret().getLine(), forward)?.line ?: return
    repeat(operatorArguments.count1 - 1) { line = getNextLineMark(editorMarks, line, forward)?.line ?: line }

    editor.jumpToLine(line)
  }

  companion object {
    fun getNextLineMark(
      localMarks: List<Mark>,
      cursorLine: Int,
      forward: Boolean,
    ): Mark? = if (forward) {
      val sorted = localMarks.sortedBy { it.line }
      sorted.firstOrNull { it.line > cursorLine } ?: sorted.firstOrNull()
    } else {
      val sorted = localMarks.sortedByDescending { it.line }
      sorted.firstOrNull { it.line < cursorLine } ?: sorted.firstOrNull()
    }
  }
}
