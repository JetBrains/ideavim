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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.EditorListener
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.mode
import com.maddyhome.idea.vim.ui.widgets.mode.VimModeWidget

internal class ModeWidgetFocusListener(private val modeWidget: VimModeWidget): EditorListener {
  override fun created(editor: VimEditor) {
    val mode = getFocusedEditorForProject(editor.ij.project)?.vim?.mode
    modeWidget.updateWidget(mode)
  }

  override fun released(editor: VimEditor) {
    val focusedEditor = getFocusedEditorForProject(editor.ij.project)
    if (focusedEditor == null || focusedEditor == editor.ij) {
      modeWidget.updateWidget(null)
    }
  }

  override fun focusGained(editor: VimEditor) {
    if (editor.ij.project != modeWidget.project) return
    val mode = editor.mode
    modeWidget.updateWidget(mode)
  }

  override fun focusLost(editor: VimEditor) {
    val mode = getFocusedEditorForProject(editor.ij.project)?.vim?.mode
    modeWidget.updateWidget(mode)
  }

  private fun getFocusedEditorForProject(editorProject: Project?): Editor? {
    if (editorProject != modeWidget.project) return null
    val fileEditorManager = FileEditorManager.getInstance(editorProject)
    return fileEditorManager.selectedTextEditor
  }
}