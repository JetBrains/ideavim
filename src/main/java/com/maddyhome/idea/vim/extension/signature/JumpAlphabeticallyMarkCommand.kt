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
 * `']`, `'[`, `` `] `` and `` `[ ``: through the marks of the file in the order of their letters rather than of their
 * position. When the caret is not on a marked line there is no letter to step from, and the plugin falls back to the
 * positional order, as the original does.
 */
internal class JumpAlphabeticallyMarkCommand(private val forward: Boolean, private val startLine: Boolean) :
  ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val editorMarks = SignatureExtension.getEditorMarks(editor)
    if (editorMarks.isEmpty()) return
    val byLetter = editorMarks.sortedWith(Mark.KeySorter)

    var target = firstHop(editor, byLetter, editorMarks) ?: return
    // Every hop after the first steps from the mark we landed on rather than from the caret's line again: two marks
    // sharing a line would otherwise send us back and forth between the same two letters.
    repeat(operatorArguments.count1 - 1) { target = byLetter.step(target) }

    if (startLine) editor.jumpToLine(target.line) else editor.jumpToOffset(target.offset(editor))
  }

  private fun firstHop(editor: VimEditor, byLetter: List<Mark>, editorMarks: List<Mark>): Mark? {
    val cursorLine = editor.primaryCaret().getLine()
    val onMarkedLine = byLetter.firstOrNull { it.line == cursorLine }
    return if (onMarkedLine != null) {
      byLetter.step(onMarkedLine)
    } else {
      JumpLineMarkCommand.getNextLineMark(editorMarks, cursorLine, forward)
    }
  }

  private fun List<Mark>.step(from: Mark): Mark {
    val index = indexOf(from)
    return this[(index + (if (forward) 1 else -1) + size) % size]
  }
}
