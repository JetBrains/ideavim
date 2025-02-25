/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode

import com.intellij.ide.ui.LafManager
import com.intellij.util.ui.UIUtil
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.awt.Color

fun getModeBackground(mode: Mode?): Color {
  val isLight = !(LafManager.getInstance()?.currentUIThemeLookAndFeel?.isDark ?: false)
  val keyPostfix = if (isLight) "_light" else "_dark"
  if (injector.variableService.getVimVariable("widget_mode_is_full_customization$keyPostfix")?.toVimNumber()?.booleanValue != true) {
    val themeString = injector.variableService.getVimVariable("widget_mode_theme$keyPostfix")?.asString() ?: ""
    val theme = ModeWidgetTheme.parseString(themeString) ?: ModeWidgetTheme.getDefaultTheme()
    when (theme) {
      ModeWidgetTheme.TERM -> {
        return when (mode) {
          Mode.INSERT -> Color.decode("#F4BF75")
          Mode.REPLACE -> Color.decode("#AC4242")
          is Mode.NORMAL, is Mode.CMD_LINE -> Color.decode("#90A959")
          is Mode.VISUAL, is Mode.SELECT -> Color.decode("#6A9FB5")
          is Mode.OP_PENDING, null -> UIUtil.getPanelBackground()
        }
      }

      ModeWidgetTheme.DRACULA -> {
        return when (mode) {
          Mode.INSERT -> Color.decode("#50FA7B")
          Mode.REPLACE -> Color.decode("#FF5555")
          is Mode.NORMAL -> Color.decode("#BD93F9")
          is Mode.CMD_LINE -> Color.decode("#FFB86C")
          is Mode.VISUAL, is Mode.SELECT -> Color.decode("#F1FA8C")
          is Mode.OP_PENDING, null -> UIUtil.getPanelBackground()
        }
      }

      ModeWidgetTheme.COLORLESS -> {
        return UIUtil.getPanelBackground()
      }
    }
  } else {
    val colorString = when (mode) {
      Mode.INSERT -> injector.variableService.getVimVariable("widget_mode_insert_background$keyPostfix")
      Mode.REPLACE -> injector.variableService.getVimVariable("widget_mode_replace_background$keyPostfix")
      is Mode.NORMAL -> injector.variableService.getVimVariable("widget_mode_normal_background$keyPostfix")
      is Mode.CMD_LINE -> injector.variableService.getVimVariable("widget_mode_command_background$keyPostfix")
      is Mode.VISUAL -> {
        val visualModeBackground = injector.variableService.getVimVariable("widget_mode_visual_background$keyPostfix")
        when (mode.selectionType) {
          SelectionType.CHARACTER_WISE -> visualModeBackground
          SelectionType.LINE_WISE -> injector.variableService.getVimVariable("widget_mode_visual_line_background$keyPostfix")
            ?: visualModeBackground

          SelectionType.BLOCK_WISE -> injector.variableService.getVimVariable("widget_mode_visual_block_background$keyPostfix")
            ?: visualModeBackground
        }
      }

      is Mode.SELECT -> {
        val selectModeBackground = injector.variableService.getVimVariable("widget_mode_select_background$keyPostfix")
        when (mode.selectionType) {
          SelectionType.CHARACTER_WISE -> selectModeBackground
          SelectionType.LINE_WISE -> injector.variableService.getVimVariable("widget_mode_select_line_background$keyPostfix")
            ?: selectModeBackground

          SelectionType.BLOCK_WISE -> injector.variableService.getVimVariable("widget_mode_select_block_background$keyPostfix")
            ?: selectModeBackground
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
          try {
            Color.decode(colorString)
          } catch (e: Exception) {
            defaultColor
          }
        }
      }
    }
    return color
  }
}

fun getModeForeground(mode: Mode?): Color {
  val isLight = !(LafManager.getInstance()?.currentUIThemeLookAndFeel?.isDark ?: false)
  val keyPostfix = if (isLight) "_light" else "_dark"
  if (injector.variableService.getVimVariable("widget_mode_is_full_customization$keyPostfix")?.toVimNumber()?.booleanValue != true) {
    val themeString = injector.variableService.getVimVariable("widget_mode_theme$keyPostfix")?.asString() ?: ""
    val theme = ModeWidgetTheme.parseString(themeString) ?: ModeWidgetTheme.getDefaultTheme()
    return when (theme) {
      ModeWidgetTheme.TERM -> if (isLight) Color.WHITE else Color.BLACK
      ModeWidgetTheme.DRACULA -> Color.BLACK
      ModeWidgetTheme.COLORLESS -> UIUtil.getLabelForeground()
    }
  } else {
    val colorString = when (mode) {
      Mode.INSERT -> injector.variableService.getVimVariable("widget_mode_insert_foreground$keyPostfix")
      Mode.REPLACE -> injector.variableService.getVimVariable("widget_mode_replace_foreground$keyPostfix")
      is Mode.NORMAL -> injector.variableService.getVimVariable("widget_mode_normal_foreground$keyPostfix")
      is Mode.CMD_LINE -> injector.variableService.getVimVariable("widget_mode_command_foreground$keyPostfix")
      is Mode.VISUAL -> {
        val visualModeBackground = injector.variableService.getVimVariable("widget_mode_visual_foreground$keyPostfix")
        when (mode.selectionType) {
          SelectionType.CHARACTER_WISE -> visualModeBackground
          SelectionType.LINE_WISE -> injector.variableService.getVimVariable("widget_mode_visual_line_foreground$keyPostfix")
            ?: visualModeBackground

          SelectionType.BLOCK_WISE -> injector.variableService.getVimVariable("widget_mode_visual_block_foreground$keyPostfix")
            ?: visualModeBackground
        }
      }

      is Mode.SELECT -> {
        val selectModeBackground = injector.variableService.getVimVariable("widget_mode_select_foreground$keyPostfix")
        when (mode.selectionType) {
          SelectionType.CHARACTER_WISE -> selectModeBackground
          SelectionType.LINE_WISE -> injector.variableService.getVimVariable("widget_mode_select_line_foreground$keyPostfix")
            ?: selectModeBackground

          SelectionType.BLOCK_WISE -> injector.variableService.getVimVariable("widget_mode_select_block_foreground$keyPostfix")
            ?: selectModeBackground
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
          try {
            Color.decode(colorString)
          } catch (e: Exception) {
            defaultColor
          }
        }
      }
    }
    return color
  }
}
