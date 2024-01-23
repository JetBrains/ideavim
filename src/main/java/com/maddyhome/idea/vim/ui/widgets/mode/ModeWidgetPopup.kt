/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


public class ModeWidgetPopup : AnAction() {
  public override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val popup = createPopup() ?: return
    popup.showCenteredInCurrentWindow(project)
  }

  public companion object {
    @Volatile
    private var currentPopup: JBPopup? = null

    public fun createPopup(): JBPopup? {
      synchronized(this) {
        if (currentPopup?.isDisposed == false) return null
        val mainPanel = JPanel(BorderLayout())
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))

        val applyButton = JButton("Apply").apply { isEnabled = false }
        val cancelButton = JButton("Close")
        buttonPanel.add(applyButton)
        buttonPanel.add(cancelButton)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)

        val tabbedPane = JBTabbedPane()
        val lightThemeSettings = createPanel(getWidgetThemeColors(true))
        val darkThemeSettings = createPanel(getWidgetThemeColors(false))
        tabbedPane.addTab(MessageHelper.getMessage("widget.mode.popup.tab.light"), lightThemeSettings.addScrollPane())
        tabbedPane.addTab(MessageHelper.getMessage("widget.mode.popup.tab.dark"), darkThemeSettings.addScrollPane())
        tabbedPane.preferredSize = Dimension(300, 600)
        for (i in 0 until tabbedPane.tabCount) {
          val label = JLabel(tabbedPane.getTitleAt(i), JLabel.CENTER)
          label.preferredSize = Dimension(126, tabbedPane.getTabComponentAt(i).preferredSize.height)
          tabbedPane.setTabComponentAt(i, label)
        }
        tabbedPane.selectedIndex = if (LafManager.getInstance().currentUIThemeLookAndFeel.isDark) 1 else 0
        mainPanel.add(tabbedPane, BorderLayout.CENTER)

        val popupContent = ContentFactory.getInstance().createContent(mainPanel, "", false).component
        val popup = JBPopupFactory.getInstance()
          .createComponentPopupBuilder(popupContent, popupContent)
          .setTitle(MessageHelper.getMessage("widget.mode.popup.title"))
          .setMovable(true)
          .setRequestFocus(true)
          .setCancelOnClickOutside(false)
          .setCancelKeyEnabled(false)
          .createPopup()

        applyButton.addActionListener {
          lightThemeSettings.apply()
          darkThemeSettings.apply()
          repaintModeWidget()
        }

        cancelButton.addActionListener {
          popup.cancel()
        }

        val alarm = Alarm(popup)
        fun updateApplyButtonVisibility() {
          alarm.addRequest({
            applyButton.isEnabled = lightThemeSettings.isModified() || darkThemeSettings.isModified()
            updateApplyButtonVisibility()
          }, 500L)
        }
        updateApplyButtonVisibility()

        currentPopup = popup
        return currentPopup
      }
    }

    private fun getWidgetThemeColors(isLight: Boolean): ModeColors {
      val keyPostfix = if (isLight) "_light" else "_dark"
      return ModeColors(
         "widget_mode_normal_background$keyPostfix",
         "widget_mode_normal_foreground$keyPostfix",
         "widget_mode_insert_background$keyPostfix",
         "widget_mode_insert_foreground$keyPostfix",
         "widget_mode_replace_background$keyPostfix",
         "widget_mode_replace_foreground$keyPostfix",
         "widget_mode_command_background$keyPostfix",
         "widget_mode_command_foreground$keyPostfix",
         "widget_mode_visual_background$keyPostfix",
         "widget_mode_visual_foreground$keyPostfix",
         "widget_mode_visual_line_background$keyPostfix",
         "widget_mode_visual_line_foreground$keyPostfix",
         "widget_mode_visual_block_background$keyPostfix",
         "widget_mode_visual_block_foreground$keyPostfix",
         "widget_mode_select_background$keyPostfix",
         "widget_mode_select_foreground$keyPostfix",
         "widget_mode_select_line_background$keyPostfix",
         "widget_mode_select_line_foreground$keyPostfix",
         "widget_mode_select_block_background$keyPostfix",
         "widget_mode_select_block_foreground$keyPostfix",
      )
    }

    private fun createPanel(modeColors: ModeColors): DialogPanel {
      val panel = panel {
        row { text(MessageHelper.getMessage("widget.mode.popup.color.instruction")) }

        group(MessageHelper.getMessage("widget.mode.popup.group.normal.title")) {
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.background"))
            textField().bindText(modeColors::normalBg)
          }.layout(RowLayout.PARENT_GRID)
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
            textField().bindText(modeColors::normalFg)
          }.layout(RowLayout.PARENT_GRID)
        }

        group(MessageHelper.getMessage("widget.mode.popup.group.insert.title")) {
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.background"))
            textField().bindText(modeColors::insertBg)
          }.layout(RowLayout.PARENT_GRID)
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
            textField().bindText(modeColors::insertFg)
          }.layout(RowLayout.PARENT_GRID)
        }

        group(MessageHelper.getMessage("widget.mode.popup.group.replace.title")) {
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.background"))
            textField().bindText(modeColors::replaceBg)
          }.layout(RowLayout.PARENT_GRID)
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
            textField().bindText(modeColors::replaceFg)
          }.layout(RowLayout.PARENT_GRID)
        }

        group(MessageHelper.getMessage("widget.mode.popup.group.command.title")) {
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.background"))
            textField().bindText(modeColors::commandBg)
          }.layout(RowLayout.PARENT_GRID)
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
            textField().bindText(modeColors::commandFg)
          }.layout(RowLayout.PARENT_GRID)
        }

        group(MessageHelper.getMessage("widget.mode.popup.group.visual.title")) {
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.background"))
            textField().bindText(modeColors::visualBg)
          }.layout(RowLayout.PARENT_GRID)
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
            textField().bindText(modeColors::visualFg)
          }.layout(RowLayout.PARENT_GRID)

          collapsibleGroup(MessageHelper.getMessage("widget.mode.popup.group.visual.subgroup.line.title")) {
            row { text(MessageHelper.getMessage("widget.mode.popup.group.visual.subgroup.instruction")) }
            row {
              label(MessageHelper.getMessage("widget.mode.popup.field.background"))
              textField().bindText(modeColors::visualLineBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::visualLineFg)
            }.layout(RowLayout.PARENT_GRID)
          }

          collapsibleGroup(MessageHelper.getMessage("widget.mode.popup.group.visual.subgroup.block.title")) {
            row { text(MessageHelper.getMessage("widget.mode.popup.group.visual.subgroup.instruction")) }
            row {
              label(MessageHelper.getMessage("widget.mode.popup.field.background"))
              textField().bindText(modeColors::visualBlockBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::visualBlockFg)
            }.layout(RowLayout.PARENT_GRID)
          }
        }

        group(MessageHelper.getMessage("widget.mode.popup.group.select.title")) {
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.background"))
            textField().bindText(modeColors::selectBg)
          }.layout(RowLayout.PARENT_GRID)
          row {
            label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
            textField().bindText(modeColors::selectFg)
          }.layout(RowLayout.PARENT_GRID)

          collapsibleGroup(MessageHelper.getMessage("widget.mode.popup.group.select.subgroup.line.title")) {
            row { text(MessageHelper.getMessage("widget.mode.popup.group.select.subgroup.instruction")) }
            row {
              label(MessageHelper.getMessage("widget.mode.popup.field.background"))
              textField().bindText(modeColors::selectLineBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::selectLineFg)
            }.layout(RowLayout.PARENT_GRID)
          }

          collapsibleGroup(MessageHelper.getMessage("widget.mode.popup.group.select.subgroup.block.title")) {
            row { text(MessageHelper.getMessage("widget.mode.popup.group.select.subgroup.instruction")) }
            row {
              label(MessageHelper.getMessage("widget.mode.popup.field.background"))
              textField().bindText(modeColors::selectBlockBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.getMessage("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::selectBlockFg)
            }.layout(RowLayout.PARENT_GRID)
          }
        }
      }
      return panel
    }

    private fun JComponent.addScrollPane(): JComponent {
      val scrollPane = JBScrollPane(this, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
      scrollPane.border = BorderFactory.createEmptyBorder()
      return scrollPane
    }
  }

  private class ModeColors(
    normalBgKey: String, normalFgKey: String,
    insertBgKey: String, insertFgKey: String,
    replaceBgKey: String, replaceFgKey: String,
    commandBgKey: String, commandFgKey: String,
    visualBgKey: String, visualFgKey: String, visualLineBgKey: String, visualLineFgKey: String, visualBlockBgKey: String, visualBlockFgKey: String,
    selectBgKey: String, selectFgKey: String, selectLineBgKey: String, selectLineFgKey: String, selectBlockBgKey: String, selectBlockFgKey: String
  ) {
    var normalBg: String by VimScopeVariable(normalBgKey)
    var normalFg: String by VimScopeVariable(normalFgKey)
    var insertBg: String by VimScopeVariable(insertBgKey)
    var insertFg: String by VimScopeVariable(insertFgKey)
    var replaceBg: String by VimScopeVariable(replaceBgKey)
    var replaceFg: String by VimScopeVariable(replaceFgKey)
    var commandBg: String by VimScopeVariable(commandBgKey)
    var commandFg: String by VimScopeVariable(commandFgKey)
    var visualBg: String by VimScopeVariable(visualBgKey)
    var visualFg: String by VimScopeVariable(visualFgKey)
    var visualLineBg: String by VimScopeVariable(visualLineBgKey)
    var visualLineFg: String by VimScopeVariable(visualLineFgKey)
    var visualBlockBg: String by VimScopeVariable(visualBlockBgKey)
    var visualBlockFg: String by VimScopeVariable(visualBlockFgKey)
    var selectBg: String by VimScopeVariable(selectBgKey)
    var selectFg: String by VimScopeVariable(selectFgKey)
    var selectLineBg: String by VimScopeVariable(selectLineBgKey)
    var selectLineFg: String by VimScopeVariable(selectLineFgKey)
    var selectBlockBg: String by VimScopeVariable(selectBlockBgKey)
    var selectBlockFg: String by VimScopeVariable(selectBlockFgKey)

    private class VimScopeVariable(private var key: String): ReadWriteProperty<ModeColors, String> {
      override fun getValue(thisRef: ModeColors, property: KProperty<*>): String {
        return injector.variableService.getVimVariable(key)?.asString() ?: ""
      }

      override fun setValue(thisRef: ModeColors, property: KProperty<*>, value: String) {
        injector.variableService.storeVimVariable(key, VimString(value))
      }
    }
  }
}
