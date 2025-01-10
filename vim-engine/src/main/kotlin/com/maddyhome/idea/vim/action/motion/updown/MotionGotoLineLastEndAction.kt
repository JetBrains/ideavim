/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.updown

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeLine
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.isInsertionAllowed
import java.util.*

@CommandOrMotion(keys = ["<C-End>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionGotoLineLastEndAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    var allow = false
    if (editor.isInsertionAllowed) {
      allow = true
    } else if (editor.inVisualMode) {
      allow = injector.options(editor).selection != "old"
    }

    return moveCaretGotoLineLastEnd(editor, operatorArguments.count0, operatorArguments.count1 - 1, allow).toMotion()
  }
}

@CommandOrMotion(keys = ["<C-End>"], modes = [Mode.INSERT])
class MotionGotoLineLastEndInsertAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    var allow = false
    if (editor.isInsertionAllowed) {
      allow = true
    } else if (editor.inVisualMode) {
      allow = injector.options(editor).selection != "old"
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
    if (rawCount == 0) editor.normalizeLine(editor.lineCount() - 1) else line,
    pastEnd,
  )
}
