/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComboBoxTableRenderer
import com.intellij.openapi.ui.StripeTable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.TableUtil
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.RestartDialogImpl
import com.intellij.util.ui.UIUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MacKeyRepeat
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo.AllModes
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo.PerMode
import com.maddyhome.idea.vim.vimscript.services.VimRcService
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import kotlin.io.path.exists

/**
 * @author vlan
 */
internal class VimEmulationConfigurable : Configurable {
  private val settingsPanel: VimSettingsPanel by lazy { VimSettingsPanel() }

  private var savedKeyRepeat = false

  override fun getDisplayName(): String = MessageHelper.message("configurable.name.vim.emulation")

  override fun getHelpTopic(): String? {
    return null
  }

  // The component here should be created on demand, only after calling this method.
  // We're trying to do that by initializing settingsPanel in lazy
  override fun createComponent(): JComponent {
    return settingsPanel
  }

  override fun isModified(): Boolean {
    val checkBox = settingsPanel.keyRepeatCheckBox
    return settingsPanel.model.isModified || (checkBox != null && checkBox.isSelected != savedKeyRepeat) ||
      settingsPanel.vimrcPath != injector.vimrcPathService.vimrcPath
  }

  override fun apply() {
    settingsPanel.model.apply()
    applyKeyRepeat()
    applyVimrcPath()
  }

  private fun applyVimrcPath() {
    val service = injector.vimrcPathService
    if (settingsPanel.vimrcPath == service.vimrcPath) return
    service.vimrcPath = settingsPanel.vimrcPath
    // The file is executed from a different location now, so replace the loaded configuration with the new one
    reloadIdeaVimRc()
    settingsPanel.updateVimrcPathHints()
  }

  override fun reset() {
    settingsPanel.model.reset()
    settingsPanel.vimrcPath = injector.vimrcPathService.vimrcPath
    settingsPanel.updateVimrcPathHints()
    val checkBox = settingsPanel.keyRepeatCheckBox ?: return
    savedKeyRepeat = MacKeyRepeat.isEnabled ?: false
    checkBox.isSelected = savedKeyRepeat
  }

  /**
   * Applies the key repeat checkbox. Does nothing unless the checkbox itself was changed, so that editing the shortcut
   * conflicts alone never touches the system setting and never asks for a restart.
   */
  private fun applyKeyRepeat() {
    val checkBox = settingsPanel.keyRepeatCheckBox ?: return
    val enabled = checkBox.isSelected
    if (enabled == savedKeyRepeat) return
    savedKeyRepeat = enabled

    VimPlugin.getEditor().setKeyRepeat(enabled)
    ApplicationManager.getApplication().executeOnPooledThread {
      MacKeyRepeat.isEnabled = enabled
      // macOS reads ApplePressAndHoldEnabled when an application starts and caches it for the lifetime of the process,
      // so the running IDE keeps the previous behaviour until it is restarted. The non-modal modality state postpones
      // the suggestion until the settings dialog is closed.
      ApplicationManager.getApplication()
        .invokeLater({ RestartDialogImpl.showRestartRequired() }, ModalityState.nonModal())
    }
  }

  override fun disposeUIResources() {}

  private class VimSettingsPanel : JPanel() {

    val model: VimShortcutConflictsTable.Model = VimShortcutConflictsTable.Model()

    val keyRepeatCheckBox: JBCheckBox? = if (SystemInfo.isMac) createKeyRepeatCheckBox() else null

    private val vimrcPathField = createVimrcPathField()
    private val vimrcPathWarning = createVimrcPathWarning()

    /**
     * The path to the configuration file, as entered by the user and without the surrounding whitespace. An empty
     * string means that the default locations are searched.
     */
    var vimrcPath: String
      get() = vimrcPathField.text.trim()
      set(value) {
        vimrcPathField.text = value
      }

