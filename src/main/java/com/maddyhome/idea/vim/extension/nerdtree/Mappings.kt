/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.runAfterGotFocus
import com.maddyhome.idea.vim.key.KeyStrokeTrie
import com.maddyhome.idea.vim.key.add
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import javax.swing.KeyStroke
import javax.swing.tree.TreeNode

/**
 * Maps key sequences to NERDTree actions using KeyStrokeTrie for efficient lookup.
 *
 * @constructor Creates an empty trie for key mappings
 * @param name The name of the KeyStrokeTrie instance (for debug purposes)
 */
internal class Mappings(name: String) {
  private val trie = KeyStrokeTrie<Action>(name)
  val getAction = trie::getData
  val isPrefix = trie::isPrefix

  private val _keyStrokes = mutableSetOf<KeyStroke>()
  val keyStrokes: Set<KeyStroke> get() = _keyStrokes

  fun register(variable: String, defaultMapping: String, action: Action) {
    val variableValue = VimPlugin.getVariableService().getGlobalVariableValue(variable)
    val mapping = if (variableValue is VimString) {
      variableValue.value
    } else {
      defaultMapping
    }
    register(mapping, action)
  }

  fun register(mapping: String, action: Action) {
    trie.add(mapping, action)
    _keyStrokes.addAll(injector.parser.parseKeys(mapping))
  }

  /**
   * Defines the actual behavior of actions in NERDTree
   */
  class Action(val action: (AnActionEvent, Tree) -> Unit) {
    companion object {
      fun callAction(editor: VimEditor?, name: String, context: ExecutionContext) {
        val action = ActionManager.getInstance().getAction(name) ?: run {
          VimPlugin.showMessage(MessageHelper.message("action.not.found.0", name))
          return
        }
        val application = ApplicationManager.getApplication()
        if (application.isUnitTestMode) {
          injector.actionExecutor.executeAction(editor, action.vim, context)
        } else {
          runAfterGotFocus {
            injector.actionExecutor.executeAction(editor, action.vim, context)
          }
        }
      }

      /**
       * Creates an [Action] that executes an IntelliJ action identified by its ID.
       *
       * @param id A string representing the ID of the action to execute.
       * @return An [Action] that runs the specified action when triggered.
       */
      fun ij(id: String) = Action { event, _ -> callAction(null, id, event.dataContext.vim) }
    }
  }

  /**
   * Registers navigation-related mappings
   * <pre><code>
   * Default~
   * Key      Description                                                             Map settings
   *
   * O........Recursively open the selected directory..................*NERDTreeMapOpenRecursively*
   * x........Close the current nodes parent..................................*NERDTreeMapCloseDir*
   * X........Recursively close all children of the current node.........*NERDTreeMapCloseChildren*
   *
   * P........Jump to the root node...........................................*NERDTreeMapJumpRoot*
   * p........Jump to current nodes parent..................................*NERDTreeMapJumpParent*
   * K........Jump up inside directories at the current tree depth......*NERDTreeMapJumpFirstChild*
   * J........Jump down inside directories at the current tree depth.....*NERDTreeMapJumpLastChild*
   * <C-J>....Jump down to next sibling of the current directory.......*NERDTreeMapJumpNextSibling*
   * <C-K>....Jump up to previous sibling of the current directory.....*NERDTreeMapJumpPrevSibling*
   * </code></pre>
   */
  fun registerNavigationMappings() {
    // TODO support going [count] lines upward/downward or to line [count]
    register("k", Action.ij("Tree-selectPrevious"))
    register("j", Action.ij("Tree-selectNext"))
    register("G", Action.ij("Tree-selectLast"))
    register("gg", Action.ij("Tree-selectFirst"))

    // FIXME lazy loaded tree nodes are not expanded
    register("NERDTreeMapOpenRecursively", "O", Action.ij("FullyExpandTreeNode"))
    // This action respects `ide.tree.collapse.recursively`. We may prompt the user to disable it
    register("NERDTreeMapCloseDir", "x", Action { _, tree ->
      tree.selectionPath?.parentPath?.let {
        if (tree.getRowForPath(it) >= 0) { // skip if invisible, but we cannot use `tree.isVisible(path)` here
          tree.selectionPath = it
          tree.collapsePath(it)
          tree.scrollPathToVisible(it)
        }
      }
    })
    register(
      "NERDTreeMapCloseChildren",
      "X",
      Action { _, tree ->
        val path = tree.selectionPath ?: return@Action

        // FIXME We should avoid relying on `ide.tree.collapse.recursively` since it closes visible paths only
        val recursive = AdvancedSettings.getBoolean("ide.tree.collapse.recursively")
        try {
          AdvancedSettings.setBoolean("ide.tree.collapse.recursively", true)
          // Note that we cannot use `tree.collapsePaths` here since it does not respect `ide.tree.collapse.recursively`
          TreeUtil.listChildren(path.lastPathComponent as TreeNode).filterNot(TreeNode::isLeaf)
            .map(path::pathByAddingChild).forEach(tree::collapsePath)
        } finally {
          AdvancedSettings.setBoolean("ide.tree.collapse.recursively", recursive)
        }

        tree.scrollPathToVisible(path)
      },
    )

    // FIXME The first row is not the root of External Libraries
    register("NERDTreeMapJumpRoot", "P", Action.ij("Tree-selectFirst"))
    register("NERDTreeMapJumpParent", "p", Action.ij("Tree-selectParentNoCollapse"))
    register(
      "NERDTreeMapJumpFirstChild",
      "K",
      Action { _, tree ->
        var path = tree.selectionPath ?: return@Action
        while (true) {
          val previous = TreeUtil.previousVisibleSibling(tree, path)
          if (previous == null) break
          path = previous
        }
        tree.selectionPath = path
        tree.scrollPathToVisible(path)
      },
    )
    register(
      "NERDTreeMapJumpLastChild",
      "J",
      Action { _, tree ->
        var path = tree.selectionPath ?: return@Action
        while (true) {
          val next = TreeUtil.nextVisibleSibling(tree, path)
          if (next == null) break
          path = next
        }
        tree.selectionPath = path
        tree.scrollPathToVisible(path)
      },
    )
    register("NERDTreeMapJumpNextSibling", "<C-J>", Action.ij("Tree-selectNextSibling"))
    register("NERDTreeMapJumpPrevSibling", "<C-K>", Action.ij("Tree-selectPreviousSibling"))

    register("/", Action.ij("SpeedSearch"))
    register("<ESC>", Action { _, _ -> })
  }
}
