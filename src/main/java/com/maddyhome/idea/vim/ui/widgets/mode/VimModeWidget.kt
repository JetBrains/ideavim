/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.util.width
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import kotlin.math.max

class VimModeWidget(val project: Project) : CustomStatusBarWidget, VimStatusBarWidget {
  companion object {
    private const val INSERT = "INSERT"
    private const val NORMAL = "NORMAL"
    private const val INSERT_NORMAL = "(insert)"
    private const val INSERT_PENDING_PREFIX = "(insert) "
    private const val REPLACE = "REPLACE"
    private const val REPLACE_NORMAL = "(replace)"
    private const val REPLACE_PENDING_PREFIX = "(replace) "
    private const val COMMAND = "COMMAND"
    private const val VISUAL = "VISUAL"
    private const val VISUAL_LINE = "V-LINE"
    private const val VISUAL_BLOCK = "V-BLOCK"
    private const val SELECT = "SELECT"
    private const val SELECT_LINE = "S-LINE"
    private const val SELECT_BLOCK = "S-BLOCK"

    fun getModeText(mode: Mode?): String? {
      return when (mode) {
        Mode.INSERT -> INSERT
        Mode.REPLACE -> REPLACE
        is Mode.NORMAL -> getNormalModeText(mode)
        is Mode.CMD_LINE -> COMMAND
        is Mode.VISUAL -> getVisualModeText(mode)
        is Mode.SELECT -> getSelectModeText(mode)
        is Mode.OP_PENDING, null -> null
      }
    }

    private fun getNormalModeText(mode: Mode.NORMAL) = when {
      mode.isInsertPending -> INSERT_NORMAL
      mode.isReplacePending -> REPLACE_NORMAL
      else -> NORMAL
    }

    private fun getVisualModeText(mode: Mode.VISUAL): String {
      val prefix = when {
        mode.isInsertPending -> INSERT_PENDING_PREFIX
        mode.isReplacePending -> REPLACE_PENDING_PREFIX
        else -> ""
      }
      return prefix + when (mode.selectionType) {
        SelectionType.CHARACTER_WISE -> VISUAL
        SelectionType.LINE_WISE -> VISUAL_LINE
        SelectionType.BLOCK_WISE -> VISUAL_BLOCK
      }
    }

    private fun getSelectModeText(mode: Mode.SELECT): String {
      val prefix = when {
        mode.isInsertPending -> INSERT_PENDING_PREFIX
        mode.isReplacePending -> REPLACE_PENDING_PREFIX
        else -> ""
      }
      return prefix + when (mode.selectionType) {
        SelectionType.CHARACTER_WISE -> SELECT
        SelectionType.LINE_WISE -> SELECT_LINE
        SelectionType.BLOCK_WISE -> SELECT_BLOCK
      }
    }
  }

  private val label = JBLabelWiderThan(setOf(REPLACE)).apply { isOpaque = true }

  init {
    val mode = getFocusedEditor(project)?.vim?.mode
    updateLabel(mode)

    label.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val popup = ModeWidgetPopup.createPopup() ?: return
        val dimension = popup.content.preferredSize

        val widgetLocation = e.component.locationOnScreen
        popup.show(RelativePoint(Point(
          widgetLocation.x + e.component.width - dimension.width,
          widgetLocation.y - dimension.height,
        )))
      }
    })
  }

  override fun ID(): String {
    return ModeWidgetFactory.ID
  }

  override fun getComponent(): JComponent {
    return label
  }

  fun updateWidget() {
    val mode = getFocusedEditor(project)?.vim?.mode
    updateWidget(mode)
  }

  fun updateWidget(mode: Mode?) {
    updateLabel(mode)
    updateWidgetInStatusBar(ModeWidgetFactory.ID, project)
  }

  private fun updateLabel(mode: Mode?) {
    label.text = getModeText(mode)
    label.foreground = getModeForeground(mode)
    label.background = getModeBackground(mode)
  }

  private fun getFocusedEditor(project: Project): Editor? {
    val fileEditorManager = FileEditorManager.getInstance(project)
    return fileEditorManager.selectedTextEditor
  }

  private class JBLabelWiderThan(private val words: Collection<String>): JBLabel("", CENTER) {
    private val wordWidth: Int
      get() {
        val fontMetrics = getFontMetrics(font)
        return words.maxOfOrNull { fontMetrics.stringWidth(it) } ?: 0
      }

    override fun getMinimumSize(): Dimension {
      val minimumSize = super.getMinimumSize()
      return Dimension(max(minimumSize.width, wordWidth + insets.width), minimumSize.height)
    }

    override fun getPreferredSize(): Dimension {
      val preferredSize = super.getPreferredSize()
      return Dimension(max(preferredSize.width, wordWidth + insets.width), preferredSize.height)
    }

    override fun getMaximumSize(): Dimension {
      val maximumSize = super.getMaximumSize()
      return Dimension(max(maximumSize.width, wordWidth + insets.width), maximumSize.height)
    }
  }
}

fun updateModeWidget() {
  val factory = StatusBarWidgetFactory.EP_NAME.findExtension(ModeWidgetFactory::class.java) ?: return
  for (project in ProjectManager.getInstance().openProjects) {
    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateWidget(factory)
  }
}

fun repaintModeWidget() {
  for (project in ProjectManager.getInstance().openProjects) {
    val widgets = WindowManager.getInstance()?.getStatusBar(project)?.allWidgets ?: continue

    for (widget in widgets) {
      if (widget is VimModeWidget) {
        widget.updateWidget()
      }
    }
  }
}
