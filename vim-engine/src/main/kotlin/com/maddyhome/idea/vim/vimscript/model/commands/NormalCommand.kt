/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.key.KeySource
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

// todo make it for each caret
@ExCommand(
  command = "norm[al]",
  barSeparates = false,
  description = "Execute {commands} as Normal mode keys, once for each line in [range]. With [!], mappings are not used."
)
data class NormalCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags = flags(
    RangeFlag.RANGE_OPTIONAL,
    ArgumentFlag.ARGUMENT_OPTIONAL,
    Access.WRITABLE,
  )

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val modeBefore = editor.mode
    if (modeBefore.isInsertOrReplace()) {
      executeFromInsertMode(editor, context, modeBefore)
    } else {
      executeOnEachLine(editor, context)
    }
    return ExecutionResult.Success
  }

  /** From Insert mode, the keys run once, in Normal mode, and Insert mode is back when they are done */
  private fun executeFromInsertMode(editor: VimEditor, context: ExecutionContext, insertMode: Mode) {
    editor.mode = Mode.NORMAL()
    val line = getLineRange(editor, editor.primaryCaret()).startLine
    if (line <= editor.lineCount()) {
      executeOnLine(editor, context, line)
      closeCommandLine(editor)
    }
    editor.mode = insertMode
  }

  private fun executeOnEachLine(editor: VimEditor, context: ExecutionContext) {
    val lines = getLineRange(editor, editor.primaryCaret())
    for (line in lines.startLine..lines.endLine) {
      // The keys can delete lines, so the end of the buffer is checked again for every line
      if (line > editor.lineCount()) break
      executeOnLine(editor, context, line)
      abandonUnfinishedCommand(editor, context)
    }
  }

  private fun executeOnLine(editor: VimEditor, context: ExecutionContext, line: Int) {
    if (isRangeGiven) {
      editor.currentCaret().moveToOffset(injector.motion.moveCaretToLineStart(editor, line))
    }
    typeKeys(editor, context)
  }

  /**
   * The keys go through the key stack, so that a handler waiting for a character, such as the one of surround's `yss`,
   * takes the next key of the command instead of waiting for the user to type it
   */
  private fun typeKeys(editor: VimEditor, context: ExecutionContext) {
    val keyHandler = KeyHandler.getInstance()
    keyHandler.reset(editor)
    keyHandler.keyStack.addKeys(injector.parser.stringToKeys(argument))
    try {
      while (keyHandler.keyStack.hasStroke()) {
        keyHandler.handleKey(editor, keyHandler.keyStack.feedStroke(), keySource, context, keyHandler.keyHandlerState)
      }
    } finally {
      keyHandler.keyStack.removeFirst()
    }
  }

  /** Keys that end in Insert or Command-line mode leave a command unfinished, and it is abandoned */
  private fun abandonUnfinishedCommand(editor: VimEditor, context: ExecutionContext) {
    when {
      editor.mode is Mode.CMD_LINE -> closeCommandLine(editor)
      editor.mode.isInsertOrReplace() -> editor.exitInsertMode(context)
    }
  }

  private fun closeCommandLine(editor: VimEditor) {
    if (editor.mode is Mode.CMD_LINE) {
      injector.commandLine.getActiveCommandLine()?.close(refocusOwningEditor = true, resetCaret = false)
    }
  }

  private val isRangeGiven: Boolean
    get() = range.size() != 0

  private val keySource: KeySource
    get() = if (modifier == CommandModifier.BANG) KeySource.NORMAL_COMMAND_NOT_MAPPED else KeySource.NORMAL_COMMAND

  private fun Mode.isInsertOrReplace() = this is Mode.INSERT || this is Mode.REPLACE
}
