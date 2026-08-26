/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getCurrentIndex
import com.maddyhome.idea.vim.api.getEntries
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.tag.TagStackEntry
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :tags"
 */
@ExCommand(command = "tags")
data class TagsCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_FORBIDDEN, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    injector.outputPanel.clear(editor, context)
    injector.outputPanel.output(editor, context, renderTagStack(editor))
    return ExecutionResult.Success
  }

  private fun renderTagStack(editor: VimEditor): String {
    val entries = injector.tagService.getEntries(editor)
    val currentIndex = injector.tagService.getCurrentIndex(editor)
    val isAtTopOfStack = currentIndex == entries.size

    return buildString {
      appendLine(HEADER)
      entries.forEachIndexed { index, entry ->
        appendLine(renderEntry(editor, entry, index, isCurrent = index == currentIndex))
      }
      // At the top of the stack there is no entry to mark, so the marker gets a line of its own
      if (isAtTopOfStack) appendLine(CURRENT_ENTRY_MARKER)
    }
  }

  private fun renderEntry(editor: VimEditor, entry: TagStackEntry, index: Int, isCurrent: Boolean): String {
    val marker = if (isCurrent) CURRENT_ENTRY_MARKER else " "
    val number = (index + 1).toString().padStart(NUMBER_WIDTH)
    val matchNumber = ONLY_MATCH.padStart(MATCH_NUMBER_WIDTH)
    val tagName = entry.tagName.padEnd(TAG_NAME_WIDTH)
    val fromLine = (entry.line + 1).toString().padStart(LINE_NUMBER_WIDTH)
    return "$marker$number $matchNumber $tagName $fromLine  ${renderJumpedFrom(editor, entry)}"
  }

  /**
   * The line the tag jump was made from, or the file holding it when that is not the file we are looking at
   */
  private fun renderJumpedFrom(editor: VimEditor, entry: TagStackEntry): String {
    if (editor.getVirtualFile()?.path != entry.filepath) return entry.filepath
    return toPrintableText(editor.getLineText(entry.line))
  }

  private fun toPrintableText(line: String): String {
    val keys = injector.parser.stringToKeys(line.trim().take(MAX_TEXT_LENGTH))
    return EngineStringHelper.toPrintableCharacters(keys).take(MAX_TEXT_LENGTH)
  }

  private companion object {
    const val HEADER = "  # TO tag         FROM line  in file/text"
    const val CURRENT_ENTRY_MARKER = ">"

    /** We only ever record the match the IDE navigated to, so there is no `:tnext` to count through */
    const val ONLY_MATCH = "1"

    const val NUMBER_WIDTH = 2
    const val MATCH_NUMBER_WIDTH = 2
    const val TAG_NAME_WIDTH = 15
    const val LINE_NUMBER_WIDTH = 5
    const val MAX_TEXT_LENGTH = 200
  }
}
