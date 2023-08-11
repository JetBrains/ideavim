/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.select

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.options.OptionConstants
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

public class SelectDeleteAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.INSERT

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
    val actions = injector.keyGroup.getActions(editor, enterKeyStroke)
    for (action in actions) {
      if (injector.actionExecutor.executeAction(editor, action, context)) {
        break
      }
    }

    var adjustCaret = !injector.globalOptions().keymodel.contains(OptionConstants.keymodel_startsel)
    editor.exitSelectModeNative(adjustCaret)
    if (adjustCaret) {
      injector.changeGroup.insertBeforeCursor(editor, context)
    }
    return true
  }
}
