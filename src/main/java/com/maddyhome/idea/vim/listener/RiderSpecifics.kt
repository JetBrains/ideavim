package com.maddyhome.idea.vim.listener

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
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
  override fun beforeActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
    if (!VimPlugin.isEnabled()) return

    val hostEditor = dataContext.getData(CommonDataKeys.HOST_EDITOR)
    if (hostEditor != null) {
      editor = hostEditor
    }
  }

  override fun afterActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
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
