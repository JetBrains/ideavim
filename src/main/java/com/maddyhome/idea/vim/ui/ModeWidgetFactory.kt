/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.EditorListener
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.mode
import java.awt.Component

private const val ID = "IdeaVim::Mode"

public class ModeWidgetFactory : StatusBarWidgetFactory {
  private companion object {
    private const val NO_MODE = "" // for cases were no editors are focused
    private const val INSERT = "INSERT"
    private const val NORMAL = "NORMAL"
    private const val REPLACE = "REPLACE"
    private const val COMMAND = "COMMAND"
    private const val VISUAL = "VISUAL"
    private const val VISUAL_LINE = "VISUAL LINE"
    private const val VISUAL_BLOCK = "VISUAL BLOCK"
    private const val SELECT = "SELECT"
    private const val SELECT_LINE = "SELECT LINE"
    private const val SELECT_BLOCK = "SELECT BLOCK"
  }
  private lateinit var currentMode: String

  private val modeListener = object : ModeChangeListener {
    override fun modeChanged(editor: VimEditor, oldMode: Mode) {
      val editorMode = editor.mode
      if (editor.mode !is Mode.OP_PENDING) {
        currentMode = getModeString(editorMode)
      }
      updateWidget()
    }
  }

  private val focusListener = object : EditorListener {
    override fun created(editor: VimEditor) {
      val project = editor.ij.project ?: return
      val mode = getFocusedEditor(project)?.vim?.mode
      currentMode = getModeString(mode)
      updateWidget()
    }

    override fun released(editor: VimEditor) {
      val project = editor.ij.project ?: return
      val focusedEditor = getFocusedEditor(project)
      if (focusedEditor == null || focusedEditor == editor.ij) {
        currentMode = NO_MODE
      }
      updateWidget()
    }

    override fun focusGained(editor: VimEditor) {
      currentMode = getModeString(editor.mode)
      updateWidget()
    }

    override fun focusLost(editor: VimEditor) {
      val project = editor.ij.project ?: return
      val mode = getFocusedEditor(project)?.vim?.mode
      currentMode = getModeString(mode)
      updateWidget()
    }
  }

  override fun getId(): String {
    return ID
  }

  override fun getDisplayName(): String {
    return "IdeaVim Mode Widget"
  }

  override fun createWidget(project: Project): StatusBarWidget {
    val mode = getFocusedEditor(project)?.vim?.mode
    currentMode = getModeString(mode)

    injector.listenersNotifier.apply {
      modeChangeListeners.add(modeListener)
      myEditorListeners.add(focusListener)
    }
    return VimModeWidget()
  }

  override fun isAvailable(project: Project): Boolean {
    return VimPlugin.isEnabled() && injector.globalIjOptions().showmodewidget
  }

  private fun updateWidget() {
    val windowManager = WindowManager.getInstance()
    ProjectManager.getInstance().openProjects.forEach {
      val statusBar = windowManager.getStatusBar(it)
      statusBar.updateWidget(ID)
    }
  }

  private fun getFocusedEditor(project: Project): Editor? {
    val fileEditorManager = FileEditorManager.getInstance(project)
    return fileEditorManager.selectedTextEditor
  }

  private fun getModeString(mode: Mode?): String {
    return when (mode) {
      Mode.INSERT -> INSERT
      Mode.REPLACE -> REPLACE
      is Mode.NORMAL -> NORMAL
      is Mode.CMD_LINE -> COMMAND
      is Mode.VISUAL -> getVisualMode(mode)
      is Mode.SELECT -> getSelectMode(mode)
      is Mode.OP_PENDING, null -> NO_MODE // method should not be called for OP_PENDING when possible
    }
  }

  private fun getVisualMode(mode: Mode.VISUAL) = when (mode.selectionType) {
    SelectionType.CHARACTER_WISE -> VISUAL
    SelectionType.LINE_WISE -> VISUAL_LINE
    SelectionType.BLOCK_WISE -> VISUAL_BLOCK
  }

  private fun getSelectMode(mode: Mode.SELECT) = when (mode.selectionType) {
    SelectionType.CHARACTER_WISE -> SELECT
    SelectionType.LINE_WISE -> SELECT_LINE
    SelectionType.BLOCK_WISE -> SELECT_BLOCK
  }

  private inner class VimModeWidget : StatusBarWidget {
    override fun ID(): String {
      return ID
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation? {
      return VimModeWidgetPresentation()
    }
  }

  private inner class VimModeWidgetPresentation : StatusBarWidget.TextPresentation {
    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getText(): String {
      return currentMode
    }

    override fun getTooltipText(): String {
      return "Current Vim Mode: ${getText()}"
    }
  }
}

internal object ModeWidgetListener : GlobalOptionChangeListener {
  override fun onGlobalOptionChanged() {
    val factory = StatusBarWidgetFactory.EP_NAME.findExtension(ModeWidgetFactory::class.java) ?: return
    for (project in ProjectManager.getInstance().openProjects) {
      val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
      statusBarWidgetsManager.updateWidget(factory)
    }
  }
}