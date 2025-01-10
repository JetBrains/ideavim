/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.scroll

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandFlags.FLAG_CLEAR_STROKES
import com.maddyhome.idea.vim.command.CommandFlags.FLAG_IGNORE_SCROLL_JUMP
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

@CommandOrMotion(keys = ["<C-B>", "<PageUp>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionScrollPageUpAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(FLAG_IGNORE_SCROLL_JUMP)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return injector.scroll.scrollFullPage(editor, editor.primaryCaret(), -cmd.count)
  }
}

@CommandOrMotion(keys = ["<PageUp>"], modes = [Mode.INSERT])
class MotionScrollPageUpInsertModeAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(FLAG_IGNORE_SCROLL_JUMP, FLAG_CLEAR_STROKES)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return injector.scroll.scrollFullPage(editor, editor.primaryCaret(), -cmd.count)
  }
}
