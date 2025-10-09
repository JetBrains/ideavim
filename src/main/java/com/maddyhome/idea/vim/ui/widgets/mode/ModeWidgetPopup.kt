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
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
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

class ModeWidgetPopup : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val popup = createPopup() ?: return
    popup.showCenteredInCurrentWindow(project)
  }

  companion object {
    @Volatile
    private var currentPopup: JBPopup? = null

    fun createPopup(): JBPopup? {
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
        tabbedPane.addTab(MessageHelper.message("widget.mode.popup.tab.light"), lightThemeSettings.addScrollPane())
        tabbedPane.addTab(MessageHelper.message("widget.mode.popup.tab.dark"), darkThemeSettings.addScrollPane())
        tabbedPane.preferredSize = Dimension(300, 300)
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
          .setTitle(MessageHelper.message("widget.mode.popup.title"))
          .setResizable(true)
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
        "widget_mode_is_full_customization$keyPostfix",
        "widget_mode_theme$keyPostfix",
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
        lateinit var advancedSettings: Cell<JBCheckBox>
        row {
          advancedSettings =
            checkBox(MessageHelper.message("widget.mode.popup.field.advanced.settings")).bindSelected(modeColors::isFullCustomization)
        }
        group {
          row {
            label(MessageHelper.message("widget.mode.popup.field.theme"))
            comboBox(ModeWidgetTheme.entries).bindItem(modeColors::theme.toNullableProperty())
          }
          row {
            browserLink(
              "Suggest your theme",
              "https://youtrack.jetbrains.com/issue/VIM-1377/Normal-mode-needs-to-be-more-obvious"
            )
          }
        }.topGap(TopGap.NONE).visibleIf(!advancedSettings.selected)
        group(MessageHelper.message("widget.mode.popup.group.title.full.customization")) {
          row { text(MessageHelper.message("widget.mode.popup.color.instruction")) }

          group(MessageHelper.message("widget.mode.popup.group.normal.title")) {
            row {
              label(MessageHelper.message("widget.mode.popup.field.background"))
              textField().bindText(modeColors::normalBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.message("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::normalFg)
            }.layout(RowLayout.PARENT_GRID)
          }

          group(MessageHelper.message("widget.mode.popup.group.insert.title")) {
            row {
              label(MessageHelper.message("widget.mode.popup.field.background"))
              textField().bindText(modeColors::insertBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.message("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::insertFg)
            }.layout(RowLayout.PARENT_GRID)
          }

          group(MessageHelper.message("widget.mode.popup.group.replace.title")) {
            row {
              label(MessageHelper.message("widget.mode.popup.field.background"))
              textField().bindText(modeColors::replaceBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.message("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::replaceFg)
            }.layout(RowLayout.PARENT_GRID)
          }

          group(MessageHelper.message("widget.mode.popup.group.command.title")) {
            row {
              label(MessageHelper.message("widget.mode.popup.field.background"))
              textField().bindText(modeColors::commandBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.message("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::commandFg)
            }.layout(RowLayout.PARENT_GRID)
          }

          group(MessageHelper.message("widget.mode.popup.group.visual.title")) {
            row {
              label(MessageHelper.message("widget.mode.popup.field.background"))
              textField().bindText(modeColors::visualBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.message("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::visualFg)
            }.layout(RowLayout.PARENT_GRID)

            collapsibleGroup(MessageHelper.message("widget.mode.popup.group.visual.subgroup.line.title")) {
              row { text(MessageHelper.message("widget.mode.popup.group.visual.subgroup.instruction")) }
              row {
                label(MessageHelper.message("widget.mode.popup.field.background"))
                textField().bindText(modeColors::visualLineBg)
              }.layout(RowLayout.PARENT_GRID)
              row {
                label(MessageHelper.message("widget.mode.popup.field.foreground"))
                textField().bindText(modeColors::visualLineFg)
              }.layout(RowLayout.PARENT_GRID)
            }

            collapsibleGroup(MessageHelper.message("widget.mode.popup.group.visual.subgroup.block.title")) {
              row { text(MessageHelper.message("widget.mode.popup.group.visual.subgroup.instruction")) }
              row {
                label(MessageHelper.message("widget.mode.popup.field.background"))
                textField().bindText(modeColors::visualBlockBg)
              }.layout(RowLayout.PARENT_GRID)
              row {
                label(MessageHelper.message("widget.mode.popup.field.foreground"))
                textField().bindText(modeColors::visualBlockFg)
              }.layout(RowLayout.PARENT_GRID)
            }
          }

          group(MessageHelper.message("widget.mode.popup.group.select.title")) {
            row {
              label(MessageHelper.message("widget.mode.popup.field.background"))
              textField().bindText(modeColors::selectBg)
            }.layout(RowLayout.PARENT_GRID)
            row {
              label(MessageHelper.message("widget.mode.popup.field.foreground"))
              textField().bindText(modeColors::selectFg)
            }.layout(RowLayout.PARENT_GRID)

            collapsibleGroup(MessageHelper.message("widget.mode.popup.group.select.subgroup.line.title")) {
              row { text(MessageHelper.message("widget.mode.popup.group.select.subgroup.instruction")) }
              row {
                label(MessageHelper.message("widget.mode.popup.field.background"))
                textField().bindText(modeColors::selectLineBg)
              }.layout(RowLayout.PARENT_GRID)
              row {
                label(MessageHelper.message("widget.mode.popup.field.foreground"))
                textField().bindText(modeColors::selectLineFg)
              }.layout(RowLayout.PARENT_GRID)
            }

            collapsibleGroup(MessageHelper.message("widget.mode.popup.group.select.subgroup.block.title")) {
              row { text(MessageHelper.message("widget.mode.popup.group.select.subgroup.instruction")) }
              row {
                label(MessageHelper.message("widget.mode.popup.field.background"))
                textField().bindText(modeColors::selectBlockBg)
              }.layout(RowLayout.PARENT_GRID)
              row {
                label(MessageHelper.message("widget.mode.popup.field.foreground"))
                textField().bindText(modeColors::selectBlockFg)
              }.layout(RowLayout.PARENT_GRID)
            }
          }
        }.topGap(TopGap.NONE).visibleIf(advancedSettings.selected)
      }
      return panel
    }

    private fun JComponent.addScrollPane(): JComponent {
      val scrollPane =
        JBScrollPane(this, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
      scrollPane.border = BorderFactory.createEmptyBorder()
      return scrollPane
    }
  }

  private class ModeColors(
    isFullCustomizationKey: String, themeKey: String,
    normalBgKey: String, normalFgKey: String,
    insertBgKey: String, insertFgKey: String,
    replaceBgKey: String, replaceFgKey: String,
    commandBgKey: String, commandFgKey: String,
    visualBgKey: String, visualFgKey: String, visualLineBgKey: String, visualLineFgKey: String, visualBlockBgKey: String, visualBlockFgKey: String,
    selectBgKey: String, selectFgKey: String, selectLineBgKey: String, selectLineFgKey: String, selectBlockBgKey: String, selectBlockFgKey: String
  ) {
    var isFullCustomization: Boolean by VimScopeBooleanVariable(isFullCustomizationKey)
    var theme: ModeWidgetTheme by VimScopeThemeVariable(themeKey)
    var normalBg: String by VimScopeStringVariable(normalBgKey)
    var normalFg: String by VimScopeStringVariable(normalFgKey)
    var insertBg: String by VimScopeStringVariable(insertBgKey)
    var insertFg: String by VimScopeStringVariable(insertFgKey)
    var replaceBg: String by VimScopeStringVariable(replaceBgKey)
    var replaceFg: String by VimScopeStringVariable(replaceFgKey)
    var commandBg: String by VimScopeStringVariable(commandBgKey)
    var commandFg: String by VimScopeStringVariable(commandFgKey)
    var visualBg: String by VimScopeStringVariable(visualBgKey)
    var visualFg: String by VimScopeStringVariable(visualFgKey)
    var visualLineBg: String by VimScopeStringVariable(visualLineBgKey)
    var visualLineFg: String by VimScopeStringVariable(visualLineFgKey)
    var visualBlockBg: String by VimScopeStringVariable(visualBlockBgKey)
    var visualBlockFg: String by VimScopeStringVariable(visualBlockFgKey)
    var selectBg: String by VimScopeStringVariable(selectBgKey)
    var selectFg: String by VimScopeStringVariable(selectFgKey)
    var selectLineBg: String by VimScopeStringVariable(selectLineBgKey)
    var selectLineFg: String by VimScopeStringVariable(selectLineFgKey)
    var selectBlockBg: String by VimScopeStringVariable(selectBlockBgKey)
    var selectBlockFg: String by VimScopeStringVariable(selectBlockFgKey)

    private class VimScopeBooleanVariable(private var key: String) : ReadWriteProperty<ModeColors, Boolean> {
      override fun getValue(thisRef: ModeColors, property: KProperty<*>): Boolean {
        return injector.variableService.getVimVariable(key)?.toVimNumber()?.booleanValue ?: false
      }

      override fun setValue(thisRef: ModeColors, property: KProperty<*>, value: Boolean) {
        injector.variableService.storeVimVariable(key, value.asVimInt())
      }
    }

    private class VimScopeStringVariable(private var key: String) : ReadWriteProperty<ModeColors, String> {
      override fun getValue(thisRef: ModeColors, property: KProperty<*>): String {
        return injector.variableService.getVimVariable(key)?.toVimString()?.value ?: ""
      }

      override fun setValue(thisRef: ModeColors, property: KProperty<*>, value: String) {
        injector.variableService.storeVimVariable(key, VimString(value))
      }
    }

    private class VimScopeThemeVariable(private var key: String) : ReadWriteProperty<ModeColors, ModeWidgetTheme> {
      override fun getValue(thisRef: ModeColors, property: KProperty<*>): ModeWidgetTheme {
        val themeString =
          injector.variableService.getVimVariable(key)?.toVimString()?.value ?: return ModeWidgetTheme.getDefaultTheme()
        return ModeWidgetTheme.parseString(themeString) ?: ModeWidgetTheme.getDefaultTheme()
      }

      override fun setValue(thisRef: ModeColors, property: KProperty<*>, value: ModeWidgetTheme) {
        injector.variableService.storeVimVariable(key, VimString(value.toString()))
      }
    }
  }
}

enum class ModeWidgetTheme(private var value: String) {
  TERM("Term"),
  COLORLESS("Colorless"),
  DRACULA("Dracula");

  override fun toString(): String {
    return value
  }

  companion object {
    fun parseString(string: String): ModeWidgetTheme? {
      return entries.firstOrNull { it.value == string }
    }

    fun getDefaultTheme(): ModeWidgetTheme = TERM
  }
}
