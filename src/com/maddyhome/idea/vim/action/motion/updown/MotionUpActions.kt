/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

sealed class MotionUpBase : MotionActionHandler.ForEachCaret() {
  private var col: Int = 0

  override fun preOffsetComputation(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
    col = EditorHelper.prepareLastColumn(editor, caret)
    return true
  }

  override fun postMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {
    EditorHelper.updateLastColumn(editor, caret, col)
  }
}

open class MotionUpAction : MotionUpBase() {
  override val motionType: MotionType = MotionType.INCLUSIVE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_MOT_LINEWISE)

  override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
    return VimPlugin.getMotion().moveCaretVertical(editor, caret, -count)
  }
}

class MotionUpCtrlPAction : MotionUpAction() {
  override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
    val activeLookup = LookupManager.getActiveLookup(editor)
    return if (activeLookup != null) {
      val primaryCaret = editor.caretModel.primaryCaret
      if (caret == primaryCaret) {
        IdeEventQueue.getInstance().flushDelayedKeyEvents()
        EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP).execute(editor, primaryCaret, context)
      }
      caret.offset
    } else {
      super.getOffset(editor, caret, context, count, rawCount, argument)
    }
  }
}

class MotionUpNotLineWiseAction : MotionUpBase() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
    return VimPlugin.getMotion().moveCaretVertical(editor, caret, -count)
  }
}
