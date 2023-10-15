/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.history.HistoryConstants
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.regexp.CharacterClasses
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.VimRegexOptions
import com.maddyhome.idea.vim.register.RegisterConstants
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import org.jetbrains.annotations.TestOnly
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.*
import kotlin.math.max
import kotlin.math.min

public abstract class VimSearchGroupBase : VimSearchGroup {

  protected companion object {
    protected var lastPatternOffset: String? = ""
    protected var lastSearch: String? = ""
    protected var lastSubstitute: String? = ""
    protected var lastDirection: Direction = Direction.FORWARDS
    @JvmStatic
    protected var lastIgnoreSmartCase: Boolean = false
    protected var lastPatternType: PatternType? = null
    protected var lastSubstituteString: String? = null

  }

  protected abstract fun highlightSearchLines(
    editor: VimEditor,
    startLine: Int,
    endLine: Int,
  )

  protected abstract fun updateSearchHighlights(
    force: Boolean,
  )

  /**
   * Reset the search highlights to the last used pattern after highlighting incsearch results.
   */
  protected abstract fun resetIncsearchHighlights()

  protected abstract fun confirmChoice(
    editor: VimEditor,
    match: String,
    caret: VimCaret,
    startOffset: Int,
  ): ReplaceConfirmationChoice

  protected abstract fun parseVimScriptExpression(
    expressionString: String,
  ): Expression?

  protected abstract fun addSubstitutionConfirmationHighlight(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
  )

  protected abstract fun setLatestMatch(
    match: String,
  )

  protected abstract fun replaceString(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
    newString: String,
  )

  protected abstract fun resetSearchHighlight()

  abstract override fun clearSearchHighlight()

  // For substitute command
  private var do_all = false // do multiple substitutions per line
  private var do_ask = false // ask for confirmation
  private var do_error = true // if false, ignore errors
  private var do_ic: Boolean? = null // ignore case flag

  override var lastSearchPattern: String?
    get() = lastSearch
    set(value) { lastSearch = value }
  override var lastSubstitutePattern: String?
    get() = lastSubstitute
    set(value) { lastSubstitute = value }

  // TODO: this can be made not open when SearchGroup.java is removed
  protected open fun getLastUsedPattern(): String? {
    return when (lastPatternType) {
      PatternType.SEARCH -> lastSearch
      PatternType.SUBSTITUTE -> lastSubstitute
      else -> null
    }
  }

  override fun findUnderCaret(
    editor: VimEditor,
  ): TextRange? {
    val backSearch = searchBackward(editor, editor.primaryCaret().offset.point + 1, 1) ?: return null
    return if (backSearch.contains(editor.primaryCaret().offset.point)) backSearch else null
  }

  override fun searchBackward(
    editor: VimEditor,
    offset: Int,
    count: Int,
  ): TextRange? {
    // Backward search returns wrong end offset for some cases. That's why we should perform additional forward search
    val searchOptions = EnumSet.of(SearchOptions.WRAP, SearchOptions.WHOLE_FILE, SearchOptions.BACKWARDS)
    val foundBackward = injector.searchHelper.findPattern(editor, getLastUsedPattern(), offset, count, searchOptions) ?: return null
    var startOffset = foundBackward.startOffset - 1
    if (startOffset < 0) startOffset = editor.fileSize().toInt()
    searchOptions.remove(SearchOptions.BACKWARDS)
    return injector.searchHelper.findPattern(editor, getLastUsedPattern(), startOffset, 1, searchOptions)
  }

  override fun getNextSearchRange(
    editor: VimEditor,
    count: Int,
    forwards: Boolean,
  ): TextRange? {
    editor.removeSecondaryCarets()
    var current = findUnderCaret(editor)

    if (current == null || editor.inVisualMode && atEdgeOfGnRange(
        current,
        editor,
        forwards
      )
    ) {
      current = findNextSearchForGn(editor, count, forwards)
    } else if (count > 1) {
      current = findNextSearchForGn(editor, count - 1, forwards)
    }
    return current
  }

