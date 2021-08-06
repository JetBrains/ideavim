package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import java.lang.Integer.min

data class GoToLineCommand(val ranges: Ranges) :
  Command.ForEachCaret(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_REQUIRED, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: Editor,
    caret: Caret,
    context: DataContext,
    vimContext: VimContext,
  ): ExecutionResult {
    val line = min(this.getLine(editor, caret), EditorHelper.getLineCount(editor) - 1)

    if (line >= 0) {
      val offset = VimPlugin.getMotion().moveCaretToLineWithStartOfLineOption(editor, line, caret)
      MotionGroup.moveCaret(editor, caret, offset)
      return ExecutionResult.Success
    }

    MotionGroup.moveCaret(editor, caret, 0)
    return ExecutionResult.Error
  }
}
