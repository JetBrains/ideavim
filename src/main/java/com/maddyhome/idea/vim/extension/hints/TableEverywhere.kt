/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.ui.SwingActionDelegate
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.ShortcutDispatcher
import com.maddyhome.idea.vim.extension.VimExtension
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import javax.swing.JTable
import javax.swing.KeyStroke

/**
 * Extends Vim-style `h`/`j`/`k`/`l` cell navigation to any Swing [JTable].
 *
 * Every IntelliJ table (`JBTable`, `TableView`, the database grid's `TableResultView`, ...) ultimately
 * extends [JTable], so keying off [JTable] in the focus listener covers all of them uniformly — the same
 * way [NerdTreeEverywhere] keys off `Tree`.
 *
 * Navigation is delegated to the table's own Swing `ActionMap` (populated by `BasicTableUI`) via
 * [SwingActionDelegate], mirroring [com.maddyhome.idea.vim.extension.nerdtree.NerdTreeAction.swing].
 * This avoids `ActionManager.tryToExecute` (which can RPC to the backend in split mode) while preserving
 * platform behaviour such as selection scrolling.
 */
internal class TableEverywhere : VimExtension {
  companion object {
    const val PLUGIN_NAME = "TableEverywhere"
  }

  override fun getName() = PLUGIN_NAME

  val focusListener = PropertyChangeListener { evt ->
    val newFocusOwner = evt.newValue
    val oldFocusOwner = evt.oldValue
    val dispatcher = service<TableEverywhereDispatcher>()
    if (newFocusOwner is JTable) {
      // `register` is idempotent — its internal implementation prevents duplicate registrations
      dispatcher.register(newFocusOwner)
      // While the table is focused, suppress "type to edit". Otherwise the platform's autoStartsEdit
      // handling (JTable.processKeyBinding -> editCellAt) begins cell editing on the KEY_PRESSED of a
      // printable char and moves focus into the cell editor, so our KEY_TYPED shortcut never fires and
      // h/j/k/l are typed into the cell instead. Editing is still available via Enter/F2/double-click.
      disableTypeToEdit(newFocusOwner)
    }
    // Unregistration of the shortcut is required to make the plugin disposable
    if (oldFocusOwner is JTable && newFocusOwner != null) {
      dispatcher.unregisterCustomShortcutSet(oldFocusOwner)
      restoreTypeToEdit(oldFocusOwner)
    }
  }

  override fun init() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener)
  }

  override fun dispose() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener)
    super.dispose()
  }

  @Service
  internal class TableEverywhereDispatcher :
    ShortcutDispatcher<(JTable) -> Unit>(PLUGIN_NAME, createMappings(), TableNavListener) {
    init {
      templatePresentation.isEnabledInModalContext = true
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = true
      // If <ESC> is pressed, clear the keys; skip only if there are no keys
      if ((e.inputEvent as? KeyEvent)?.keyCode == KeyEvent.VK_ESCAPE) {
        e.presentation.isEnabled = keyStrokes.isNotEmpty()
        keyStrokes.clear()
      }
      // Don't swallow keys while a cell is being edited or while SpeedSearch is active
      val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
      if (component is JTable && component.isEditing) {
        e.presentation.isEnabled = false
      }
      if (e.getData(PlatformDataKeys.SPEED_SEARCH_TEXT) != null) {
        e.presentation.isEnabled = false
      }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }
}

private object TableNavListener : ShortcutDispatcher.Listener<(JTable) -> Unit> {
  override fun onMatch(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>, data: (JTable) -> Unit) {
    val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
    if (component is JTable) {
      data(component)
    }
    keyStrokes.clear()
  }

  override fun onInvalid(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>) {
    keyStrokes.clear()
    injector.messages.indicateError()
  }
}

/** Swing client property that controls whether typing a printable char starts cell editing. */
private const val AUTO_STARTS_EDIT = "JTable.autoStartsEdit"

/** Marks a table whose `autoStartsEdit` we have suppressed, and stashes the value to restore. */
private const val AUTO_STARTS_EDIT_STASH = "IdeaVim.tableEverywhere.autoStartsEditStash"

private fun disableTypeToEdit(table: JTable) {
  // Idempotent: only stash the original value on the first suppression.
  if (table.getClientProperty(AUTO_STARTS_EDIT_STASH) != null) return
  // A previously-set value is Boolean or null; wrap it so a null (absent) round-trips correctly.
  table.putClientProperty(AUTO_STARTS_EDIT_STASH, listOf(table.getClientProperty(AUTO_STARTS_EDIT)))
  table.putClientProperty(AUTO_STARTS_EDIT, false)
}

private fun restoreTypeToEdit(table: JTable) {
  val stash = table.getClientProperty(AUTO_STARTS_EDIT_STASH) as? List<*> ?: return
  // Restoring null removes the property, returning the table to its default (enabled) behaviour.
  table.putClientProperty(AUTO_STARTS_EDIT, stash.firstOrNull())
  table.putClientProperty(AUTO_STARTS_EDIT_STASH, null)
}

/**
 * Maps Vim motions to the standard action names registered in a [JTable]'s `ActionMap` by `BasicTableUI`.
 */
private fun createMappings(): Map<List<KeyStroke>, (JTable) -> Unit> {
  val mappings = mutableMapOf<List<KeyStroke>, (JTable) -> Unit>()

  fun swing(swingActionId: String): (JTable) -> Unit =
    { table -> SwingActionDelegate.performAction(swingActionId, table) }

  fun register(keys: String, action: (JTable) -> Unit) {
    mappings[injector.parser.parseKeys(keys)] = action
  }

  register("h", swing("selectPreviousColumn"))
  register("l", swing("selectNextColumn"))
  register("j", swing("selectNextRow"))
  register("k", swing("selectPreviousRow"))

  register("gg", swing("selectFirstRow"))
  register("G", swing("selectLastRow"))
  register("0", swing("selectFirstColumn"))
  register("$", swing("selectLastColumn"))

  register("<ESC>") { }

  return mappings
}