  private fun findNextSearchForGn(
    editor: VimEditor,
    count: Int,
    forwards: Boolean,
  ): TextRange? {
    return if (forwards) {
      val searchOptions = EnumSet.of(SearchOptions.WRAP, SearchOptions.WHOLE_FILE)
      injector.searchHelper.findPattern(
        editor,
        getLastUsedPattern(),
        editor.primaryCaret().offset.point,
        count,
        searchOptions
      )
    } else {
      searchBackward(editor, editor.primaryCaret().offset.point, count)
    }
  }


  private fun atEdgeOfGnRange(
    nextRange: TextRange,
    editor: VimEditor,
    forwards: Boolean,
  ): Boolean {
    val currentPosition: Int = editor.currentCaret().offset.point
    return if (forwards) {
      nextRange.endOffset - injector.visualMotionGroup.selectionAdj == currentPosition
    } else {
      nextRange.startOffset == currentPosition
    }
  }

  override fun processSearchRange(
    editor: VimEditor,
    pattern: String,
    patternOffset: Int,
    startOffset: Int,
    direction: Direction,
  ): Int {
    // Will set last pattern, required by findItOffset
    // IgnoreSmartCase and Direction are always reset.
    // PatternOffset is cleared before searching. ExRanges will add/subtract the line offset from the final search range
    // pattern, but we need the value to update lastPatternOffset for future searches.
    // TODO: Consider improving pattern offset handling
    setLastUsedPattern(pattern, PatternType.SEARCH, true)
    lastIgnoreSmartCase = false
    lastPatternOffset = "" // Do not apply a pattern offset yet!

    lastDirection = direction

    resetSearchHighlight()
    updateSearchHighlights(true)

    val result = findItOffset(editor, startOffset, 1, lastDirection)

    // Set lastPatternOffset AFTER searching, so it doesn't affect the result
    lastPatternOffset = if (patternOffset != 0) patternOffset.toString() else ""

    return result
  }

  override fun searchNext(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    return searchNextWithDirection(editor, caret, count, lastDirection)
  }

  override fun searchPrevious(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    return searchNextWithDirection(editor, caret, count, lastDirection.reverse())
  }

  private fun searchNextWithDirection(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    dir: Direction,
  ): Int {
    resetSearchHighlight()
    updateSearchHighlights(true)

    val startOffset: Int = caret.offset.point
    var offset = findItOffset(editor, startOffset, count, dir)
    if (offset == startOffset) {
      /* Avoid getting stuck on the current cursor position, which can
       * happen when an offset is given and the cursor is on the last char
       * in the buffer: Repeat with count + 1. */
      offset = findItOffset(editor, startOffset, count + 1, dir)
    }
    return offset
  }

  private fun skip_regexp(p: CharPointer, dirc: Char, magic: Boolean): CharPointer {
    var p = p
    var mymagic: Int
    mymagic = if (magic) {
      3
    } else {
      2
    }
    while (!p.end()) {
      if (p.charAt() == dirc) /* found end of regexp */ {
        break
      }
      if (p.charAt() == '[' && mymagic >= 3 ||
        p.charAt() == '\\' && p.charAt(1) == '[' && mymagic <= 2
      ) {
        p = skip_anyof(p.ref(1))
        if (p.end()) {
          break
        }
      } else if (p.charAt() == '\\' && p.charAt(1) != '\u0000') {
        p.inc() /* skip next character */
        if (p.charAt() == 'v') {
          mymagic = 4
        } else if (p.charAt() == 'V') {
          mymagic = 1
        }
      }
      p.inc()
    }
    return p
  }