    /**
     * Refreshes the placeholder that names the file found in the default locations, and the warning shown when the
     * entered path does not point to an existing file.
     */
    fun updateVimrcPathHints() {
      (vimrcPathField.textField as? JBTextField)?.emptyText?.text = defaultVimrcPathHint()
      val resolved = injector.vimrcPathService.resolvePath(vimrcPath)
      val missing = resolved != null && !resolved.exists()
      vimrcPathWarning.text = if (missing) MessageHelper.message("configurable.label.vimrc.path.not.found", resolved) else ""
      vimrcPathWarning.isVisible = missing
    }

    private fun defaultVimrcPathHint(): String {
      val default = VimRcService.findDefaultIdeaVimRc()
      return if (default != null) {
        MessageHelper.message("configurable.label.vimrc.path.default.found", default)
      } else {
        MessageHelper.message("configurable.label.vimrc.path.default.not.found")
      }
    }

    init {
      val shortcutConflictsTable = VimShortcutConflictsTable(model)
      val rowsToBeChangedOnToggleAllHandlers = getRowsToBeToggled()

      layout = BorderLayout()
      val decorator = ToolbarDecorator.createDecorator(shortcutConflictsTable)
      decorator.setToolbarPosition(ActionToolbarPosition.RIGHT)
      decorator.addExtraAction(CopyForRcAction(model))
      decorator.addExtraAction(ResetHandlersAction(model, shortcutConflictsTable))
      decorator.addExtraAction(
        ToggleAllHandlersAction(
          model,
          shortcutConflictsTable,
          rowsToBeChangedOnToggleAllHandlers
        )
      )
      val scrollPane = decorator.createPanel()
      scrollPane.border = LineBorder(JBColor.border())
      val conflictsPanel = JPanel(BorderLayout())
      val title = MessageHelper.message("configurable.border.title.shortcut.conflicts.for.active.keymap")
      conflictsPanel.border = IdeBorderFactory.createTitledBorder(title, false)
      conflictsPanel.add(scrollPane)
      val headerPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        keyRepeatCheckBox?.let { add(alignLeft(createKeyRepeatPanel(it))) }
        add(alignLeft(createVimrcPathPanel()))
      }
      add(headerPanel, BorderLayout.NORTH)
      add(conflictsPanel, BorderLayout.CENTER)
      addHelpLine(model)
    }

    private fun createKeyRepeatCheckBox(): JBCheckBox {
      val checkBox = JBCheckBox(MessageHelper.message("configurable.checkbox.mac.key.repeat"))
      checkBox.toolTipText = MessageHelper.message("configurable.checkbox.mac.key.repeat.tooltip")
      return checkBox
    }

    private fun createKeyRepeatPanel(checkBox: JBCheckBox): JPanel {
      val comment = JBLabel(IdeBundle.message("ide.restart.required.comment"))
      comment.foreground = UIUtil.getInactiveTextColor()

      val panel = JPanel(HorizontalLayout(4, SwingConstants.CENTER))
      panel.add(checkBox)
      panel.add(comment)
      panel.border = JBUI.Borders.emptyBottom(8)
      return panel
    }

