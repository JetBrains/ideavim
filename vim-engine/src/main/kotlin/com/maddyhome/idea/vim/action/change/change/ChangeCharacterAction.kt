/*
 * Copyright 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

class ChangeCharacterAction : ChangeEditorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.DIGRAPH

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_ALLOW_DIGRAPH)

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return argument != null && changeCharacter(editor, caret, operatorArguments.count1, argument.character)
  }
}

private val logger = vimLogger<ChangeCharacterAction>()

/**
 * Replace each of the next count characters with the character ch
 *
 * @param editor The editor to change
 * @param caret  The caret to perform action on
 * @param count  The number of characters to change
 * @param ch     The character to change to
 * @return true if able to change count characters, false if not
 */
private fun changeCharacter(editor: VimEditor, caret: VimCaret, count: Int, ch: Char): Boolean {
  val col = caret.getLogicalPosition().column
  val len = injector.engineEditorHelper.getLineLength(editor)
  val offset = caret.offset.point
  if (len - col < count) {
    return false
  }

  // Special case - if char is newline, only add one despite count
  var num = count
  var space: String? = null
  if (ch == '\n') {
    num = 1
    space = injector.engineEditorHelper.getLeadingWhitespace(editor, editor.offsetToLogicalPosition(offset).line)
    logger.debug { "space='$space'" }
  }
  val repl = StringBuilder(count)
  for (i in 0 until num) {
    repl.append(ch)
  }
  injector.changeGroup.replaceText(editor, offset, offset + count, repl.toString())

  // Indent new line if we replaced with a newline
  if (ch == '\n') {
    injector.changeGroup.insertText(editor, caret, offset + 1, space!!)
    var slen = space.length
    if (slen == 0) {
      slen++
    }
    caret.moveToInlayAwareOffset(offset + slen)
  }
  return true
}
