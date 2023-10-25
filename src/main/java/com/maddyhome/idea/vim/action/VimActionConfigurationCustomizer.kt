/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action

import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PerformWithDocumentsCommitted
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.actionSystem.impl.ActionConfigurationCustomizer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorMouseHoverPopupManager
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.project.DumbAware
import java.awt.event.MouseEvent

// [VERSION UPDATE] 233+ Remove class
// The ShowHoverInfo action is built into the platform (using a nicer EditorMouseHoverPopupManager API)
public class VimActionConfigurationCustomizer : ActionConfigurationCustomizer {
  public override fun customize(actionManager: ActionManager) {
    // If the ShowHoverInfo action doesn't exist in the platform, add our own implementation
    if (actionManager.getAction("ShowHoverInfo") == null) {
      actionManager.registerAction("ShowHoverInfo", VimShowHoverInfoAction())
    }
  }

  private class VimShowHoverInfoAction : AnAction(), HintManagerImpl.ActionToIgnore, PopupAction, DumbAware,
    PerformWithDocumentsCommitted {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
      val dataContext = e.dataContext
      val editor = CommonDataKeys.EDITOR.getData(dataContext)
      if (editor == null) {
        e.presentation.isEnabledAndVisible = false
      }
    }

    override fun actionPerformed(e: AnActionEvent) {
      val editor = CommonDataKeys.EDITOR.getData(e.dataContext) ?: return

      val editorMouseEvent = createFakeEditorMouseEvent(editor)
      EditorMouseHoverPopupManager.getInstance().showInfoTooltip(editorMouseEvent)
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
}
