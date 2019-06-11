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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.action.motion.select.motion

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.option.Options
import com.maddyhome.idea.vim.option.Options.KEYMODEL
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

class SelectMotionRightAction : VimCommandAction() {
  override fun makeActionHandler() = object : MotionActionHandler.ForEachCaret() {
    override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
      val keymodel = Options.getInstance().getListOption(KEYMODEL)
      if (keymodel?.contains("stopsel") == true || keymodel?.contains("stopselect") == true) {
        VimPlugin.getVisualMotion().exitSelectMode(editor, false)
        TemplateManager.getInstance(editor.project)
          .getActiveTemplate(editor)?.run { VimPlugin.getChange().insertBeforeCursor(editor, context) }
        return caret.offset
      }
      return VimPlugin.getMotion().moveCaretHorizontal(editor, caret, count, false)
    }
  }

  override val mappingModes: MutableSet<MappingMode> = MappingMode.S

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("<Right>")

  override val type: Command.Type = Command.Type.MOTION
}