  private val REGEXP_INRANGE = "]^-n\\"
  private val REGEXP_ABBR = "nrteb"
  private fun skip_anyof(p: CharPointer): CharPointer {
    if (p.charAt() == '^') /* Complement of range. */ {
      p.inc()
    }
    if (p.charAt() == ']' || p.charAt() == '-') {
      p.inc()
    }
    while (!p.end() && p.charAt() != ']') {
      if (p.charAt() == '-') {
        p.inc()
        if (!p.end() && p.charAt() != ']') {
          p.inc()
        }
      } else if (p.charAt() == '\\' &&
        (REGEXP_INRANGE.indexOf(p.charAt(1)) != -1 || REGEXP_ABBR.indexOf(p.charAt(1)) != -1)
      ) {
        p.inc(2)
      } else if (p.charAt() == '[') {
        if (skip_class_name(p) == CharacterClasses.CLASS_NONE) {
          p.inc() /* It was not a class name */
        }
      } else {
        p.inc()
      }
    }
    return p
  }

  private fun skip_class_name(pp: CharPointer): Int {
    var i: Int
    if (pp.charAt(1) == ':') {
      i = 0
      while (i < CharacterClasses.CLASS_NAMES.size) {
        if (pp.ref(2)
            .strncmp(CharacterClasses.CLASS_NAMES[i], CharacterClasses.CLASS_NAMES[i].length) == 0
        ) {
          pp.inc(CharacterClasses.CLASS_NAMES[i].length + 2)
          return i
        }
        ++i
      }
    }
    return CharacterClasses.CLASS_NONE
  }

  override fun processSearchCommand(
    editor: VimEditor,
    command: String,
    startOffset: Int,
    dir: Direction,
  ): Int {
    var isNewPattern = false
    var pattern: String? = null
    var patternOffset: String? = null

    val type = if (dir === Direction.FORWARDS) '/' else '?'

    if (command.isNotEmpty()) {
      if (command[0] != type) {
        val p = CharPointer(command)
        val end: CharPointer = skip_regexp(p.ref(0), type, true)
        pattern = p.substring(end.pointer() - p.pointer())
        isNewPattern = true
        if (p.charAt() == type) p.inc()
        patternOffset = if (end.charAt(0) == type) {
          end.inc()
          end.toString()
        } else {
          ""
        }
      } else if (command.length == 1) {
        patternOffset = ""
      } else {
        patternOffset = command.substring(1)
      }
    }

    if (pattern.isNullOrEmpty()) {
      pattern = lastSearch
      patternOffset = lastPatternOffset
      if (pattern.isNullOrEmpty()) {
        isNewPattern = true
        pattern = lastSubstitute
        if (pattern.isNullOrEmpty()) {
          injector.messages.showStatusBarMessage(null, "E35: No previous regular expression")
          return -1
        }
      }
    }

    // Only update the last pattern with a new input pattern. Do not update if we're reusing the last pattern
    setLastUsedPattern(pattern, PatternType.SEARCH, isNewPattern)

    lastIgnoreSmartCase = false
    lastPatternOffset = patternOffset // This might include extra search patterns separated by `;`

    lastDirection = dir

    resetSearchHighlight()
    updateSearchHighlights(true)

    return findItOffset(editor, startOffset, 1, lastDirection)
  }

  override fun searchWord(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    whole: Boolean,
    dir: Direction,
  ): Int {
    val range: TextRange = findWordUnderCursor(editor, caret) ?: return -1

    val start = range.startOffset
    val end = range.endOffset
    val pattern: String = if (whole) "\\<${editor.getText(start, end)}\\>" else editor.getText(start, end)

    // Updates last pattern, ready for findItOffset
    // Direction is always saved
    // IgnoreSmartCase is always set to true
    // There is no pattern offset available
    setLastUsedPattern(pattern, PatternType.SEARCH, true)
    lastIgnoreSmartCase = true
    lastPatternOffset = ""
    lastDirection = dir

    resetSearchHighlight()
    updateSearchHighlights(true)

    val offset = findItOffset(editor, range.startOffset, count, lastDirection)
    return if (offset == -1) range.startOffset else offset
  }

