/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.change

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

/**
 * @author vlan
 */
class ChangeVisualCharacterAction : VisualOperatorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.DIGRAPH

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_ALLOW_DIGRAPH, CommandFlags.FLAG_EXIT_VISUAL)

  override fun executeAction(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    range: VimSelection,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = cmd.argument
    return argument != null &&
      changeCharacterRange(editor, range.toVimTextRange(false), argument.character)
  }
}

private val logger = vimLogger<ChangeVisualCharacterAction>()

/**
 * Each character in the supplied range gets replaced with the character ch
 *
 * @param editor The editor to change
 * @param range  The range to change
 * @param ch     The replacing character
 * @return true if able to change the range, false if not
 */
private fun changeCharacterRange(editor: VimEditor, range: TextRange, ch: Char): Boolean {
  logger.debug { "change range: $range to $ch" }
  val chars = editor.text()
  val starts = range.startOffsets
  val ends = range.endOffsets
  for (j in ends.indices.reversed()) {
    for (i in starts[j] until ends[j]) {
      if (i < chars.length && '\n' != chars[i]) {
        injector.changeGroup.replaceText(editor, i, i + 1, Character.toString(ch))
      }
    }
  }
  return true
}
