/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.nerdtree

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.treeStructure.Tree
import com.maddyhome.idea.vim.extension.nerdtree.armSelectionRestoreOnEscape
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
}
