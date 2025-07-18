/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.select

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_BACK_SPACE
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_DELETE

@CommandOrMotion(keys = ["<DEL>"], modes = [Mode.SELECT])
class SelectDeleteAction : SelectDeleteBackspaceActionBase() {
  override val keyStroke: VimKeyStroke
    get() = VimKeyStroke.getKeyStroke(VK_DELETE, 0)
}

@CommandOrMotion(keys = ["<BS>"], modes = [Mode.SELECT])
class SelectBackspaceAction : SelectDeleteBackspaceActionBase() {
  override val keyStroke: VimKeyStroke
    get() = VimKeyStroke.getKeyStroke(VK_BACK_SPACE, 0)
}

abstract class SelectDeleteBackspaceActionBase : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  abstract val keyStroke: VimKeyStroke

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    // TODO: It would be nice to know _why_ we use native actions for Delete/Backspace
    // If there's some kind of additional native editor action bound to Delete or Backspace (e.g. cancelling something,
    // etc.) then we should of course invoke it, like we do with Escape or Enter. But if we do, we should reconsider
    // unconditionally exiting Select mode. If the additional native editor action doesn't delete the text, then we're
    // exiting Select mode incorrectly. If there isn't an additional native editor action, then would it just be simpler
    // to delete the selected text using editor APIs?
    val actions = injector.keyGroup.getActions(editor, keyStroke)
    for (action in actions) {
      if (injector.actionExecutor.executeAction(editor, action, context)) {
        break
      }
    }

    // Note that Vim returns to the pending mode. I.e., when starting Select from Normal/Visual, it will return to
    // Normal. When returning from Insert or Replace pending Select (via shifted keys), it will return to Insert/Replace
    editor.exitSelectModeNative(true)
    return true
  }
}
