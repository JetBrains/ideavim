/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments

/**
 * An action handler registered by its extension to be executed as a Vim command.
 *
 * @author vlan
 */
interface ExtensionHandler {
  /**
   * Executes the action.
   *
   * The action is run on the EDT thread inside the @link com.intellij.openapi.command.CommandProcessor}.
   *
   * It's run without any read or write actions of @link com.intellij.openapi.application.Application}, so you have to
   * make sure your code is synchronized properly. A read action is not needed for the EDT in the IntelliJ platform. As
   * for a write action, you'll have to apply it by yourself if you're modifying IntelliJ's data structures like
   * documents or virtual files.
   */
  fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {}

  val isRepeatable: Boolean
    get() = false

  abstract class WithCallback : ExtensionHandler {
    var _backingFunction: Runnable? = null
    fun continueVimExecution() {
      if (_backingFunction != null) {
        _backingFunction!!.run()
      }
    }
  }
}