  /**
   * Find the word under the cursor or the next word to the right of the cursor on the current line.
   *
   * @param editor The editor to find the word in
   * @param caret  The caret to find word under
   * @return The text range of the found word or null if there is no word under/after the cursor on the line
   */
  private fun findWordUnderCursor(
    editor: VimEditor,
    caret: ImmutableVimCaret,
  ): TextRange? {

    val stop: Int = editor.getLineEndOffset(caret.getBufferPosition().line, true)
    val pos: Int = caret.offset.point

    // Technically the first condition is covered by the second one, but let it be
    if (editor.text().isEmpty() || editor.text().length <= pos) return null
    //if (pos == chars.length() - 1) return new TextRange(chars.length() - 1, chars.length());
    var start = pos
    val types = arrayOf(
      CharacterHelper.CharacterType.KEYWORD,
      CharacterHelper.CharacterType.PUNCTUATION
    )
    for (i in 0..1) {
      start = pos
      val type = CharacterHelper.charType(editor, editor.text()[start], false)
      if (type === types[i]) {
        // Search back for start of word
        while (start > 0 && CharacterHelper.charType(editor, editor.text()[start - 1], false) === types[i]) {
          start--
        }
      } else {
        // Search forward for start of word
        while (start < stop && CharacterHelper.charType(editor, editor.text()[start], false) !== types[i]) {
          start++
        }
      }
      if (start != stop) {
        break
      }
    }
    if (start == stop) {
      return null
    }
    // Special case 1 character words because 'findNextWordEnd' returns one to many chars
    val end: Int = if (start < stop &&
      (start >= editor.text().length - 1 ||
        CharacterHelper.charType(editor, editor.text()[start + 1], false) !== CharacterHelper.CharacterType.KEYWORD)
    ) {
      start + 1
    } else {
      injector.searchHelper.findNextWordEnd(editor, start, 1, bigWord = false, spaceWords = false) + 1
    }
    return TextRange(start, end)
  }

