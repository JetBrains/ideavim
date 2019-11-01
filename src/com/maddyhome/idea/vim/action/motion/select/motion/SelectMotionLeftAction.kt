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

package com.maddyhome.idea.vim.action.motion.select.motion

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.option.KeyModelOptionData
import com.maddyhome.idea.vim.option.OptionsManager

/**
 * @author Alex Plate
 */

class SelectMotionLeftAction : MotionActionHandler.ForEachCaret() {

  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
    val keymodel = OptionsManager.keymodel
    if (KeyModelOptionData.stopsel in keymodel || KeyModelOptionData.stopselect in keymodel) {
      logger.info("Keymodel option has stopselect. Exiting select mode")
      val startSelection = caret.selectionStart
      val endSelection = caret.selectionEnd
      editor.exitSelectMode(false)
      if (editor.isTemplateActive()) {
        logger.info("Template is active. Activate insert mode")
        VimPlugin.getChange().insertBeforeCursor(editor, context)
        if (caret.offset in startSelection..endSelection) {
          return startSelection
        }
      }
      // No return statement, perform motion to left
    }
    return VimPlugin.getMotion().moveCaretHorizontal(editor, caret, -count, false)
  }

  companion object {
    private val logger = Logger.getInstance(SelectMotionLeftAction::class.java)
  }
}
