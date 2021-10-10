/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion

class MotionDownFirstNonSpaceAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: Editor,
    caret: Caret,
    context: DataContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): Motion {
    return VimPlugin.getMotion().moveCaretToLineStartSkipLeadingOffset(editor, caret, count).toMotion()
  }
}

class EnterNormalAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: Editor,
    caret: Caret,
    context: DataContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): Motion {
    val templateState = TemplateManagerImpl.getTemplateState(editor)
    return if (templateState != null) {
      KeyHandler.executeAction(IdeActions.ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE, context)
      Motion.NoMotion
    } else {
      VimPlugin.getMotion().moveCaretToLineStartSkipLeadingOffset(editor, caret, count).toMotion()
    }
  }
}
