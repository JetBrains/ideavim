/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.treeStructure.Tree
import com.maddyhome.idea.vim.newapi.vim

/**
 * Defines the actual behavior of actions in NERDTree
 */
internal class NerdAction(val action: (AnActionEvent, Tree) -> Unit) {
  companion object {
    fun executeAction(event: AnActionEvent, id: String) = NerdTree.Util.callAction(null, id, event.dataContext.vim)

    /**
     * Creates a NerdAction that executes a series of IntelliJ actions identified by their IDs.
     *
     * @param ids A variable number of strings representing the IDs of the actions to execute.
     * @return A NerdAction that runs the specified actions when triggered.
     */
    fun ij(vararg ids: String): NerdAction = NerdAction { event, _ -> ids.forEach { id -> executeAction(event, id) } }
  }
}
