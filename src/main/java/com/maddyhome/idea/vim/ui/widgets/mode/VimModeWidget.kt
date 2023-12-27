/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.mode
import com.maddyhome.idea.vim.ui.widgets.mode.listeners.ModeWidgetFocusListener
import com.maddyhome.idea.vim.ui.widgets.mode.listeners.ModeWidgetModeListener
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JLabel

public class VimModeWidget(public val project: Project) : CustomStatusBarWidget {
  private companion object {
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
  private val useColors = injector.globalIjOptions().colorfulmodewidget
  private val label = JLabel("", JLabel.CENTER).apply {
    isOpaque = useColors
  }

  init {
    val mode = getFocusedEditor(project)?.vim?.mode
    updateLabel(mode)

    injector.listenersNotifier.apply {
      modeChangeListeners.add(ModeWidgetModeListener(this@VimModeWidget))
      myEditorListeners.add(ModeWidgetFocusListener(this@VimModeWidget))
    }
  }

  override fun ID(): String {
    return ModeWidgetFactory.ID
  }

  override fun getComponent(): JComponent {
    return label
  }

  public fun updateWidget() {
    val mode = getFocusedEditor(project)?.vim?.mode
    updateWidget(mode)
  }

  public fun updateWidget(mode: Mode?) {
    updateLabel(mode)
    val windowManager = WindowManager.getInstance()
    val statusBar = windowManager.getStatusBar(project)
    statusBar.updateWidget(ModeWidgetFactory.ID)
  }

  private fun updateLabel(mode: Mode?) {
    label.text = getModeText(mode)
    if (useColors) {
      label.foreground = getModeForeground(mode)
      label.background = getModeBackground(mode)
    }
  }

  private fun getFocusedEditor(project: Project): Editor? {
    val fileEditorManager = FileEditorManager.getInstance(project)
    return fileEditorManager.selectedTextEditor
  }

  private fun getModeText(mode: Mode?): String? {
    return when (mode) {
      Mode.INSERT -> INSERT
      Mode.REPLACE -> REPLACE
      is Mode.NORMAL -> NORMAL
      is Mode.CMD_LINE -> COMMAND
      is Mode.VISUAL -> getVisualModeText(mode)
      is Mode.SELECT -> getSelectModeText(mode)
      is Mode.OP_PENDING, null -> null
    }
  }

  private fun getVisualModeText(mode: Mode.VISUAL) = when (mode.selectionType) {
    SelectionType.CHARACTER_WISE -> VISUAL
    SelectionType.LINE_WISE -> VISUAL_LINE
    SelectionType.BLOCK_WISE -> VISUAL_BLOCK
  }

  private fun getSelectModeText(mode: Mode.SELECT) = when (mode.selectionType) {
    SelectionType.CHARACTER_WISE -> SELECT
    SelectionType.LINE_WISE -> SELECT_LINE
    SelectionType.BLOCK_WISE -> SELECT_BLOCK
  }

  private fun getModeForeground(mode: Mode?): Color {
    // TODO make it customizable via settings (color picker) or .idevimrc
    return label.parent?.background ?: Color.WHITE
  }

  private fun getModeBackground(mode: Mode?): Color? {
    // TODO make it customizable via settings (color picker) or .idevimrc
    return when (mode) {
      Mode.INSERT -> Color(134, 174, 213)
      Mode.REPLACE -> Color(213, 134, 134)
      is Mode.NORMAL -> Color(174, 213, 134)
      is Mode.CMD_LINE -> Color(174, 213, 134)
      is Mode.VISUAL -> Color(213, 174, 213)
      is Mode.SELECT -> Color(213, 174, 213)
      is Mode.OP_PENDING, null -> label.parent?.background
    }
  }
}