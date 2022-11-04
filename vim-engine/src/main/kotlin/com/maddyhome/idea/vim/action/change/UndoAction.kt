/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.action.ComplicatedKeysAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class UndoAction : VimActionHandler.SingleExecution(), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    injector.parser.parseKeys("u"),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_UNDO, 0))
  )

  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    var count = operatorArguments.count1
    var result = injector.undo.undo(context)
    while ((--count > 0) && result) {
      result = injector.undo.undo(context)
    }
    return result
  }
}
