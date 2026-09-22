/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.nerdtree

import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.treeStructure.Tree
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.nerdtree.armSelectionRestoreOnEscape
import com.maddyhome.idea.vim.extension.nerdtree.navigationMappings
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.awt.event.KeyEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class NerdTreeTest : VimTestCase() {
  @Test
  fun `test collapse recursively advanced setting id`() {
    assertDoesNotThrow {
      AdvancedSettings.getBoolean("ide.tree.collapse.recursively") // will throw if the id is invalid
    }
  }

  // VIM-4196: pressing `/` to speed search and then ESC should restore the
  // original tree selection rather than leave the cursor on the search match.
  @Test
  fun `test esc after speed search restores original tree selection`() {
    onEdt {
      val tree = createSampleTree()
      val pathA = tree.pathFor("fileA.txt")
      val pathB = tree.pathFor("fileB.txt")

      tree.selectionPath = pathA

      armSelectionRestoreOnEscape(tree)

      tree.selectionPath = pathB

      tree.fireKeyPressed(KeyEvent.VK_ESCAPE)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

      assertEquals(pathA, tree.selectionPath, "ESC after `/` speed search should restore original selection")
    }
  }

  // VIM-4196: pressing ENTER (commit) must not roll the selection back to the
  // pre-search path; the user explicitly chose the matched item.
  @Test
  fun `test enter after speed search keeps the search match selected`() {
    onEdt {
      val tree = createSampleTree()
      val pathA = tree.pathFor("fileA.txt")
      val pathB = tree.pathFor("fileB.txt")

      tree.selectionPath = pathA

      armSelectionRestoreOnEscape(tree)

      tree.selectionPath = pathB

      tree.fireKeyPressed(KeyEvent.VK_ENTER)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

      assertEquals(pathB, tree.selectionPath, "ENTER after `/` speed search should keep the matched selection")
    }
  }

  // `<C-D>` and `<C-U>` scroll half a page, like in Vim: the selection moves by half the number of rows that fit
  // into the visible part of the tree.
  @Test
  fun `test ctrl-d moves the selection half a page down`() {
    onEdt {
      val tree = createTallTree(childCount = 40, visibleRows = 10)
      tree.setSelectionRow(1)

      tree.perform("<C-D>")

      assertEquals(6, tree.selectedRow, "<C-D> should move the selection five rows down")
    }
  }

  @Test
  fun `test ctrl-u moves the selection half a page up`() {
    onEdt {
      val tree = createTallTree(childCount = 40, visibleRows = 10)
      tree.setSelectionRow(20)

      tree.perform("<C-U>")

      assertEquals(15, tree.selectedRow, "<C-U> should move the selection five rows up")
    }
  }

  @Test
  fun `test half page scrolling stops at the last row`() {
    onEdt {
      val tree = createTallTree(childCount = 40, visibleRows = 10)
      val lastRow = tree.rowCount - 1
      tree.setSelectionRow(lastRow - 2)

      tree.perform("<C-D>")

      assertEquals(lastRow, tree.selectedRow, "<C-D> should not move the selection past the last row")
    }
  }

  @Test
  fun `test half page scrolling stops at the first row`() {
    onEdt {
      val tree = createTallTree(childCount = 40, visibleRows = 10)
      tree.setSelectionRow(2)

      tree.perform("<C-U>")

      assertEquals(0, tree.selectedRow, "<C-U> should not move the selection above the first row")
    }
  }

  // Nothing is selected yet when the tree has just been opened - the scroll should start from the topmost visible row
  // instead of throwing.
  @Test
  fun `test half page scrolling without a selection`() {
    onEdt {
      val tree = createTallTree(childCount = 40, visibleRows = 10)
      tree.clearSelection()

      tree.perform("<C-D>")

      assertEquals(5, tree.selectedRow, "<C-D> without a selection should select half a page below the first row")
    }
  }

  private fun onEdt(block: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait(block)
  }

  private fun createSampleTree(): Tree {
    val root = DefaultMutableTreeNode("root")
    root.add(DefaultMutableTreeNode("fileA.txt"))
    root.add(DefaultMutableTreeNode("fileB.txt"))
    root.add(DefaultMutableTreeNode("fileC.txt"))
    return Tree(DefaultTreeModel(root))
  }

  private fun Tree.pathFor(name: String): TreePath {
    val root = model.root as DefaultMutableTreeNode
    val child = (0 until root.childCount)
      .map { root.getChildAt(it) as DefaultMutableTreeNode }
      .first { it.userObject == name }
    return TreePath(arrayOf<Any>(root, child))
  }

  /**
   * Fire a synthetic KEY_PRESSED to all listeners registered on the tree. We
   * call the listeners directly because `Component.dispatchEvent` does not fire
   * KeyListeners on a non-displayed component in headless tests.
   */
  private fun Tree.fireKeyPressed(keyCode: Int) {
    val keyChar = if (keyCode == KeyEvent.VK_ENTER) '\n' else KeyEvent.CHAR_UNDEFINED
    val event = KeyEvent(this, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, keyChar)
    keyListeners.forEach { it.keyPressed(event) }
  }

  /**
   * A tree with more rows than fit on the screen. Without a scroll pane the tree's visible rectangle is its own
   * bounds, so sizing it to [visibleRows] rows is what makes `TreeUtil.getVisibleRowCount` report a page.
   */
  private fun createTallTree(childCount: Int, visibleRows: Int): Tree {
    val root = DefaultMutableTreeNode("root")
    repeat(childCount) { root.add(DefaultMutableTreeNode("file$it.txt")) }
    val tree = Tree(DefaultTreeModel(root))
    tree.rowHeight = ROW_HEIGHT
    tree.expandRow(0)
    tree.setSize(200, ROW_HEIGHT * visibleRows)
    assertEquals(childCount + 1, tree.rowCount, "The whole tree should be expanded")
    return tree
  }

  private val Tree.selectedRow: Int
    get() = selectionRows?.single() ?: -1

  private fun Tree.perform(keys: String) {
    val action = navigationMappings[injector.parser.parseKeys(keys)] ?: error("No NERDTree mapping for `$keys`")
    action.action(AnActionEvent.createEvent(DataContext.EMPTY_CONTEXT, null, "test", ActionUiKind.NONE, null), this)
  }

  companion object {
    private const val ROW_HEIGHT = 20
  }
}
