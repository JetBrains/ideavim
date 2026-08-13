/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.hints

import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent
import com.maddyhome.idea.vim.extension.hints.TableEverywhere
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeEvent
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class TableEverywhereTest : VimTestCase() {

  // VIM-4307: an open cell editor is the table's Insert mode, so the navigation shortcuts must stand down.
  // Otherwise `G`/`$` are swallowed as selectLastRow/selectLastColumn instead of being typed into the cell
  // (unshifted keys like `g`/`0` never even reach us - the platform gives them to the focused text component).
  @Test
  fun `test navigation is disabled while a cell editor is active`() {
    onEdt {
      val table = createSampleTable()
      table.editCellAt(0, 0)
      val cellEditor = checkNotNull(table.editorComponent) { "Cell editing did not start" }

      assertFalse(
        isNavigationEnabledFor(cellEditor),
        "Keys typed into an active cell editor must not be swallowed by table navigation",
      )
      assertFalse(
        isNavigationEnabledFor(table),
        "Table navigation must stay disabled while a cell is being edited",
      )
    }
  }

  @Test
  fun `test navigation is enabled for a focused table that is not editing`() {
    onEdt {
      assertTrue(
        isNavigationEnabledFor(createSampleTable()),
        "Table navigation should be enabled while the table itself is focused",
      )
    }
  }

  // VIM-4307: the focus manager reports the loss as `focusOwner: table -> null` before the next owner gains focus.
  // Skipping the cleanup on that transition left the shortcuts registered on the table (so its cell editor inherited
  // them) and left `autoStartsEdit` suppressed forever.
  @Test
  fun `test leaving a table unregisters the shortcuts when the next focus owner is not known yet`() {
    onEdt {
      val extension = TableEverywhere()
      val dispatcher = service<TableEverywhere.TableEverywhereDispatcher>()
      val table = createSampleTable()

      extension.focusListener.propertyChange(focusOwnerChange(from = null, to = table))
      assertTrue(ActionUtil.getActions(table).contains(dispatcher), "Focusing a table should register the shortcuts")
      assertFalse(
        table.getClientProperty("JTable.autoStartsEdit") as Boolean,
        "Type-to-edit should be suppressed while the table is focused",
      )

      extension.focusListener.propertyChange(focusOwnerChange(from = table, to = null))
      assertFalse(
        ActionUtil.getActions(table).contains(dispatcher),
        "Leaving a table should unregister the shortcuts even when the next focus owner is null",
      )
      assertNull(
        table.getClientProperty("JTable.autoStartsEdit"),
        "Type-to-edit should be restored once the table loses focus",
      )
    }
  }

  private fun isNavigationEnabledFor(component: Component): Boolean {
    val dispatcher = service<TableEverywhere.TableEverywhereDispatcher>()
    val context = SimpleDataContext.builder().add(PlatformDataKeys.CONTEXT_COMPONENT, component).build()
    val event = TestActionEvent.createTestEvent(dispatcher, context)
    dispatcher.update(event)
    return event.presentation.isEnabled
  }

  private fun focusOwnerChange(from: Any?, to: Any?) =
    PropertyChangeEvent(KeyboardFocusManager.getCurrentKeyboardFocusManager(), "focusOwner", from, to)

  private fun createSampleTable(): JTable {
    val model = DefaultTableModel(
      arrayOf(arrayOf<Any>("FIRST_NAME", "value1"), arrayOf<Any>("SECOND_NAME", "value2")),
      arrayOf<Any>("Name", "Value"),
    )
    return JTable(model)
  }

  private fun onEdt(block: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait(block)
  }
}
