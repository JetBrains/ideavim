/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.editor.RangeMarker
import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.group.SearchGroup
import com.maddyhome.idea.vim.group.SearchGroup.RE_BOTH
import com.maddyhome.idea.vim.group.SearchGroup.RE_LAST
import com.maddyhome.idea.vim.group.SearchGroup.RE_SEARCH
import com.maddyhome.idea.vim.group.SearchGroup.RE_SUBST
import com.maddyhome.idea.vim.helper.MessageHelper.message
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.regexp.RegExp
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :global" / "h :vglobal"
 */
@ExCommand(command = "g[lobal],v[global]")
internal data class GlobalCommand(val ranges: Ranges, val argument: String, val invert: Boolean) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    editor.removeSecondaryCarets()
    val caret = editor.currentCaret()

    // For :g command the default range is %
    val lineRange: LineRange = if (ranges.size() == 0) {
      LineRange(0, editor.lineCount() - 1)
    } else {
      getLineRange(editor, caret)
    }
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
    // When nesting the command works on one line.  This allows for
    // ":g/found/v/notfound/command".
    if (globalBusy && (range.startLine != 0 || range.endLine != editor.lineCount() - 1)) {
      VimPlugin.showMessage(message("E147"))
      VimPlugin.indicateError()
      return false
    }
    var cmd = CharPointer(StringBuffer(argument))

    val pat: CharPointer
    val delimiter: Char
    var whichPat = RE_LAST

    /*
     * undocumented vi feature:
     * "\/" and "\?": use previous search pattern.
     *   "\&": use previous substitute pattern.
     */
    if (argument.isEmpty()) {
      VimPlugin.showMessage(message("E148"))
      VimPlugin.indicateError()
      return false
    } else if (cmd.charAt() == '\\') {
      cmd.inc()
      if ("/?&".indexOf(cmd.charAt()) == -1) {
        VimPlugin.showMessage(message(Msg.e_backslash))
        return false
      }
      whichPat = if (cmd.charAt() == '&') RE_SUBST else RE_SEARCH
      cmd.inc()
      pat = CharPointer("") /* empty search pattern */
    } else {
      delimiter = cmd.charAt() /* get the delimiter */
      cmd.inc()
      pat = cmd.ref(0) /* remember start of pattern */
      cmd = RegExp.skip_regexp(cmd, delimiter, true)
      if (cmd.charAt() == delimiter) { /* end delimiter found */
        cmd.set('\u0000').inc() /* replace it with a NUL */
      }
    }

    if (injector.globalIjOptions().useNewRegex) {
      val regex = try {
        VimRegex(pat.toString())
      } catch (e: VimRegexException) {
        injector.messages.showStatusBarMessage(editor, e.message)
        return false
      }

      if (globalBusy) {
        val match = regex.findInLine(editor, editor.currentCaret().getLine().line)
        if (match is VimMatchResult.Success == !invert) {
          globalExecuteOne(editor, context, editor.getLineStartOffset(editor.currentCaret().getLine().line), cmd.toString())
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
          editor.ij.document.createRangeMarker(editor.getLineStartForOffset(it.range.startOffset), editor.getLineStartForOffset(it.range.startOffset))
        // filter out lines that contain a match
        } else (line1..line2).filterNot { line ->
          matches.map { match ->
            editor.offsetToBufferPosition(match.range.startOffset).line
          }.contains(line)
        }.map { editor.ij.document.createRangeMarker(editor.getLineStartOffset(it), editor.getLineStartOffset(it)) }

        if (gotInt) {
          VimPlugin.showMessage(message("e_interr"))
        } else if (marks.isEmpty()) {
          if (invert) {
            VimPlugin.showMessage(message("global.command.not.found.v", pat.toString()))
          } else {
            VimPlugin.showMessage(message("global.command.not.found.g", pat.toString()))
          }
        } else {
          globalExe(editor, context, marks, cmd.toString())
        }
      }
    } else {
      val (first, second) = (injector.searchGroup as SearchGroup).search_regcomp(pat, whichPat, RE_BOTH)
      if (!first) {
        VimPlugin.showMessage(message(Msg.e_invcmd))
        VimPlugin.indicateError()
        return false
      }
      val regmatch = second.first as RegExp.regmmatch_T
      val sp = second.third as RegExp

      var match: Int
      val lcount = editor.lineCount()
      val searchcol = 0
      if (globalBusy) {
        val offset = editor.currentCaret().offset
        val lineStartOffset = editor.getLineStartForOffset(offset.point)
        match = sp.vim_regexec_multi(regmatch, editor, lcount, editor.currentCaret().getLine().line, searchcol)
        if ((!invert && match > 0) || (invert && match <= 0)) {
          globalExecuteOne(editor, context, lineStartOffset, cmd.toString())
        }
      } else {
        // pass 1: set marks for each (not) matching line
        val line1 = range.startLine
        val line2 = range.endLine
        //region search_regcomp implementation
        // We don't need to worry about lastIgnoreSmartCase, it's always false. Vim resets after checking, and it only sets
        // it to true when searching for a word with `*`, `#`, `g*`, etc.

        if (line1 < 0 || line2 < 0) {
          return false
        }

        var ndone = 0
        val marks = mutableListOf<RangeMarker>()
        for (lnum in line1..line2) {
          if (gotInt) break

          // a match on this line?
          match = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, searchcol)
          if ((!invert && match > 0) || (invert && match <= 0)) {
            val lineStartOffset = editor.getLineStartOffset(lnum)
            marks += editor.ij.document.createRangeMarker(lineStartOffset, lineStartOffset)
            ndone += 1
          }
          // TODO: 25.05.2021 Check break
        }

        // pass 2: execute the command for each line that has been marked
        if (gotInt) {
          VimPlugin.showMessage(message("e_interr"))
        } else if (ndone == 0) {
          if (invert) {
            VimPlugin.showMessage(message("global.command.not.found.v", pat.toString()))
          } else {
            VimPlugin.showMessage(message("global.command.not.found.g", pat.toString()))
          }
        } else {
          globalExe(editor, context, marks, cmd.toString())
        }
      }
    }
    return true
  }

  private fun globalExe(editor: VimEditor, context: ExecutionContext, marks: List<RangeMarker>, cmd: String) {
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
    if (cmd == null || cmd.isEmpty() || (cmd.length == 1 && cmd[0] == '\n')) {
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
