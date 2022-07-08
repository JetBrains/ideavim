/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.action.motion.updown

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*

class MotionGotoLineLastEndAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override fun getOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    var allow = false
    if (editor.inInsertMode) {
      allow = true
    } else if (editor.inVisualMode) {
      val opt = (
        injector.optionService.getOptionValue(
          OptionScope.LOCAL(editor),
          OptionConstants.selectionName
        ) as VimString
        ).value
      if (opt != "old") {
        allow = true
      }
    }

    return moveCaretGotoLineLastEnd(editor, operatorArguments.count0, operatorArguments.count1 - 1, allow).toMotion()
  }
}

class MotionGotoLineLastEndInsertAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)

  override fun getOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    var allow = false
    if (editor.inInsertMode) {
      allow = true
    } else if (editor.inVisualMode) {
      val opt = (
        injector.optionService
          .getOptionValue(OptionScope.LOCAL(editor), OptionConstants.selectionName) as VimString
        ).value
      if (opt != "old") {
        allow = true
      }
    }

    return moveCaretGotoLineLastEnd(editor, operatorArguments.count0, operatorArguments.count1 - 1, allow).toMotion()
  }
}

private fun moveCaretGotoLineLastEnd(
  editor: VimEditor,
  rawCount: Int,
  line: Int,
  pastEnd: Boolean,
): Int {
  return injector.motion.moveCaretToLineEnd(
    editor,
    if (rawCount == 0) injector.engineEditorHelper.normalizeLine(editor, editor.lineCount() - 1) else line,
    pastEnd
  )
}
