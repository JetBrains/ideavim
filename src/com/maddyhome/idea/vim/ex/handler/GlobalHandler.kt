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
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.testFramework.tearDownProjectAndApp
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.group.HistoryGroup
import com.maddyhome.idea.vim.group.RegisterGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper.message
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.helper.shouldIgnoreCase
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.regexp.RegExp
import com.maddyhome.idea.vim.regexp.RegExp.regmmatch_T

class GlobalHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    var result = true
    for (caret in editor.caretModel.allCarets) {
      val lineRange = cmd.getLineRange(editor, caret)
      if (!processGlobalCommand(editor, caret, lineRange, cmd.command, cmd.argument)) {
        result = false
      }
    }
    return result
  }

  private fun processGlobalCommand(
    editor: Editor,
    caret: Caret,
    range: LineRange,
    excmd: String,
    exarg: String,
  ): Boolean {
    // TODO: 25.05.2021 Nesting command
    // TODO: 25.05.2021 ":global!" is like ":vglobal"


    // TODO: 25.05.2021 Multiple types
    val type = 'g'

    var cmd = CharPointer(StringBuffer(exarg))

    var pat: CharPointer
    var delimiter: Char
    var which_pat = RE_LAST

    /*
     * undocumented vi feature:
     * "\/" and "\?": use previous search pattern.
     *   "\&": use previous substitute pattern.
     */
    // TODO: 25.05.2021 Process empty expression
    if (cmd.charAt() == '\\') {
      cmd.inc()
      if ("/?&".indexOf(cmd.charAt()) == -1) {
        VimPlugin.showMessage(message(Msg.e_backslash))
        return false
      }
      if (cmd.charAt() == '&') {
        which_pat = RE_SUBST /* use last '/' pattern */
      } else {
        which_pat = RE_SEARCH /* use last '/' pattern */
      }
      cmd.inc()
      pat = CharPointer("") /* empty search pattern */
    } else {
      delimiter = cmd.charAt()  /* get the delimiter */
      cmd.inc() // // TODO: 25.05.2021 Here should be if
      pat = cmd.ref(0) /* remember start of pattern */
      cmd = RegExp.skip_regexp(cmd, delimiter, true)
      if (cmd.charAt() == delimiter) { /* end delimiter found */
        cmd.set('\u0000').inc() /* replace it with a NUL */
      }
    }

    //region search_regcomp implementation
    // We don't need to worry about lastIgnoreSmartCase, it's always false. Vim resets after checking, and it only sets
    // it to true when searching for a word with `*`, `#`, `g*`, etc.
    var isNewPattern = true
    var pattern: String? = ""
    if (pat == null || pat.isNul) {
      isNewPattern = false
      if (which_pat == RE_LAST) {
        which_pat = lastPatternIdx
      }
      var errorMessage: String? = null
      when (which_pat) {
        RE_SEARCH -> {
          pattern = lastSearch
          errorMessage = message("e_nopresub")
        }
        RE_SUBST -> {
          pattern = lastSubstitute
          errorMessage = message("e_noprevre")
        }
      }

      // Pattern was never defined
      if (pattern == null) {
        VimPlugin.showMessage(errorMessage)
        return false
      }
    } else {
      pattern = pat.toString()
    }

    // Set RE_SUBST and RE_LAST, but only for explicitly typed patterns. Reused patterns are not saved/updated

    // Set RE_SUBST and RE_LAST, but only for explicitly typed patterns. Reused patterns are not saved/updated
    setLastUsedPattern(pattern, RE_SUBST, isNewPattern)

    // Always reset after checking, only set for nv_ident

    // Always reset after checking, only set for nv_ident
    lastIgnoreSmartCase = false
    // Substitute does NOT reset last direction or pattern offset!

    // Substitute does NOT reset last direction or pattern offset!
    val sp: RegExp
    val regmatch = regmmatch_T()
    regmatch.rmm_ic = shouldIgnoreCase(pattern, false)
    sp = RegExp()
    regmatch.regprog = sp.vim_regcomp(pattern, 1)
    if (regmatch.regprog == null) {
      if (do_error) {
        VimPlugin.showMessage(message(Msg.e_invcmd))
      }
      return false
    }
    //endregion

    // TODO: 25.05.2021 global busy

    // pass 1: set marks for each (not) matching line
    val line1 = range.startLine
    val line2 = range.endLine
    var match: Int
    //region search_regcomp implementation
    // We don't need to worry about lastIgnoreSmartCase, it's always false. Vim resets after checking, and it only sets
    // it to true when searching for a word with `*`, `#`, `g*`, etc.


    if (line1 < 0 || line2 < 0) {
      return false
    }

    val lcount = EditorHelper.getLineCount(editor)
    val searchcol = 0
    var ndone = 0
    val marks = mutableListOf<RangeHighlighter>()
    for (lnum in line1..line2) {
      // TODO: 25.05.2021 recheck gotInt
      if (!gotInt) break

      // a match on this line?
      match = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, searchcol)
      if ((type == 'g' && match > 0) || (type == 'v' && match <= 0)) {
        // TODO: 25.05.2021 Use another way to mark things?
        marks += editor.markupModel.addLineHighlighter(null, lnum, 0)
        ndone += 1;
      }
      // TODO: 25.05.2021 Check break
    }

    // pass 2: execute the command for each line that has been marked
    if (gotInt) {
      // TODO: 25.05.2021
    }
    else if (ndone == 0) {
      // TODO: 25.05.2021
    }
    else {
      globalExe(marks)
    }
    // TODO: 25.05.2021 More staff
    return true
  }

  private fun globalExe(marks: List<RangeHighlighter>) {
    TODO("Not yet implemented")
  }

  /**
   * Set the last used pattern
   *
   *
   * Only updates the last used flag if the pattern is new. This prevents incorrectly setting the last used pattern
   * when search or substitute doesn't explicitly set the pattern but uses the last saved value. It also ensures the
   * last used pattern is updated when a new pattern with the same value is used.
   *
   *
   * Also saves the text to the search register and history.
   *
   * @param pattern       The pattern to remember
   * @param which_pat     Which pattern to save - RE_SEARCH, RE_SUBST or RE_BOTH
   * @param isNewPattern  Flag to indicate if the pattern is new, or comes from a last used pattern. True means to
   * update the last used pattern index
   */
  private fun setLastUsedPattern(pattern: String, which_pat: Int, isNewPattern: Boolean) {
    // Only update the last pattern with a new input pattern. Do not update if we're reusing the last pattern
    // TODO: RE_BOTH isn't used in IdeaVim yet. Should be used for the global command
    if ((which_pat == RE_SEARCH || which_pat == RE_BOTH) && isNewPattern) {
      lastSearch = pattern
      lastPatternIdx = RE_SEARCH
    }
    if ((which_pat == RE_SUBST || which_pat == RE_BOTH) && isNewPattern) {
      lastSubstitute = pattern
      lastPatternIdx = RE_SUBST
    }

    // Vim never actually sets this register, but looks it up on request
    VimPlugin.getRegister().storeTextSpecial(RegisterGroup.LAST_SEARCH_REGISTER, pattern)

    // This will remove an existing entry and add it back to the end, and is expected to do so even if the string value
    // is the same
    VimPlugin.getHistory().addEntry(HistoryGroup.SEARCH, pattern)
  }

  companion object {
    var gotInt: Boolean = false
    private var lastPatternIdx = 0 // Which pattern was used last? RE_SEARCH or RE_SUBST?
    private var lastSearch: String? = null // Pattern used for last search command (`/`)
    private var lastSubstitute: String? = null // Pattern used for last substitute command (`:s`)
    private var lastIgnoreSmartCase = false
    private const val do_error = true /* if false, ignore errors */


    // Matching the values defined in Vim. Do not change these values, they are used as indexes
    private const val RE_SEARCH = 0 // Save/use search pattern
    private const val RE_SUBST = 1 // Save/use substitute pattern
    private const val RE_BOTH = 2 // Save to both patterns
    private const val RE_LAST = 2 // Use last used pattern if "pat" is NULL
  }
}