  override fun processSubstituteCommand(
    editor: VimEditor,
    caret: VimCaret,
    range: LineRange,
    excmd: String,
    exarg: String,
    parent: VimLContext,
  ): Boolean {
    // Explicitly exit visual mode here, so that visual mode marks don't change when we move the cursor to a match.
    val exceptions: MutableList<ExException> = ArrayList()
    if (editor.inVisualMode) editor.exitVisualMode()

    var cmd = CharPointer(StringBuffer(exarg))

    var which_pat = if ("~" == excmd) {
      // use last used regexp
      lastPatternType
    } else {
      PatternType.SUBSTITUTE // use last substitute regexp
    }

    val pat: CharPointer?
    val sub: CharPointer
    val delimiter: Char
    // new pattern and substitution
    if (excmd[0] == 's' && !cmd.isNul && !Character.isWhitespace(
        cmd.charAt()
      ) && "0123456789cegriIp|\"".indexOf(cmd.charAt()) == -1
    ) {
      // don't accept alphanumeric for separator
      if (CharacterClasses.isAlpha(cmd.charAt())) {
        injector.messages.showStatusBarMessage(null, "E146")
        return false
      }

      /*
       * undocumented vi feature:
       *  "\/sub/" and "\?sub?" use last used search pattern (almost like
       *  //sub/r).  "\&sub&" use last substitute pattern (like //sub/).
       */if (cmd.charAt() == '\\') {
        cmd.inc()
        if ("/?&".indexOf(cmd.charAt()) == -1) {
          injector.messages.showStatusBarMessage(null, "e_backslash")
          return false
        }
        if (cmd.charAt() != '&') {
          which_pat = PatternType.SEARCH // use last search pattern
        }
        pat = CharPointer("") // empty search pattern
        delimiter = cmd.charAt() // remember delimiter character
        cmd.inc()
      } else {
        // find the end of the regexp
        which_pat = lastPatternType // use last used regexp
        delimiter = cmd.charAt() // remember delimiter character
        cmd.inc()
        pat = cmd.ref(0) // remember start of search pat
        cmd = skip_regexp(cmd, delimiter, true)
        if (cmd.charAt() == delimiter) { // end delimiter found
          cmd.set('\u0000').inc() // replace it with a NUL
        }
      }

      /*
       * Small incompatibility: vi sees '\n' as end of the command, but in
       * Vim we want to use '\n' to find/substitute a NUL.
       */
      sub = cmd.ref(0) // remember the start of the substitution
      while (!cmd.isNul) {
        if (cmd.charAt() == delimiter) /* end delimiter found */ {
          cmd.set('\u0000').inc() /* replace it with a NUL */
          break
        }
        if (cmd.charAt(0) == '\\' && cmd.charAt(1).code != 0) /* skip escaped characters */ {
          cmd.inc()
        }
        cmd.inc()
      }
    } else {
      // use previous pattern and substitution
      if (lastSubstituteString == null) {
        // there is no previous command
        injector.messages.showStatusBarMessage(null, "e_nopresub")
        return false
      }
      pat = null
      sub = CharPointer(lastSubstituteString!!)
    }

    // Find trailing options.  When '&' is used, keep old options.
    if (cmd.charAt() == '&') {
      cmd.inc()
    } else {
      // :h :&& - "Note that :s and :& don't keep the flags"
      do_all = injector.options(editor).gdefault
      do_ask = false
      do_error = true
      do_ic = null
    }
    while (!cmd.isNul) {
      /*
       * Note that 'g' and 'c' are always inverted, also when p_ed is off.
       * 'r' is never inverted.
       */
      if (cmd.charAt() == 'g') {
        do_all = !do_all
      } else if (cmd.charAt() == 'c') {
        do_ask = !do_ask
      } else if (cmd.charAt() == 'e') {
        do_error = !do_error
      } else if (cmd.charAt() == 'r') {
        // use last used regexp
        which_pat = lastPatternType
      } else if (cmd.charAt() == 'i') {
        // ignore case
        do_ic = true
      } else if (cmd.charAt() == 'I') {
        /* don't ignore case */
        do_ic = false
      } else if (cmd.charAt() != 'p' && cmd.charAt() != 'l' && cmd.charAt() != '#' && cmd.charAt() != 'n') {
        // TODO: Support printing last changed line, with options for line number/list format
        // TODO: Support 'n' to report number of matches without substituting
        break
      }
      cmd.inc()
    }

    var line1 = range.startLine
    var line2 = range.endLine

    if (line1 < 0 || line2 < 0) {
      return false
    }

    // check for a trailing count
    cmd.skipWhitespaces()
    if (Character.isDigit(cmd.charAt())) {
      val i = cmd.digits
      if (i <= 0 && do_error) {
        injector.messages.showStatusBarMessage(null, "e_zerocount")
        return false
      }
      line1 = line2
      line2 = editor.normalizeLine(line1 + i - 1)
    }

    // check for trailing command or garbage
    cmd.skipWhitespaces()
    if (!cmd.isNul && cmd.charAt() != '"') {
      // if not end-of-line or comment
      injector.messages.showStatusBarMessage(null, "e_trailing")
      return false
    }


    val options: MutableList<VimRegexOptions> = ArrayList()
    if (injector.globalOptions().smartcase) options.add(VimRegexOptions.SMART_CASE)
    if (injector.globalOptions().ignorecase) options.add(VimRegexOptions.IGNORE_CASE)
    if (injector.globalOptions().wrapscan) options.add(VimRegexOptions.WRAP_SCAN)

    var isNewPattern = true
    var pattern: String? = ""
    if (pat == null || pat.isNul) {
      isNewPattern = false
      val errorMessage: String? = when (which_pat) {
        PatternType.SEARCH -> {
          pattern = lastSearch
          "E33: No previous substitute regular expression"
        }

        PatternType.SUBSTITUTE -> {
          pattern = lastSubstitute
          "E35: No previous regular expression"
        }
        else -> null
      }

      // Pattern was never defined
      if (pattern == null) {
        injector.messages.showStatusBarMessage(null, errorMessage)
        return false
      }
    } else {
      pattern = pat.toString()
    }

    // Set last substitute pattern, but only for explicitly typed patterns. Reused patterns are not saved/updated
    setLastUsedPattern(pattern, PatternType.SUBSTITUTE, isNewPattern)

    // Always reset after checking, only set for nv_ident
    lastIgnoreSmartCase = false

    // TODO: allow option to force (no)ignore case in a better way
    pattern = when (do_ic) {
      true -> "\\c$pattern"
      false -> "\\C$pattern"
      null -> pattern
    }

    val regex: VimRegex = try {
      VimRegex(pattern)
    } catch (e: VimRegexException) {
      injector.messages.showStatusBarMessage(editor, e.message)
      return false
    }

    val hasExpression = sub.charAt(0) == '\\' && sub.charAt(1) == '='

    val oldLastSubstituteString: String = lastSubstituteString ?: ""
    lastSubstituteString = sub.toString()

    resetSearchHighlight()
    updateSearchHighlights(true)

    var lastMatchStartOffset = -1
    var got_quit = false
    var column = 0
    var line = line1
    while (line <= line2 && !got_quit) {
      val substituteResult = regex.substitute(editor, sub.toString(), oldLastSubstituteString, line, column, hasExpression, options)
      if (substituteResult == null) {
        line++
        column = 0
        continue
      }

      injector.jumpService.saveJumpLocation(editor)
      val matchRange = substituteResult.first.range
      var expression: Expression? = null
      if (hasExpression) {
        val exprString = sub.toString().substring(2)
        expression = parseVimScriptExpression(exprString)
        if (expression == null) {
          exceptions.add(ExException("E15: Invalid expression: $exprString"))
          expression = SimpleExpression(VimString(""))
        }
      }
      var match = substituteResult.second
      lastMatchStartOffset = matchRange.startOffset

      var didReplace = false
      if (do_all || line != editor.lineCount()) {
        var doReplace = true
        if (do_ask) {
          addSubstitutionConfirmationHighlight(editor, matchRange.startOffset, matchRange.endOffset)

          val choice: ReplaceConfirmationChoice = confirmChoice(editor, match, caret, matchRange.startOffset)
          when (choice) {
            ReplaceConfirmationChoice.SUBSTITUTE_THIS -> {}
            ReplaceConfirmationChoice.SKIP -> doReplace = false
            ReplaceConfirmationChoice.SUBSTITUTE_ALL -> do_ask = false
            ReplaceConfirmationChoice.QUIT -> {
              doReplace = false
              got_quit = true
            }

            ReplaceConfirmationChoice.SUBSTITUTE_LAST -> {
              do_all = false
              line2 = line
            }
          }
        }
        if (doReplace) {
          setLatestMatch(editor.getText(TextRange(matchRange.startOffset, matchRange.endOffset)))
          caret.moveToOffset(matchRange.startOffset)
          if (expression != null) {
            match = try {
              expression.evaluate(editor, injector.executionContextManager.onEditor(editor, null), parent)
                .toInsertableString()
            } catch (e: Exception) {
              exceptions.add(e as ExException)
              ""
            }
          }

          val endPositionWithoutReplace = editor.offsetToBufferPosition(matchRange.endOffset)

          // FIXME: if we received an instance of MutableVimEditor this method might not be necessary
          replaceString(editor, matchRange.startOffset, matchRange.endOffset, match)
          didReplace = true

          val endPositionWithReplace = editor.offsetToBufferPosition(matchRange.startOffset + match.length)
          line += endPositionWithReplace.line - endPositionWithoutReplace.line
          line2 += endPositionWithReplace.line - endPositionWithoutReplace.line
        }
      }

      if (do_all && matchRange.startOffset != matchRange.endOffset) {
        if (didReplace) {
          // if there was a replacement, we start next search from where the new string ends
          val endPosition = editor.offsetToBufferPosition(matchRange.startOffset + match.length)
          line = endPosition.line
          column = endPosition.column
        } else {
          // no replacement, so start next search where the match ended
          val endPosition = editor.offsetToVisualPosition(matchRange.endOffset)
          column = endPosition.column
        }
      } else {
        column = 0
        line++
      }
    }

    if (!got_quit) {
      if (lastMatchStartOffset != -1) {
        caret.moveToOffset(
          injector.motion.moveCaretToLineStartSkipLeading(editor, editor.offsetToBufferPosition(lastMatchStartOffset).line)
        )
      } else {
        injector.messages.showStatusBarMessage(null, "E486: Pattern not found: $pattern")
      }
    }

    setLatestMatch("")

    // todo throw multiple exceptions at once
    if (exceptions.isNotEmpty()) {
      injector.messages.indicateError()
      injector.messages.showStatusBarMessage(null, exceptions[0].toString())
    }

    // TODO: Support reporting number of changes (:help 'report')
    return true
  }

