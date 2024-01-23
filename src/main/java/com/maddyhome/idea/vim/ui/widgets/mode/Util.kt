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

public fun getModeBackground(mode: Mode?): Color {
  val isLight = !LafManager.getInstance().currentUIThemeLookAndFeel.isDark
  val keyPostfix = if (isLight) "_light" else "_dark"
  if (injector.variableService.getVimVariable("widget_mode_is_full_customization$keyPostfix")?.asBoolean() != true) {
    val themeString = injector.variableService.getVimVariable("widget_mode_theme$keyPostfix")?.asString() ?: ""
    val theme = ModeWidgetTheme.parseString(themeString) ?: ModeWidgetTheme.getDefaultTheme()
    when (theme) {
      ModeWidgetTheme.TEST -> {
        return when (mode) {
          Mode.INSERT -> Color.decode("#D08770")
          Mode.REPLACE -> Color.decode("#EBCB8B")
          is Mode.NORMAL -> Color.decode("#BF616A")
          is Mode.CMD_LINE -> Color.decode("#A3BE8C")
          is Mode.VISUAL -> Color.decode("#B48EAD")
          is Mode.SELECT -> Color.decode("#B48EAD")
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
}

public fun getModeForeground(mode: Mode?): Color {
  val isLight = !LafManager.getInstance().currentUIThemeLookAndFeel.isDark
  val keyPostfix = if (isLight) "_light" else "_dark"
  if (injector.variableService.getVimVariable("widget_mode_is_full_customization$keyPostfix")?.asBoolean() != true) {
    val themeString = injector.variableService.getVimVariable("widget_mode_theme$keyPostfix")?.asString() ?: ""
    val theme = ModeWidgetTheme.parseString(themeString) ?: ModeWidgetTheme.getDefaultTheme()
    return when (theme) {
      ModeWidgetTheme.TEST -> Color.decode("#2E3440")
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

}
