/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode.listeners

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.EditorListener
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.ui.widgets.VimWidgetListener
import com.maddyhome.idea.vim.ui.widgets.mode.ModeWidgetFactory
import com.maddyhome.idea.vim.ui.widgets.mode.VimModeWidget
import com.maddyhome.idea.vim.ui.widgets.mode.updateModeWidget

internal class ModeWidgetListener : ModeChangeListener, EditorListener, VimWidgetListener({ updateModeWidget() }) {
  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    val modeWidget = getWidget(editor) ?: return
    val editorMode = editor.mode
    if (editorMode !is Mode.OP_PENDING) {
      modeWidget.updateWidget(editorMode)
    }
  }

  private fun getWidget(editor: VimEditor): VimModeWidget? {
    val project = (editor as IjVimEditor).editor.project ?: return null
    return getWidget(project)
  }

  private fun getWidget(project: Project): VimModeWidget? {
    val statusBar = WindowManager.getInstance()?.getStatusBar(project) ?: return null
    return statusBar.getWidget(ModeWidgetFactory.ID) as? VimModeWidget
  }

  override fun created(editor: VimEditor) {
    updateModeWidget()
    val modeWidget = getWidget(editor) ?: return
    val mode = getFocusedEditorForProject(editor.ij.project)?.vim?.mode
    modeWidget.updateWidget(mode)
  }

  override fun released(editor: VimEditor) {
    updateModeWidget()
    val modeWidget = getWidget(editor) ?: return
    val focusedEditor = getFocusedEditorForProject(editor.ij.project)
    if (focusedEditor == null || focusedEditor == editor.ij) {
      modeWidget.updateWidget(null)
    }
  }

  override fun focusGained(editor: VimEditor) {
    val modeWidget = getWidget(editor) ?: return
    val mode = editor.mode
    modeWidget.updateWidget(mode)
  }

  override fun focusLost(editor: VimEditor) {
    val modeWidget = getWidget(editor) ?: return
    val mode = getFocusedEditorForProject(editor.ij.project)?.vim?.mode
    modeWidget.updateWidget(mode)
  }

  private fun getFocusedEditorForProject(editorProject: Project?): Editor? {
    if (editorProject == null) return null
    return recursionGuard.doPreventingRecursion(recursionKey, false) {
      val fileEditorManager = FileEditorManager.getInstance(editorProject)
      fileEditorManager.selectedTextEditor
    }
  }

  companion object {
    private val recursionGuard = RecursionManager.createGuard<Any>("IdeaVim.modeWidgetListener")
    private val recursionKey = Any()
  }
}