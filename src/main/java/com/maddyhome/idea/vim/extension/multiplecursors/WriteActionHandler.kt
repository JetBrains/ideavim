/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.newapi.ij

internal abstract class WriteActionHandler : ExtensionHandler {

  /**
   * Whether a count repeats the command
   */
  open val acceptsCount: Boolean = false

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val count = if (acceptsCount) operatorArguments.count1 else 1
    ApplicationManager.getApplication().runWriteAction {
      for (i in 1..count) {
        if (!executeInWriteAction(editor.ij)) break
      }
    }
  }

  /**
   * Returns whether the command has done anything. Repeating stops at the first command that hasn't.
   */
  abstract fun executeInWriteAction(editor: Editor): Boolean
}
