/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.util.width
import com.intellij.util.ui.UIUtil
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.mode
import com.maddyhome.idea.vim.ui.widgets.mode.listeners.ModeWidgetFocusListener
import com.maddyhome.idea.vim.ui.widgets.mode.listeners.ModeWidgetModeListener
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import kotlin.math.max

public class VimModeWidget(public val project: Project) : CustomStatusBarWidget, VimStatusBarWidget {
  private companion object {
    private const val INSERT = "INSERT"
    private const val NORMAL = "NORMAL"
    private const val REPLACE = "REPLACE"
    private const val COMMAND = "COMMAND"
    private const val VISUAL = "VISUAL"
    private const val VISUAL_LINE = "V-LINE"
    private const val VISUAL_BLOCK = "V-BLOCK"
    private const val SELECT = "SELECT"
    private const val SELECT_LINE = "S-LINE"
    private const val SELECT_BLOCK = "S-BLOCK"
  }
  private val useColors = injector.globalIjOptions().colorfulmodewidget
  private val label = JBLabelWiderThan(setOf(REPLACE)).apply {
    isOpaque = useColors
  }

  init {
    val mode = getFocusedEditor(project)?.vim?.mode
    updateLabel(mode)

    injector.listenersNotifier.apply {
      modeChangeListeners.add(ModeWidgetModeListener(this@VimModeWidget))
      myEditorListeners.add(ModeWidgetFocusListener(this@VimModeWidget))
    }

    label.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val popup = ModeWidgetPopup.createPopup()
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

  public fun updateWidget() {
    val mode = getFocusedEditor(project)?.vim?.mode
    updateWidget(mode)
  }

  public fun updateWidget(mode: Mode?) {
    updateLabel(mode)
    updateWidgetInStatusBar(ModeWidgetFactory.ID, project)
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
    val keyPostfix = if (LafManager.getInstance().currentUIThemeLookAndFeel.isDark) "_dark" else "_light"
    val colorString = when (mode) {
      Mode.INSERT -> injector.variableService.getVimVariable("widget_mode_insert_foreground$keyPostfix")
      Mode.REPLACE -> injector.variableService.getVimVariable("widget_mode_replace_foreground$keyPostfix")
      is Mode.NORMAL -> injector.variableService.getVimVariable("widget_mode_normal_foreground$keyPostfix")
      is Mode.CMD_LINE -> injector.variableService.getVimVariable("widget_mode_command_foreground$keyPostfix")
      is Mode.VISUAL -> {
        val visualModeBackground = injector.variableService.getVimVariable("widget_mode_visual_foreground$keyPostfix")
        when (mode.selectionType) {
          SelectionType.CHARACTER_WISE -> visualModeBackground
          SelectionType.LINE_WISE -> injector.variableService.getVimVariable("widget_mode_visual_line_foreground$keyPostfix") ?: visualModeBackground
          SelectionType.BLOCK_WISE -> injector.variableService.getVimVariable("widget_mode_visual_block_foreground$keyPostfix") ?: visualModeBackground
        }
      }
      is Mode.SELECT -> {
        val selectModeBackground = injector.variableService.getVimVariable("widget_mode_select_foreground$keyPostfix")
        when (mode.selectionType) {
          SelectionType.CHARACTER_WISE -> selectModeBackground
          SelectionType.LINE_WISE -> injector.variableService.getVimVariable("widget_mode_select_line_foreground$keyPostfix") ?: selectModeBackground
          SelectionType.BLOCK_WISE -> injector.variableService.getVimVariable("widget_mode_select_block_foreground$keyPostfix") ?: selectModeBackground
        }
      }
      is Mode.OP_PENDING, null -> null
    }?.asString()
    val defaultColor = UIUtil.getLabelForeground()
    val color = when (colorString) {
      "v:status_bar_bg" -> UIUtil.getPanelBackground()
      "v:status_bar_fg" -> UIUtil.getLabelForeground()
      else -> {
        if (colorString == null) {
          defaultColor
        } else {
          try { Color.decode(colorString) } catch (e: Exception) { defaultColor }
        }
      }
    }
    return color
  }

  private fun getModeBackground(mode: Mode?): Color {
    val keyPostfix = if (LafManager.getInstance().currentUIThemeLookAndFeel.isDark) "_dark" else "_light"
    val colorString = when (mode) {
      Mode.INSERT -> injector.variableService.getVimVariable("widget_mode_insert_background$keyPostfix")
      Mode.REPLACE -> injector.variableService.getVimVariable("widget_mode_replace_background$keyPostfix")
      is Mode.NORMAL -> injector.variableService.getVimVariable("widget_mode_normal_background$keyPostfix")
      is Mode.CMD_LINE -> injector.variableService.getVimVariable("widget_mode_command_background$keyPostfix")
      is Mode.VISUAL -> {
        val visualModeBackground = injector.variableService.getVimVariable("widget_mode_visual_background$keyPostfix")
        when (mode.selectionType) {
          SelectionType.CHARACTER_WISE -> visualModeBackground
          SelectionType.LINE_WISE -> injector.variableService.getVimVariable("widget_mode_visual_line_background$keyPostfix") ?: visualModeBackground
          SelectionType.BLOCK_WISE -> injector.variableService.getVimVariable("widget_mode_visual_block_background$keyPostfix") ?: visualModeBackground
        }
      }
      is Mode.SELECT -> {
        val selectModeBackground = injector.variableService.getVimVariable("widget_mode_select_background$keyPostfix")
        when (mode.selectionType) {
          SelectionType.CHARACTER_WISE -> selectModeBackground
          SelectionType.LINE_WISE -> injector.variableService.getVimVariable("widget_mode_select_line_background$keyPostfix") ?: selectModeBackground
          SelectionType.BLOCK_WISE -> injector.variableService.getVimVariable("widget_mode_select_block_background$keyPostfix") ?: selectModeBackground
        }
      }
      is Mode.OP_PENDING, null -> null
    }?.asString()
    val defaultColor = UIUtil.getPanelBackground()
    val color = when (colorString) {
      "v:status_bar_bg" -> UIUtil.getPanelBackground()
      "v:status_bar_fg" -> UIUtil.getLabelForeground()
      else -> {
        if (colorString == null) {
          defaultColor
        } else {
          try { Color.decode(colorString) } catch (e: Exception) { defaultColor }
        }
      }
    }
    return color
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

public fun updateModeWidget() {
  val factory = StatusBarWidgetFactory.EP_NAME.findExtension(ModeWidgetFactory::class.java) ?: return
  for (project in ProjectManager.getInstance().openProjects) {
    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateWidget(factory)
  }
}