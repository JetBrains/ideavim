/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimRangeMarker
import com.maddyhome.idea.vim.api.VimSearchGroupBase
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :global" / "h :vglobal"
 */
// FIXME: I'm such a mess, please refactor me, responsible developer
@ExCommand(command = "g[lobal],v[global]")
internal data class GlobalCommand(val range: Range, val argument: String, val invert: Boolean) : Command.SingleExecution(range, argument) {

  init {
    // Most commands have a default range of the current line ("."). Global has a default range of the whole file
    defaultRange = "%"
  }

  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    editor.removeSecondaryCarets()
    val caret = editor.currentCaret()
    val lineRange = getLineRange(editor, caret)
    if (!processGlobalCommand(editor, context, lineRange)) {
      result = ExecutionResult.Error
    }
    return result
  }

  private fun processGlobalCommand(
    editor: VimEditor,
    context: ExecutionContext,
    range: LineRange,
  ): Boolean {
    val messages = injector.messages
    // When nesting the command works on one line.  This allows for
    // ":g/found/v/notfound/command".
    if (globalBusy && (range.startLine != 0 || range.endLine != editor.lineCount() - 1)) {
      messages.showStatusBarMessage(null, messages.message("E147"))
      messages.indicateError()
      return false
    }

    val search = injector.searchGroup as VimSearchGroupBase
    val globalCommandArguments = search.parseGlobalCommand(argument) ?: return false

    val regex = try {
      search.prepareRegex(globalCommandArguments.pattern, globalCommandArguments.whichPattern, 2)
    } catch (e: VimRegexException) {
      messages.showStatusBarMessage(editor, e.message)
      return false
    }

    if (globalBusy) {
      val match = regex.findInLine(editor, editor.currentCaret().getLine())
      if (match is VimMatchResult.Success == !invert) {
        globalExecuteOne(editor, context, editor.getLineStartOffset(editor.currentCaret().getLine()), globalCommandArguments.command)
      }
    } else {
      val line1 = range.startLine
      val line2 = range.endLine
      if (line1 < 0 || line2 < 0) {
        return false
      }
      val matches = regex.findAll(
        editor,
        editor.getLineStartOffset(line1),
        editor.getLineEndOffset(line2),
      )
      val marks = if (!invert) matches.map {
        injector.engineEditorHelper.createRangeMarker(editor, editor.getLineStartForOffset(it.range.startOffset), editor.getLineStartForOffset(it.range.startOffset))
        // filter out lines that contain a match
      } else (line1..line2).filterNot { line ->
        matches.map { match ->
          editor.offsetToBufferPosition(match.range.startOffset).line
        }.contains(line)
      }.map { injector.engineEditorHelper.createRangeMarker(editor, editor.getLineStartOffset(it), editor.getLineStartOffset(it)) }

      if (gotInt) {
        messages.showStatusBarMessage(null, messages.message("e_interr"))
      } else if (marks.isEmpty()) {
        if (invert) {
          messages.showStatusBarMessage(null, messages.message("global.command.not.found.v", globalCommandArguments.pattern.toString()))
        } else {
          messages.showStatusBarMessage(null, messages.message("global.command.not.found.g", globalCommandArguments.pattern.toString()))
        }
      } else {
        globalExe(editor, context, marks, globalCommandArguments.command)
      }
    }
    return true
  }

  private fun globalExe(editor: VimEditor, context: ExecutionContext, marks: List<VimRangeMarker>, cmd: String) {
    globalBusy = true
    try {
      for (mark in marks) {
        if (gotInt) break
        if (!globalBusy) break
        val startOffset = mark.startOffset
        mark.dispose()
        globalExecuteOne(editor, context, startOffset, cmd)
        // TODO: 26.05.2021 break check
      }
    } catch (e: Exception) {
      throw e
    } finally {
      globalBusy = false
    }
    // TODO: 26.05.2021 Add other staff
  }

  private fun globalExecuteOne(editor: VimEditor, context: ExecutionContext, lineStartOffset: Int, cmd: String?) {
    // TODO: 26.05.2021 What about folds?
    editor.currentCaret().moveToOffset(lineStartOffset)
    if (cmd.isNullOrEmpty() || (cmd.length == 1 && cmd[0] == '\n')) {
      injector.vimscriptExecutor.execute("p", editor, context, skipHistory = true, indicateErrors = true, this.vimContext)
    } else {
      injector.vimscriptExecutor.execute(cmd, editor, context, skipHistory = true, indicateErrors = true, this.vimContext)
    }
  }

  companion object {
    private var globalBusy = false

    // Interrupted. Not used at the moment
    var gotInt: Boolean = false
  }
}
