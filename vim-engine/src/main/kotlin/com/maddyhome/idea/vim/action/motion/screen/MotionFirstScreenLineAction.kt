/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.screen

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
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
import java.util.*

/*
                                                *H*
H                       To line [count] from top (Home) of window (default:
                        first line on the window) on the first non-blank
                        character |linewise|.  See also 'startofline' option.
                        Cursor is adjusted for 'scrolloff' option, unless an
                        operator is pending, in which case the text may
                        scroll.  E.g. "yH" yanks from the first visible line
                        until the cursor line (inclusive).
 */
abstract class MotionFirstScreenLineActionBase(private val operatorPending: Boolean) :
  MotionActionHandler.ForEachCaret() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    // Only apply scrolloff for NX motions. For op pending, use the actual first line and apply scrolloff after.
    // E.g. yH will yank from first visible line to current line, but it also moves the caret to the first visible line.
    // This is inside scrolloff, so Vim scrolls
    return injector.motion.moveCaretToFirstDisplayLine(editor, caret, operatorArguments.count1, !operatorPending)
      .toMotion()
  }
}

@CommandOrMotion(keys = ["H"], modes = [Mode.NORMAL, Mode.VISUAL])
class MotionFirstScreenLineAction : MotionFirstScreenLineActionBase(false)

@CommandOrMotion(keys = ["H"], modes = [Mode.OP_PENDING])
class MotionOpPendingFirstScreenLineAction : MotionFirstScreenLineActionBase(true)
