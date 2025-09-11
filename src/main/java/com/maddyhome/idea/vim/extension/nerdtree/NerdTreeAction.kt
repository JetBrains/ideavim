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
import com.intellij.ui.treeStructure.Tree
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.runAfterGotFocus
import com.maddyhome.idea.vim.newapi.vim

/**
 * Defines the actual behavior of actions in NERDTree
 */
class NerdTreeAction(val action: (AnActionEvent, Tree) -> Unit) {
  companion object {
    fun callAction(editor: VimEditor?, name: String, context: ExecutionContext) {
      val action = ActionManager.getInstance().getAction(name) ?: run {
        VimPlugin.showMessage(MessageHelper.message("nerdtree.error.action.not.found", name))
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
     * Creates an [NerdTreeAction] that executes an IntelliJ action identified by its ID.
     *
     * @param id A string representing the ID of the action to execute.
     * @return An [NerdTreeAction] that runs the specified action when triggered.
     */
    fun ij(id: String) = NerdTreeAction { event, _ -> callAction(null, id, event.dataContext.vim) }
  }
}
