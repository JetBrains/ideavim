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

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.group.visual.moveCaretOneCharLeftFromSelectionEnd
import com.maddyhome.idea.vim.helper.getTopLevelEditor

class RiderActionListener : AnActionListener {

  private var editor: Editor? = null
  override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
    if (!VimPlugin.isEnabled()) return

    val hostEditor = event.dataContext.getData(CommonDataKeys.HOST_EDITOR)
    if (hostEditor != null) {
      editor = hostEditor
    }
  }

  override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
    if (!VimPlugin.isEnabled()) return

    //region Extend Selection for Rider
    when (ActionManager.getInstance().getId(action)) {
      IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET, IdeActions.ACTION_EDITOR_UNSELECT_WORD_AT_CARET -> {
        // Rider moves caret to the end of selection
        editor?.caretModel?.addCaretListener(object : CaretListener {
          override fun caretPositionChanged(event: CaretEvent) {
            val eventEditor = event.editor.getTopLevelEditor()
            val predictedMode =
              IdeaSelectionControl.predictMode(eventEditor, VimListenerManager.SelectionSource.OTHER)
            moveCaretOneCharLeftFromSelectionEnd(eventEditor, predictedMode)
            eventEditor.caretModel.removeCaretListener(this)
          }
        })
      }
    }
    //endregion
  }
}
