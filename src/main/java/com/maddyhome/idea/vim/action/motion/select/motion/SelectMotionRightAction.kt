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

package com.maddyhome.idea.vim.action.motion.select.motion

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.services.OptionConstants
import com.maddyhome.idea.vim.vimscript.services.OptionService

/**
 * @author Alex Plate
 */

class SelectMotionRightAction : MotionActionHandler.ForEachCaret() {

  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: Editor,
    caret: Caret,
    context: DataContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val keymodel = (VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, OptionConstants.keymodelName) as VimString).value
    if (OptionConstants.keymodel_stopsel in keymodel || OptionConstants.keymodel_stopselect in keymodel) {
      logger.debug("Keymodel option has stopselect. Exiting select mode")
      val startSelection = caret.selectionStart
      val endSelection = caret.selectionEnd
      editor.exitSelectMode(false)
      if (editor.isTemplateActive()) {
        logger.debug("Template is active. Activate insert mode")
        VimPlugin.getChange().insertBeforeCursor(editor, context)
        if (caret.offset in startSelection..endSelection) {
          return endSelection.toMotion()
        }
      }
      return caret.offset.toMotion()
    }
    return VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, operatorArguments.count1, false).toMotionOrError()
  }

  companion object {
    private val logger = Logger.getInstance(SelectMotionRightAction::class.java)
  }
}
