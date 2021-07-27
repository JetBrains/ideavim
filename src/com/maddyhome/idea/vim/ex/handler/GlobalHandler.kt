/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandParser
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.group.SearchGroup.RE_BOTH
import com.maddyhome.idea.vim.group.SearchGroup.RE_LAST
import com.maddyhome.idea.vim.group.SearchGroup.RE_SEARCH
import com.maddyhome.idea.vim.group.SearchGroup.RE_SUBST
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper.message
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.regexp.RegExp

class GlobalHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    var result = true
    editor.caretModel.removeSecondaryCarets()
    val caret = editor.caretModel.currentCaret

    // For :g command the default range is %
    val lineRange: LineRange = if (cmd.ranges.size() == 0) {
      LineRange(0, editor.document.lineCount - 1)
    } else {
      cmd.getLineRange(editor, caret)
    }
    if (!processGlobalCommand(editor, context, lineRange, cmd.command, cmd.argument)) {
      result = false
    }
    return result
  }

  private fun processGlobalCommand(
    editor: Editor,
    context: DataContext,
    range: LineRange,
    excmd: String,
    _exarg: String,
  ): Boolean {
    // When nesting the command works on one line.  This allows for
    // ":g/found/v/notfound/command".
    if (globalBusy && (range.startLine != 0 || range.endLine != editor.document.lineCount - 1)) {
      VimPlugin.showMessage(message("E147"))
      VimPlugin.indicateError()
      return false
    }

    var exarg = _exarg
    val type = when {
      exarg.startsWith("!") -> {
        exarg = exarg.drop(1)
        GlobalType.V
      }
      excmd.startsWith("g") -> GlobalType.G
      excmd.startsWith("v") -> GlobalType.V
      else -> error("Unexpected command: $excmd")
    }

    var cmd = CharPointer(StringBuffer(exarg))

    val pat: CharPointer
    val delimiter: Char
    var whichPat = RE_LAST

    /*
     * undocumented vi feature:
     * "\/" and "\?": use previous search pattern.
     *   "\&": use previous substitute pattern.
     */
    if (exarg.isEmpty()) {
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

    val (first, second) = VimPlugin.getSearch().search_regcomp(pat, whichPat, RE_BOTH)
    if (!first) {
      VimPlugin.showMessage(message(Msg.e_invcmd))
      VimPlugin.indicateError()
      return false
    }
    val regmatch = second.getFirst()
    val sp = second.getThird()

    var match: Int
    val lcount = EditorHelper.getLineCount(editor)
    val searchcol = 0
    if (globalBusy) {
      val offset = editor.caretModel.currentCaret.offset
      val lineStartOffset = editor.document.getLineStartOffset(editor.document.getLineNumber(offset))
      match = sp.vim_regexec_multi(regmatch, editor, lcount, lineStartOffset, searchcol)
      if ((type == GlobalType.G && match > 0) || (type == GlobalType.V && match <= 0)) {
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
        if ((type == GlobalType.G && match > 0) || (type == GlobalType.V && match <= 0)) {
          val lineStartOffset = editor.document.getLineStartOffset(lnum)
          marks += editor.document.createRangeMarker(lineStartOffset, lineStartOffset)
          ndone += 1
        }
        // TODO: 25.05.2021 Check break
      }

      // pass 2: execute the command for each line that has been marked
      if (gotInt) {
        VimPlugin.showMessage(message("e_interr"))
      } else if (ndone == 0) {
        if (type == GlobalType.V) {
          VimPlugin.showMessage(message("global.command.not.found.v", pat.toString()))
        } else {
          VimPlugin.showMessage(message("global.command.not.found.g", pat.toString()))
        }
      } else {
        globalExe(editor, context, marks, cmd.toString())
      }
    }
    return true
  }

  private fun globalExe(editor: Editor, context: DataContext, marks: List<RangeMarker>, cmd: String) {
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

  private fun globalExecuteOne(editor: Editor, context: DataContext, lineStartOffset: Int, cmd: String?) {
    // TODO: 26.05.2021 What about folds?
    editor.caretModel.moveToOffset(lineStartOffset)
    if (cmd == null || cmd.isEmpty() || (cmd.length == 1 && cmd[0] == '\n')) {
      CommandParser.processCommand(editor, context, "p", 1, skipHistory = true)
    } else {
      CommandParser.processCommand(editor, context, cmd, 1, skipHistory = true)
    }
  }

  private enum class GlobalType {
    G,
    V,
  }

  companion object {
    private var globalBusy = false

    // Interrupted. Not used at the moment
    var gotInt: Boolean = false
  }
}
