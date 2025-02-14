/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere

internal class RiderActionListener : AnActionListener {

  private var editor: Editor? = null
  override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
    if (VimPlugin.isNotEnabled()) return

    val hostEditor = event.dataContext.getData(CommonDataKeys.HOST_EDITOR)
    if (hostEditor != null) {
      editor = hostEditor
    }
  }

  override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
    if (VimPlugin.isNotEnabled()) return

    //region Extend Selection for Rider
    when (ActionManager.getInstance().getId(action)) {
      IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET, IdeActions.ACTION_EDITOR_UNSELECT_WORD_AT_CARET -> {
        // Rider moves caret to the end of selection
        editor?.caretModel?.addCaretListener(object : CaretListener {
          override fun caretPositionChanged(event: CaretEvent) {
            val eventEditor = event.editor.getTopLevelEditor()
            if (!eventEditor.isIdeaVimDisabledHere) {
              val predictedMode =
                IdeaSelectionControl.predictMode(eventEditor, VimListenerManager.SelectionSource.OTHER)
              moveCaretOneCharLeftFromSelectionEnd(eventEditor, predictedMode)
            }
            eventEditor.caretModel.removeCaretListener(this)
          }
        })
      }
    }
    //endregion
  }
}