  override fun search_regcomp(
    pat: CharPointer?,
    which_pat: Int,
    patSave: Int,
  ): Pair<Boolean, Triple<Any, String, Any>> {
    TODO("Remove once old engine is removed")
  }

  protected fun setLastUsedPattern(
    pattern: String,
    patternType: PatternType,
    isNewPattern: Boolean,
  ) {
    // Only update the last pattern with a new input pattern. Do not update if we're reusing the last pattern
    if (isNewPattern) {
      when (patternType) {
        PatternType.SEARCH -> {
          lastSearch = pattern
          lastPatternType = PatternType.SEARCH
        }
        PatternType.SUBSTITUTE -> {
          lastSubstitute = pattern
          lastPatternType = PatternType.SUBSTITUTE
        }
      }
    }

    // Vim never actually sets this register, but looks it up on request
    injector.registerGroup.storeTextSpecial(RegisterConstants.LAST_SEARCH_REGISTER, pattern)

    // This will remove an existing entry and add it back to the end, and is expected to do so even if the string value
    // is the same
    injector.historyGroup.addEntry(HistoryConstants.SEARCH, pattern)

  }

  override fun findDecimalNumber(line: String): Int? {
    val regex = Regex("\\d+")
    val range = regex.find(line)?.range ?: return null

    return line.substring(range.first, range.last + 1).toInt()
  }

