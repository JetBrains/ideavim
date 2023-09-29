/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action

import com.intellij.codeInsight.hint.HintManagerImpl.ActionToIgnore
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PerformWithDocumentsCommitted
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorMouseHoverPopupManager
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.project.DumbAware
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.MouseEvent

public class VimShowHoverInfoAction: AnAction(), ActionToIgnore, PopupAction, DumbAware, PerformWithDocumentsCommitted {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val dataContext = e.dataContext
    val editor = CommonDataKeys.EDITOR.getData(dataContext)
    if (editor == null) {
      e.presentation.isEnabledAndVisible = false
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val dataContext = e.dataContext
    val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return

    // Use the ShowHoverInfo action from the platform if it exists (added in 233). Before that, there wasn't an
    // overload of showInfoTooltip that would show both highlighting info and documentation - the API was hardcoded to
    // only show highlighting info (for ShowErrorDescription and the tooltips in GotoNextError). We can fake the hover
    // action by asking the popup manager to show the tooltip for a fake mouse event at the current caret location
    val nativeAction = injector.actionExecutor.getAction("ShowHoverInfo")
    if (nativeAction != null) {
      injector.actionExecutor.executeAction(editor.vim, nativeAction, dataContext.vim)
    }
    else {
      val editorMouseEvent = createFakeEditorMouseEvent(editor)
      EditorMouseHoverPopupManager.getInstance().showInfoTooltip(editorMouseEvent)
    }
  }

  private fun createFakeEditorMouseEvent(editor: Editor): EditorMouseEvent {
    val xy = editor.offsetToXY(editor.caretModel.offset)
    val mouseEvent =
      MouseEvent(editor.component, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, xy.x, xy.y, 0, false)
    val editorMouseEvent = EditorMouseEvent(
      editor,
      mouseEvent,
      EditorMouseEventArea.EDITING_AREA,
      editor.caretModel.offset,
      editor.caretModel.logicalPosition,
      editor.caretModel.visualPosition,
      true,
      null,
      null,
      null
    )
    return editorMouseEvent
  }
}