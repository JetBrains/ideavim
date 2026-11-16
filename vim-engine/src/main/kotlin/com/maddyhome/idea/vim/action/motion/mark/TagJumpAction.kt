/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.mark

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

/**
 * Jumps to the definition of the keyword under or after the caret, recording where we came from, see "h CTRL-]"
 *
 * This is a tag command, so `<C-T>` walks back over it. The action does not push onto the tag stack itself: it runs the
 * IDE's Go to Declaration, and every run of that action pushes, however it was invoked. That keeps `<C-]>`, Ctrl+Click
 * and mappings to `<Action>(GotoDeclaration)` in one place.
 */
@CommandOrMotion(
  keys = ["<C-]>"],
  modes = [Mode.NORMAL, Mode.VISUAL],
  description = "Jump to the definition of the keyword under the cursor using the IDE's Go to Declaration action. Pushes the position onto the tag stack."
)
class TagJumpAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.jumpService.saveJumpLocation(editor)
    injector.actionExecutor.executeAction(editor, name = "GotoDeclaration", context = context)
    return true
  }
}