    private fun createVimrcPathField(): TextFieldWithBrowseButton {
      val field = TextFieldWithBrowseButton()
      field.textField.columns = 40
      field.text = injector.vimrcPathService.vimrcPath
      // TextFieldWithBrowseButton does not forward the tooltip to its children, and the wrapper itself is fully
      // covered by the text field and the button, so the tooltip has to be set on the text field directly
      field.textField.toolTipText = vimrcPathTooltip
      // The default file is a dotfile, which the chooser hides unless told otherwise
      val descriptor = FileChooserDescriptorFactory.singleFile()
        .withShowHiddenFiles(true)
        .withTitle(MessageHelper.message("configurable.vimrc.path.chooser.title"))
      field.addBrowseFolderListener(null, descriptor)
      field.textField.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) = updateVimrcPathHints()
      })
      return field
    }

    private fun createVimrcPathWarning(): JBLabel {
      val warning = JBLabel("", AllIcons.General.Warning, SwingConstants.LEADING)
      warning.isVisible = false
      return warning
    }

    private fun createVimrcPathPanel(): JPanel {
      val label = JBLabel(MessageHelper.message("configurable.label.vimrc.path"))
      label.labelFor = vimrcPathField.textField
      label.toolTipText = vimrcPathTooltip

      val fieldRow = JPanel(HorizontalLayout(UIUtil.DEFAULT_HGAP, SwingConstants.CENTER))
      fieldRow.add(label)
      fieldRow.add(vimrcPathField)

      val comment = JBLabel(MessageHelper.message("configurable.label.vimrc.path.comment"))
      comment.foreground = UIUtil.getInactiveTextColor()

      val hints = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.emptyLeft(label.preferredSize.width + UIUtil.DEFAULT_HGAP)
        // BoxLayout centres children of different widths unless they are all aligned the same way
        add(comment.apply { alignmentX = LEFT_ALIGNMENT })
        add(vimrcPathWarning.apply { alignmentX = LEFT_ALIGNMENT })
      }

      val panel = JPanel(BorderLayout())
      panel.add(fieldRow, BorderLayout.NORTH)
      panel.add(hints, BorderLayout.CENTER)
      panel.border = JBUI.Borders.emptyBottom(8)
      updateVimrcPathHints()
      return panel
    }

    private fun alignLeft(panel: JPanel): JPanel = panel.apply { alignmentX = LEFT_ALIGNMENT }

    private val vimrcPathTooltip: String
      get() = MessageHelper.message("configurable.label.vimrc.path.tooltip")

    private fun getRowsToBeToggled(): MutableList<Int> {
      val rowsToChange = mutableListOf<Int>()

      for (rowIndex in 0 until model.rows.size) {
        val row = model.rows[rowIndex]
        if (row.owner is AllModes && row.owner == ShortcutOwnerInfo.allUndefined) {
          rowsToChange.add(rowIndex)
        }
      }

      return rowsToChange
    }

    fun addHelpLine(model: VimShortcutConflictsTable.Model) {
      val firstPerMode = ContainerUtil.find(model.rows) { row: VimShortcutConflictsTable.Row ->
        val owner: ShortcutOwnerInfo = row.owner
        owner is PerMode
      }
      if (firstPerMode == null) {
        val label = HyperlinkLabel()
        label.setHtmlText(MessageHelper.message("configurable.sethandler.hint.text"))
        label.setHyperlinkTarget("https://jb.gg/vim-sethandler")
        label.foreground = UIUtil.getInactiveTextColor()
        add(label, BorderLayout.SOUTH)
      } else {
        val helpLine = JBLabel()
        helpLine.text = MessageHelper.message(
          "configurable.non.editable.handler.hint.text",
          (firstPerMode.owner as PerMode).toNotation(),
          KeymapUtil.getShortcutText(KeyboardShortcut(firstPerMode.keyStroke, null)),
        )
        helpLine.foreground = UIUtil.getInactiveTextColor()
        add(helpLine, BorderLayout.SOUTH)
      }
    }
  }

  class VimShortcutConflictsTable(model: Model) : StripeTable(model) {
    private val renderer: ComboBoxTableRenderer<ShortcutOwner> = ShortcutOwnerRenderer()

    init {
      getTableColumn(Column.KEYSTROKE).preferredWidth = 100
      getTableColumn(Column.IDE_ACTION).preferredWidth = 400
      val ownerColumn = getTableColumn(Column.OWNER)
      ownerColumn.preferredWidth = 150
    }

    override fun getCellRenderer(row: Int, column: Int): TableCellRenderer {
      if (column != Column.OWNER.index) return super.getCellRenderer(row, column)
      val model = model as Model
      val owner: ShortcutOwnerInfo = model.rows[row].owner
      return if (owner is PerMode) super.getCellRenderer(row, column) else renderer
    }

    override fun getCellEditor(row: Int, column: Int): TableCellEditor {
      if (column != Column.OWNER.index) return super.getCellEditor(row, column)
      val model = model as Model
      val owner: ShortcutOwnerInfo = model.rows[row].owner
      return if (owner is PerMode) super.getCellEditor(row, column) else renderer
    }

    override fun getMinimumSize(): Dimension {
      return calcSize(super.getMinimumSize())
    }

    override fun getPreferredSize(): Dimension {
      return calcSize(super.getPreferredSize())
    }

    private fun calcSize(dimension: Dimension): Dimension {
      val container = parent
      if (container != null) {
        val size = container.size
        return Dimension(size.width, dimension.height)
      }
      return dimension
    }

    private fun getTableColumn(column: Column): TableColumn {
      return getColumnModel().getColumn(column.index)
    }

    private class ShortcutOwnerRenderer : ComboBoxTableRenderer<ShortcutOwner>(ShortcutOwner.entries.toTypedArray()) {
      override fun customizeComponent(owner: ShortcutOwner, table: JTable, isSelected: Boolean) {
        super.customizeComponent(owner, table, isSelected)
        if (owner == ShortcutOwner.UNDEFINED) {
          foreground = UIUtil.getComboBoxDisabledForeground()
        }
      }

      override fun isCellEditable(event: EventObject): Boolean {
        return true
      }
    }

    private enum class Column(val index: Int, val title: @Nls(capitalization = Nls.Capitalization.Title) String) {
      KEYSTROKE(0, "Shortcut"),
      IDE_ACTION(1, "IDE Action"),
      OWNER(2, "Handler"),
      ;

      companion object {
        private val ourMembers: MutableMap<Int, Column> = HashMap()

        init {
          for (column in entries) {
            ourMembers[column.index] = column
          }
        }

        fun fromIndex(index: Int): Column? {
          return ourMembers[index]
        }
      }
    }

    class Row(val keyStroke: KeyStroke, val action: AnAction, var owner: ShortcutOwnerInfo) : Comparable<Row> {

      override fun compareTo(other: Row): Int {
        val otherKeyStroke: KeyStroke = other.keyStroke
        val keyCodeDiff: Int = keyStroke.keyCode - otherKeyStroke.keyCode
        return if (keyCodeDiff != 0) keyCodeDiff else keyStroke.modifiers - otherKeyStroke.modifiers
      }
    }

    class Model : AbstractTableModel() {
      val rows: MutableList<Row> = ArrayList()

      init {
        reset()
      }

      override fun getRowCount(): Int {
        return rows.size
      }

      override fun getColumnCount(): Int {
        return Column.entries.size
      }

      override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val column = Column.fromIndex(columnIndex)
        if (column != null && rowIndex >= 0 && rowIndex < rows.size) {
          val row = rows[rowIndex]
          when (column) {
            Column.KEYSTROKE -> return KeymapUtil.getShortcutText(KeyboardShortcut(row.keyStroke, null))
            Column.IDE_ACTION -> return row.action.templatePresentation.text
            Column.OWNER -> {
              val owner: ShortcutOwnerInfo = row.owner
              if (owner is AllModes) {
                return owner.owner
              } else if (owner is PerMode) {
                return owner.toNotation()
              }
            }
          }
        }
        return null
      }

      override fun setValueAt(`object`: Any, rowIndex: Int, columnIndex: Int) {
        val column = Column.fromIndex(columnIndex)
        if (column != null && rowIndex >= 0 && rowIndex < rows.size && `object` is ShortcutOwner) {
          val row = rows[rowIndex]
          row.owner = AllModes(`object`)
        }
      }

      override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return if (rows[rowIndex].owner is PerMode) false else Column.fromIndex(columnIndex) == Column.OWNER
      }

      override fun getColumnName(index: Int): String? {
        return Column.fromIndex(index)?.title
      }

      val isModified: Boolean
        get() = VimPlugin.getKey().shortcutConflicts != currentData

      fun apply() {
        VimPlugin.getKey().savedShortcutConflicts.putAll(currentData)
      }

      fun reset() {
        rows.clear()
        for ((keyStroke, value) in VimPlugin.getKey().shortcutConflicts) {
          val actions = VimPlugin.getKey().getKeymapConflicts(keyStroke)
          if (actions.isNotEmpty()) {
            rows.add(Row(keyStroke, actions[0].action as AnAction, value))
          }
        }
        rows.sort()
      }

      private val currentData: Map<KeyStroke, ShortcutOwnerInfo>
        get() {
          val result: MutableMap<KeyStroke, ShortcutOwnerInfo> = HashMap()
          for (row in rows) {
            result[row.keyStroke] = row.owner
          }
          return result
        }
    }
  }

  private class CopyForRcAction(
    private val myModel: VimShortcutConflictsTable.Model,
  ) : DumbAwareAction(
    "Copy Config for .ideavimrc",
    "Copy config for .ideavimrc in sethandler format",
    AllIcons.Actions.Copy,
  ) {

    override fun update(e: AnActionEvent) {
      val enabled: Boolean = myModel.rows.stream().anyMatch {
        it.owner is AllModes && (it.owner as AllModes).owner != ShortcutOwner.UNDEFINED
      }
      e.presentation.isEnabled = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
      val stringBuilder = StringBuilder()
      for (row in myModel.rows) {
        val ownerInfo: ShortcutOwnerInfo = row.owner as? AllModes ?: continue
        val owner = (ownerInfo as AllModes).owner
        if (owner === ShortcutOwner.UNDEFINED) continue
        stringBuilder.append("sethandler ")
        stringBuilder.append(injector.parser.toKeyNotation(row.keyStroke))
        stringBuilder.append(" ")
        stringBuilder.append("a:")
        stringBuilder.append(owner.ownerName)
        stringBuilder.append("\n")
      }
      val data = stringBuilder.toString()
      injector.clipboardManager.setClipboardText(data, data, emptyList())
    }
  }

  private class ToggleAllHandlersAction(
    private val myModel: VimShortcutConflictsTable.Model,
    private val myTable: VimShortcutConflictsTable,
    private val rowsToBeChangedOnToggleAllHandlers: MutableList<Int>,
  ) : DumbAwareAction(
    "Toggle all unset Handlers",
    "Toggle all action handlers to use either IDE or Vim shortcuts",
    AllIcons.Actions.ChangeView,
  ) {
    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = true
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
      TableUtil.stopEditing(myTable)

      for (rowIndex in rowsToBeChangedOnToggleAllHandlers) {
        val row = myModel.rows[rowIndex]
        row.owner =
          when (row.owner) {
            ShortcutOwnerInfo.allUndefined -> ShortcutOwnerInfo.allVim
            ShortcutOwnerInfo.allVim -> ShortcutOwnerInfo.allIde
            else -> ShortcutOwnerInfo.allUndefined
          }
      }

      IdeFocusManager.getGlobalInstance()
        .doWhenFocusSettlesDown { IdeFocusManager.getGlobalInstance().requestFocus(myTable, true) }
      TableUtil.updateScroller(myTable)
    }
  }

  class ResetHandlersAction(
    private val myModel: VimShortcutConflictsTable.Model,
    private val myTable: VimShortcutConflictsTable,
  ) : DumbAwareAction("Reset Handlers", "Reset handlers", AllIcons.General.Reset) {
    override fun update(e: AnActionEvent) {
      val enabled: Boolean = myModel.rows.stream().anyMatch {
        it.owner is AllModes && (it.owner as AllModes).owner != ShortcutOwner.UNDEFINED
      }
      e.presentation.isEnabled = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
      TableUtil.stopEditing(myTable)
      for (row in myModel.rows) {
        val owner: ShortcutOwnerInfo = row.owner
        if (owner is AllModes) {
          if (owner.owner != ShortcutOwner.UNDEFINED) {
            row.owner = ShortcutOwnerInfo.allUndefined
          }
        }
      }
      IdeFocusManager.getGlobalInstance()
        .doWhenFocusSettlesDown { IdeFocusManager.getGlobalInstance().requestFocus(myTable, true) }
      TableUtil.updateScroller(myTable)
    }
  }
}
