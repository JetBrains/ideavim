/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.normalizeColumn
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler

/**
 * Adds a cursor on the line below or above the last added one, keeping its column
 */
internal class NextLineHandler(private val direction: Direction) : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val skipping = LineSkipping.read()
    for (i in 1..operatorArguments.count1) {
      if (!addCursor(editor, skipping)) break
    }
  }

  /**
   * The last added cursor is the primary one. Its last column is kept on shorter lines, like `j` and `k` keep it.
   */
  private fun addCursor(editor: VimEditor, skipping: LineSkipping): Boolean {
    val caret = editor.primaryCaret()
    val column = caret.vimLastColumn
    val line = findLine(editor, caret.getBufferPosition().line, column, skipping) ?: return false

    val position = BufferPosition(line, editor.normalizeColumn(line, column, false))
    val newCaret = editor.addCaret(editor.bufferPositionToOffset(position)) ?: return false
    newCaret.vimLastColumn = column
    injector.scroll.scrollCaretIntoView(editor)
    return true
  }

  private fun findLine(editor: VimEditor, fromLine: Int, column: Int, skipping: LineSkipping): Int? {
    var line = fromLine + direction.step
    while (line in 0 until editor.lineCount()) {
      if (!skipping.skips(editor.lineLength(line), column)) return line
      line += direction.step
    }
    return null
  }

  internal enum class Direction(val step: Int) {
    DOWN(1), UP(-1)
  }
}