  override fun getLastSearchDirection(): Direction {
    return lastDirection
  }

  /**
   * Searches for the last saved pattern, applying the last saved pattern offset. Will loop over trailing search
   * commands.
   *
   * Make sure that lastPatternOffset has been updated before calling this. wrapscan and ignorecase come from options.
   *
   *
   * See search.c:do_search (and a little bit of normal.c:normal_search)
   *
   * @param editor        The editor to search in
   * @param startOffset   The offset to search from
   * @param count         Find the nth occurrence
   * @param dir           The direction to search in
   * @return              The offset to the occurrence or -1 if not found
   */
  private fun findItOffset(
    editor: VimEditor,
    startOffset: Int,
    count: Int,
    dir: Direction,
  ): Int {
    var startOffsetMutable = startOffset
    var offset = 0
    var offsetIsLineOffset = false
    var hasEndOffset = false
    var pp = ParsePosition(0)
    if (lastPatternOffset!!.isNotEmpty()) {
      if (Character.isDigit(lastPatternOffset!![0]) || lastPatternOffset!![0] == '+' || lastPatternOffset!![0] == '-') {
        offsetIsLineOffset = true
        if (lastPatternOffset == "+") {
          offset = 1
        } else if (lastPatternOffset == "-") {
          offset = -1
        } else {
          if (lastPatternOffset!![0] == '+') {
            lastPatternOffset = lastPatternOffset!!.substring(1)
          }
          val nf = NumberFormat.getIntegerInstance()
          pp = ParsePosition(0)
          val num = nf.parse(lastPatternOffset, pp)
          if (num != null) {
            offset = num.toInt()
          }
        }
      } else if ("ebs".indexOf(lastPatternOffset!![0]) != -1) {
        if (lastPatternOffset!!.length >= 2) {
          if ("+-".indexOf(lastPatternOffset!![1]) != -1) {
            offset = 1
          }
          val nf = NumberFormat.getIntegerInstance()
          pp = ParsePosition(if (lastPatternOffset!![1] == '+') 2 else 1)
          val num = nf.parse(lastPatternOffset, pp)
          if (num != null) {
            offset = num.toInt()
          }
        }
        hasEndOffset = lastPatternOffset!![0] == 'e'
      }
    }

    /*
     * If there is a character offset, subtract it from the current
     * position, so we don't get stuck at "?pat?e+2" or "/pat/s-2".
     * Skip this if pos.col is near MAXCOL (closed fold).
     * This is not done for a line offset, because then we would not be vi
     * compatible.
     */
    if (!offsetIsLineOffset && offset != 0) {
      startOffsetMutable =
        max(0, min((startOffsetMutable - offset), (editor.text().length - 1)))
    }
    val searchOptions = EnumSet.of(SearchOptions.SHOW_MESSAGES, SearchOptions.WHOLE_FILE)
    if (dir === Direction.BACKWARDS) searchOptions.add(SearchOptions.BACKWARDS)
    if (lastIgnoreSmartCase) searchOptions.add(SearchOptions.IGNORE_SMARTCASE)
    if (hasEndOffset) searchOptions.add(SearchOptions.WANT_ENDPOS)

    // Uses last pattern. We know this is always set before being called
    val range = injector.searchHelper.findPattern(editor, getLastUsedPattern(), startOffsetMutable, count, searchOptions) ?: return -1

    var res = range.startOffset
    if (offsetIsLineOffset) {
      val line: Int = editor.offsetToBufferPosition(range.startOffset).line
      val newLine: Int = editor.normalizeLine(line + offset)

      // TODO: Don't move the caret!
      res = injector.motion.moveCaretToLineStart(editor, newLine)
    } else if (hasEndOffset || offset != 0) {
      val base = if (hasEndOffset) range.endOffset - 1 else range.startOffset
      res = max(0, min((base + offset), (editor.text().length - 1)))
    }
    var ppos = pp.index
    if (ppos < lastPatternOffset!!.length - 1 && lastPatternOffset!![ppos] == ';') {
      val nextDir: Direction = if (lastPatternOffset!![ppos + 1] == '/') {
        Direction.FORWARDS
      } else if (lastPatternOffset!![ppos + 1] == '?') {
        Direction.BACKWARDS
      } else {
        return res
      }
      if (lastPatternOffset!!.length - ppos > 2) {
        ppos++
      }
      res = processSearchCommand(editor, lastPatternOffset!!.substring(ppos + 1), res, nextDir)
    }
    return res
  }

  /**
   * Sets the last search state, purely for tests
   *
   * @param pattern         The pattern to save. This is the last search pattern, not the last substitute pattern
   * @param patternOffset   The pattern offset, e.g. `/{pattern}/{offset}`
   * @param direction       The direction to search
   */
  @TestOnly
  public fun setLastSearchState(
    pattern: String,
    patternOffset: String,
    direction: Direction,
  ) {
    setLastUsedPattern(pattern, PatternType.SEARCH, true)
    lastIgnoreSmartCase = false
    lastPatternOffset = patternOffset
    lastDirection = direction
  }

  /**
   * Resets the last state, purely for tests
   */
  @TestOnly
  public open fun resetState() {
    lastPatternType = PatternType.SEARCH
    lastSubstitute = null
    lastSearch = null
    lastPatternOffset = ""
    lastIgnoreSmartCase = false
    lastDirection = Direction.FORWARDS
  }


  protected enum class PatternType {
    SEARCH,
    SUBSTITUTE,
  }

  protected enum class ReplaceConfirmationChoice {
    SUBSTITUTE_THIS,
    SKIP,
    SUBSTITUTE_ALL,
    QUIT,
    SUBSTITUTE_LAST,
  }
}