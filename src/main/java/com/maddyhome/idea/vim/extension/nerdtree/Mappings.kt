/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.util.ui.tree.TreeUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import javax.swing.KeyStroke
import javax.swing.tree.TreeNode

fun MutableMap<List<KeyStroke>, NerdTreeAction>.register(
  variable: String,
  defaultMapping: String,
  action: NerdTreeAction,
) {
  val variableValue = VimPlugin.getVariableService().getGlobalVariableValue(variable)
  val mapping = if (variableValue is VimString) {
    variableValue.value
  } else {
    defaultMapping
  }
  register(mapping, action)
}

fun MutableMap<List<KeyStroke>, NerdTreeAction>.register(mapping: String, action: NerdTreeAction) {
  this[injector.parser.parseKeys(mapping)] = action
}

/**
 * Navigation-related mappings
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
val navigationMappings: Map<List<KeyStroke>, NerdTreeAction> = mutableMapOf<List<KeyStroke>, NerdTreeAction>().apply {
  // TODO support going [count] lines upward/downward or to line [count]
  register("k", NerdTreeAction.ij("Tree-selectPrevious"))
  register("j", NerdTreeAction.ij("Tree-selectNext"))
  register("G", NerdTreeAction.ij("Tree-selectLast"))
  register("gg", NerdTreeAction.ij("Tree-selectFirst"))

  // FIXME lazy loaded tree nodes are not expanded
  register("NERDTreeMapOpenRecursively", "O", NerdTreeAction.ij("FullyExpandTreeNode"))
  // This action respects `ide.tree.collapse.recursively`. We may prompt the user to disable it
  register("NERDTreeMapCloseDir", "x", NerdTreeAction { _, tree ->
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
    NerdTreeAction { _, tree ->
      val path = tree.selectionPath ?: return@NerdTreeAction

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

  register("NERDTreeMapJumpRoot", "P", NerdTreeAction { _, tree ->
    // Note that we should not consider the root simply the first row
    // It cannot be guaranteed that the tree has a single visible root
    var path = tree.selectionPath ?: return@NerdTreeAction
    while (path.parentPath != null && tree.getRowForPath(path.parentPath) >= 0) {
      path = path.parentPath
    }
    tree.selectionPath = path
    tree.scrollPathToVisible(path)
  })
  register("NERDTreeMapJumpParent", "p", NerdTreeAction.ij("Tree-selectParentNoCollapse"))
  register(
    "NERDTreeMapJumpFirstChild",
    "K",
    NerdTreeAction { _, tree ->
      var path = tree.selectionPath ?: return@NerdTreeAction
      while (true) {
        val previous = TreeUtil.previousVisibleSibling(tree, path) ?: break
        path = previous
      }
      tree.selectionPath = path
      tree.scrollPathToVisible(path)
    },
  )
  register(
    "NERDTreeMapJumpLastChild",
    "J",
    NerdTreeAction { _, tree ->
      var path = tree.selectionPath ?: return@NerdTreeAction
      while (true) {
        val next = TreeUtil.nextVisibleSibling(tree, path) ?: break
        path = next
      }
      tree.selectionPath = path
      tree.scrollPathToVisible(path)
    },
  )
  register("NERDTreeMapJumpNextSibling", "<C-J>", NerdTreeAction.ij("Tree-selectNextSibling"))
  register("NERDTreeMapJumpPrevSibling", "<C-K>", NerdTreeAction.ij("Tree-selectPreviousSibling"))

  register("/", NerdTreeAction.ij("SpeedSearch"))
  register("<ESC>", NerdTreeAction { _, _ -> })
}
