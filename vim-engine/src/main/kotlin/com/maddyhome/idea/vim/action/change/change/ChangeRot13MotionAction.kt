/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.change

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler

@CommandOrMotion(keys = ["g?"], modes = [Mode.NORMAL])
class ChangeRot13MotionAction : ChangeEditorActionHandler.ForEachCaret(), DuplicableOperatorAction {
  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = '?'

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (argument == null) return false

    val range = injector.motion.getMotionRange(
      editor, caret, context, argument,
      operatorArguments
    ) ?: return false

    return applyRot13(editor, caret, range)
  }

  companion object {
    /**
     * Apply ROT13 encoding to the text in the given range.
     * Only ASCII letters below 0x80 are converted.
     */
    fun applyRot13(editor: VimEditor, caret: VimCaret, range: TextRange): Boolean {
      val starts = range.startOffsets
      val ends = range.endOffsets

      for (i in ends.indices.reversed()) {
        applyRot13ToRange(editor, caret, starts[i], ends[i])
      }

      caret.moveToOffset(range.startOffset)
      return true
    }

    /**
     * Apply ROT13 encoding to the text between start and end offsets.
     * Only ASCII letters below 0x80 are converted.
     */
    private fun applyRot13ToRange(editor: VimEditor, caret: VimCaret, start: Int, end: Int) {
      val (newStart, newEnd) = if (start > end) end to start else start to end

      val changedText = buildString {
        for (i in newStart until newEnd) {
          append(rot13(editor.text()[i]))
        }
      }

      injector.changeGroup.replaceText(editor, caret, newStart, newEnd, changedText)
    }

    /**
     * Apply ROT13 encoding to a single character.
     *
     * Only ASCII letters below 0x80 are converted, see https://github.com/vim/vim/blob/470317f78b110b4559cecb26039b5f93447c1bf0/src/ops.c#L1591
     */
    private fun rot13(ch: Char): Char {
      // Only convert ASCII letters below 0x80
      return when {
        ch.code < 0x80 && ch in 'a'..'z' -> 'a' + (ch - 'a' + 13) % 26
        ch.code < 0x80 && ch in 'A'..'Z' -> 'A' + (ch - 'A' + 13) % 26
        else -> ch
      }
    }
  }
}
