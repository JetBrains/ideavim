/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.history.HistoryConstants
import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptorBase
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.VimRegexOptions
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.register.RegisterConstants
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.functions.handlers.stringFunctions.SubmatchFunctionHandler
import org.jetbrains.annotations.TestOnly
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.*
import javax.swing.KeyStroke
import kotlin.math.max
import kotlin.math.min

abstract class VimSearchGroupBase : VimSearchGroup {

  protected companion object {
    /**
     * Last ignore smartcase option.
     */
    @JvmStatic
    protected var lastIgnoreSmartCase: Boolean = false

    /**
     * Last string trailing a pattern. E.g. in '/pattern/e+2', 'e+2' is trailing.
     */
    var lastPatternTrailing: String? = ""

    /**
     * Last used search direction.
     */
    var lastDirection: Direction = Direction.FORWARDS

    /**
     * The type of the last used pattern.
     */
    var lastPatternType: PatternType? = null

    /**
     * Last used substitute string.
     */
    private var lastSubstituteString: String? = null

    var lastReplaceString: String? = null

    private val CLASS_NAMES: List<String> = listOf(
      "alnum:]",
      "alpha:]",
      "blank:]",
      "cntrl:]",
      "digit:]",
      "graph:]",
      "lower:]",
      "print:]",
      "punct:]",
      "space:]",
      "upper:]",
      "xdigit:]",
      "tab:]",
      "return:]",
      "backspace:]",
      "escape:]",
    )
  }

  /**
   * Highlights lines startLine to endLine (inclusive), using the last used pattern.
   *
   * @param editor    The editor to highlight.
   * @param startLine The number of the line where to start highlighting (inclusive)
   * @param endLine   The number of the line where to stop highlighting (inclusive)
   */
  abstract fun highlightSearchLines(
    editor: VimEditor,
    startLine: Int,
    endLine: Int,
  )

  /**
   * Updates the current search highlights.
   *
   * @param force Whether to force this update.
   */
  protected abstract fun updateSearchHighlights(force: Boolean)

  /**
   * Reset the search highlights to the last used pattern after highlighting incsearch results.
   */
  abstract fun resetIncsearchHighlights()

  override fun updateSearchHighlightsAfterGlobalCommand() {
    setShouldShowSearchHighlights()
    updateSearchHighlights(false)
  }

  /**
   * Highlights the string that would be replaced (pending user confimation) in
   * a substitute command.
   *
   * @param editor      The editor where the substitution would be made.
   * @param startOffset The offset where the highlight should start
   * @param endOffset   The offset where the highlight should end
   */
  protected abstract fun addSubstitutionConfirmationHighlight(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
  ): SearchHighlight

  /**
   * Saves the latest matched string, for Vimscript purposes.
   *
   * @param match The match to save.
   */
  protected open fun setLatestMatch(match: String) {
    SubmatchFunctionHandler.getInstance().latestMatch = match
  }

  /**
   * Resets the variable that determines whether search highlights should be shown.
   */
  protected abstract fun setShouldShowSearchHighlights()

  abstract override fun clearSearchHighlight()

  /**
   * Whether to do multiple substitutions in the same line. 'g' flag.
   */
  private var doAll = false

  /**
   * Whether to ask for confirmation during substitution. 'c' flag.
   */
  private var doAsk = false

  /**
   * Whether to report errors. 'e' flag.
   */
  private var doError = true // if false, ignore errors

  /**
   * Whether to ignore case. 'i' or 'I' flags.
   * If null means to keep default settings.
   */
  private var doIgnorecase: Boolean? = null // ignore case flag

  override var lastSearchPattern: String? = null
  override var lastSubstitutePattern: String? = null

  // TODO: this can be made not open and private when SearchGroup.java is removed
  /**
   * Gets the latest used pattern for search or substitution.
   */
  protected open fun getLastUsedPattern(): String? {
    return when (lastPatternType) {
      PatternType.SEARCH -> lastSearchPattern
      PatternType.SUBSTITUTE -> lastSubstitutePattern
      else -> null
    }
  }

  // TODO: Delete this. Fix the mess that is the multiple entwined implementations of the search group
  // This function is required to fix a regression with the new regex engine - VIM-3348
  // This is only used from GlobalCommand in order to access and update last search/substitute patterns. Previously,
  // GlobalCommand would call VimSearchGroup.search_regcomp to build a regex based on the given pattern or last used
  // patterns. It would also update the last used patterns.
  // This method no longer exists, but we still need to be able to access and upate the last saved patterns.
  // We can't just use VimRegex directly, but need a method to create it with the right values. Perhaps we should move
  // GlobalCommand into VimSearchGroup? processGlobalCommand, just like we've got processSearchCommand and
  // processSubstituteCommand?
  fun prepareRegex(pat: CharPointer, whichPattern: Int, patternSave: Int): VimRegex {
    var isNewPattern = true
    var pattern: String? = ""
    if (pat.isNul) {
      isNewPattern = false
      val which = if (whichPattern == /*RE_LAST*/ 2) {
        if (lastPatternType == PatternType.SEARCH) /*RE_SEARCH*/ 0 else /*RE_SUBST*/ 1
      } else whichPattern
      val errorMessage = when (which) {
        /*RE_SEARCH*/ 0 -> {
          pattern = lastSearchPattern
          injector.messages.message(Msg.e_nopresub)
        }

        /*RE_SUBST*/ 1 -> {
          pattern = lastSubstitutePattern
          injector.messages.message("e_noprevre")
        }

        else -> null
      }

      // Pattern was never defined
      if (pattern == null) {
        throw ExException(errorMessage)
      }
    } else {
      pattern = pat.toString()
    }

    // Set RE_SUBST and RE_LAST, but only for explicitly typed patterns. Reused patterns are not saved/updated
    val patSave = when (patternSave) {
      /*RE_SEARCH*/ 0 -> PatternType.SEARCH
      /*RE_SUBST*/ 1 -> PatternType.SUBSTITUTE
      /*RE_BOTH*/ 2 -> PatternType.BOTH
      else -> throw ExException(injector.messages.message(Msg.e_invcmd))
    }
    setLastUsedPattern(pattern, patSave, isNewPattern)

    return VimRegex(pattern)
  }

  // TODO I think that this method (and the method above) should be part of the global command
  fun parseGlobalCommand(argument: String): GlobalCommandArguments? {
    var cmd = CharPointer(StringBuffer(argument))

    val pat: CharPointer
    val delimiter: Char
    var whichPat = 2 // RE_LAST

    val messages = injector.messages
    /*
     * undocumented vi feature:
     * "\/" and "\?": use previous search pattern.
     *   "\&": use previous substitute pattern.
     */
    if (argument.isEmpty()) {
      messages.showStatusBarMessage(null, messages.message("E148"))
      messages.indicateError()
      return null
    } else if (cmd.charAt() == '\\') {
      cmd.inc()
      if ("/?&".indexOf(cmd.charAt()) == -1) {
        messages.showStatusBarMessage(null, messages.message(Msg.e_backslash))
        return null
      }
      whichPat = if (cmd.charAt() == '&') 1 /* RE_SUBST */ else 0 /* RE_SEARCH */
      cmd.inc()
      pat = CharPointer("") /* empty search pattern */
    } else {
      delimiter = cmd.charAt() /* get the delimiter */
      cmd.inc()
      pat = cmd.ref(0) /* remember start of pattern */
      val endOfPattern = findEndOfPattern(cmd.toString(), delimiter)
      if (cmd.charAt(endOfPattern) == delimiter) {
        cmd.set('\u0000', endOfPattern)
      }
      // It is needed for the following [cmd.toString()]
      // `endOfPattern + 1` is delimiter and `endOfPattern + 2` is char after the delimiter (command start)
      cmd.pointer = endOfPattern + 2
    }
    return GlobalCommandArguments(pat, whichPat, cmd.toString())
  }

  data class GlobalCommandArguments(val pattern: CharPointer, val whichPattern: Int, val command: String)

  /****************************************************************************/
  /* Search related methods                                                   */
  /****************************************************************************/

  protected fun findUnderCaret(
    editor: VimEditor,
  ): TextRange? {
    val backSearch = searchBackward(editor, editor.primaryCaret().offset + 1, 1) ?: return null
    return if (backSearch.contains(editor.primaryCaret().offset)) backSearch else null
  }

  override fun searchBackward(
    editor: VimEditor,
    offset: Int,
    count: Int,
  ): TextRange? {
    // Backward search returns wrong end offset for some cases. That's why we should perform additional forward search
    val searchOptions = EnumSet.of(SearchOptions.WRAP, SearchOptions.WHOLE_FILE, SearchOptions.BACKWARDS)
    val foundBackward =
      injector.searchHelper.findPattern(editor, getLastUsedPattern(), offset, count, searchOptions) ?: return null
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
    lastPatternTrailing = "" // Do not apply a pattern offset yet!

    lastDirection = direction

    setShouldShowSearchHighlights()
    updateSearchHighlights(true)

    val result = findItOffset(editor, startOffset, 1, lastDirection)

    // Set lastPatternOffset AFTER searching, so it doesn't affect the result
    lastPatternTrailing = if (patternOffset != 0) patternOffset.toString() else ""

    return result?.first ?: -1
  }

  override fun searchNext(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    return searchNextWithDirection(editor, caret, count, lastDirection)
  }

  override fun searchPrevious(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    return searchNextWithDirection(editor, caret, count, lastDirection.reverse())
  }

  override fun processSearchCommand(
    editor: VimEditor,
    command: String,
    startOffset: Int,
    count1: Int,
    dir: Direction,
  ): Pair<Int, MotionType>? {

    var isNewPattern = false
    var pattern: String? = null
    var patternOffset: String? = null

    val type = if (dir === Direction.FORWARDS) '/' else '?'

    if (command.isNotEmpty()) {
      if (command[0] != type) {
        val endOfPattern = findEndOfPattern(command, type)
        pattern = command.substring(0, endOfPattern)
        isNewPattern = true
        patternOffset = if (endOfPattern < command.length) command.substring(endOfPattern + 1) else ""
      } else if (command.length == 1) {
        patternOffset = ""
      } else {
        patternOffset = command.substring(1)
      }
    }

    if (pattern.isNullOrEmpty()) {
      pattern = lastSearchPattern
      if (pattern.isNullOrEmpty()) {
        isNewPattern = true
        pattern = lastSubstitutePattern
        if (pattern.isNullOrEmpty()) {
          injector.messages.showStatusBarMessage(null, "E35: No previous regular expression")
          return null
        }
      }
      if (patternOffset.isNullOrEmpty()) {
        patternOffset = lastPatternTrailing
      }
    }

    // Only update the last pattern with a new input pattern. Do not update if we're reusing the last pattern
    setLastUsedPattern(pattern, PatternType.SEARCH, isNewPattern)

    lastIgnoreSmartCase = false
    lastPatternTrailing = patternOffset // This might include extra search patterns separated by `;`

    lastDirection = dir

    setShouldShowSearchHighlights()
    updateSearchHighlights(true)

    return findItOffset(editor, startOffset, count1, lastDirection)
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
    lastPatternTrailing = ""
    lastDirection = dir

    setShouldShowSearchHighlights()
    updateSearchHighlights(true)

    val offset = findItOffset(editor, range.startOffset, count, lastDirection)?.first ?: -1
    return if (offset == -1) range.startOffset else offset
  }

  override fun findEndOfPattern(
    command: String,
    delimiter: Char,
    startIndex: Int,
  ): Int {
    var magic = true

    var i = startIndex
    while (i < command.length) {
      // delimiter found
      if (command[i] == delimiter) break

      // collection start found, ignore until end of collection
      if (magic && command[i] == '[' ||
        !magic && command[i] == '\\' && i + 1 < command.length && command[i + 1] == '['
      ) {

        i = findEndOfCollection(command, i)
        // skip escaped char
      } else if (command[i] == '\\' && i + 1 < command.length) {
        i++
        // update magic
        if (command[i] == 'v' || command[i] == 'm') magic = true
        if (command[i] == 'V' || command[i] == 'M') magic = false
      }
      i++
    }
    return i
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
        editor.primaryCaret().offset,
        count,
        searchOptions
      )
    } else {
      searchBackward(editor, editor.primaryCaret().offset, count)
    }
  }


  private fun atEdgeOfGnRange(
    nextRange: TextRange,
    editor: VimEditor,
    forwards: Boolean,
  ): Boolean {
    val currentPosition: Int = editor.currentCaret().offset
    return if (forwards) {
      nextRange.endOffset - injector.visualMotionGroup.selectionAdj == currentPosition
    } else {
      nextRange.startOffset == currentPosition
    }
  }

  private fun searchNextWithDirection(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    dir: Direction,
  ): Int {
    setShouldShowSearchHighlights()
    updateSearchHighlights(false)

    val startOffset: Int = caret.offset
    var offset = findItOffset(editor, startOffset, count, dir)?.first ?: -1
    if (offset == startOffset) {
      /* Avoid getting stuck on the current cursor position, which can
       * happen when an offset is given and the cursor is on the last char
       * in the buffer: Repeat with count + 1. */
      offset = findItOffset(editor, startOffset, count + 1, dir)?.first ?: -1
    }
    return offset
  }

  private fun findEndOfCollection(
    command: String,
    startIndex: Int,
  ): Int {
    var i = startIndex
    while (i < command.length - 1) {
      // collection end found
      if (command[i] == ']') break

      // skip escaped char
      if (command[i] == '\\' && i + 1 < command.length) i++
      // skip character class
      else if (i + 1 < command.length && command[i] == '[' && command[i + 1] < ':') i =
        findEndOfCharacterClass(command, i + 2)

      i++
    }
    return i
  }

  private fun findEndOfCharacterClass(
    command: String,
    startIndex: Int,
  ): Int {
    for (charClass in CLASS_NAMES) {
      if (startIndex + charClass.length < command.length && command.substring(
          startIndex,
          startIndex + charClass.length
        ) == charClass
      )
      // char class found, skip to end of it
        return startIndex + charClass.length - 1
    }
    // there wasn't any valid character class
    return startIndex
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
    val pos: Int = caret.offset

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

  /****************************************************************************/
  /* Substitute related methods                                               */
  /****************************************************************************/
  override fun processSubstituteCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: LineRange,
    excmd: String,
    exarg: String,
    parent: VimLContext,
  ): Boolean {
    // Explicitly exit visual mode here, so that visual mode marks don't change when we move the cursor to a match.
    val exceptions: MutableList<ExException> = ArrayList()
    if (editor.inVisualMode) editor.exitVisualMode()

    // Parse Ex command and arguments to extract the pattern, substitute string, and line range
    val substituteCommandParse = parseSubstituteCommand(editor, range, excmd, exarg) ?: return false
    val pattern = substituteCommandParse.pattern
    val substituteString = substituteCommandParse.substituteString
    val line1 = substituteCommandParse.range.startLine
    var line2 = substituteCommandParse.range.endLine

    val options = enumSetOf<VimRegexOptions>()
    if (injector.globalOptions().smartcase) options.add(VimRegexOptions.SMART_CASE)
    if (injector.globalOptions().ignorecase) options.add(VimRegexOptions.IGNORE_CASE)

    val regex: VimRegex = try {
      VimRegex(pattern)
    } catch (e: VimRegexException) {
      injector.messages.showStatusBarMessage(editor, e.message)
      return false
    }

    val hasExpression = substituteString.length >= 2 && substituteString[0] == '\\' && substituteString[1] == '='

    val oldLastSubstituteString: String = lastSubstituteString ?: ""
    if (substituteString != "~") {
      lastSubstituteString = substituteString
    }

    setShouldShowSearchHighlights()
    updateSearchHighlights(true)

    if (!doAsk) {
      performSubstituteInLines(
        editor,
        caret,
        context,
        parent,
        regex,
        pattern,
        oldLastSubstituteString,
        line1,
        line2,
        0,
        hasExpression,
        substituteString,
        exceptions,
        options
      )
    } else {
      val lineToNextSubstitute = getNextSubstitute(
        editor,
        regex,
        oldLastSubstituteString,
        line1,
        line2,
        0,
        hasExpression,
        substituteString,
        options
      )
      if (lineToNextSubstitute == null) {
        injector.messages.indicateError()
        injector.messages.showStatusBarMessage(null, "E486: Pattern not found: $pattern")
        return true
      }
      val (line, nextSubstitute) = lineToNextSubstitute
      val matchRange = nextSubstitute.first.range
      caret.moveToOffset(matchRange.startOffset)
      val highlight = addSubstitutionConfirmationHighlight(editor, matchRange.startOffset, matchRange.endOffset)
      injector.modalInput.create(
        editor, context, injector.messages.message("replace.with.0", lineToNextSubstitute.second.second),
        SubstituteWithAskInputInterceptor(
          editor, caret, nextSubstitute, highlight, line, 0, parent, pattern, regex,
          oldLastSubstituteString, line2, hasExpression, substituteString, options,
          -1, mutableListOf(),
        )
      )
    }

    // TODO: Support reporting number of changes (:help 'report')
    return true
  }

  private fun getNextSubstitute(
    editor: VimEditor,
    regex: VimRegex,
    oldLastSubstituteString: String,
    startLine: Int,
    endLine: Int,
    column: Int,
    hasExpression: Boolean,
    substituteString: String,
    options: EnumSet<VimRegexOptions>,
  ): Pair<Int, Pair<VimMatchResult.Success, String>>? {
    for (line in startLine..endLine) {
      val col = if (line == startLine) column else 0
      val result =
        regex.substitute(editor, substituteString, oldLastSubstituteString, line, col, hasExpression, options)
      if (result != null) return line to result
    }
    return null
  }

  private fun prepareToSubstituteWithoutAsk(
    editor: VimEditor,
    regex: VimRegex,
    oldLastSubstituteString: String,
    line: Int,
    endLine: Int,
    column: Int,
    hasExpression: Boolean,
    substituteString: String,
    options: EnumSet<VimRegexOptions>,
  ): SubstitutePreparationResult {
    val substituteResult =
      regex.substitute(editor, substituteString, oldLastSubstituteString, line, column, hasExpression, options)
        ?: return SubstitutePreparationResult.Skip(line + 1, 0)
    val matchRange = substituteResult.first.range
    val match = substituteResult.second

    injector.jumpService.saveJumpLocation(editor)
    return SubstitutePreparationResult.Prepared(match, matchRange, endLine, doReplace = true, gotQuit = false)
  }

  private sealed interface SubstitutePreparationResult {
    data class Skip(val newLine: Int, val newColumn: Int) : SubstitutePreparationResult
    data class Prepared(
      val match: String,
      val matchRange: TextRange,
      val newEndLine: Int,
      val doReplace: Boolean,
      val gotQuit: Boolean,
    ) : SubstitutePreparationResult
  }

  private fun performSubstituteInLines(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    parent: VimLContext,
    regex: VimRegex,
    pattern: String,
    oldLastSubstituteString: String,
    startLine: Int,
    endLine: Int,
    startColumn: Int,
    hasExpression: Boolean,
    substituteString: String,
    exceptions: MutableList<ExException>,
    options: EnumSet<VimRegexOptions>,
  ) {
    var column = startColumn
    var line = startLine
    var line2 = endLine
    var lastMatchLine = -1
    while (line <= line2) {
      val preparationResult = prepareToSubstituteWithoutAsk(
        editor,
        regex,
        oldLastSubstituteString,
        line,
        line2,
        column,
        hasExpression,
        substituteString,
        options
      )
      if (preparationResult is SubstitutePreparationResult.Skip) {
        line = preparationResult.newLine
        column = preparationResult.newColumn
        continue
      }
      preparationResult as SubstitutePreparationResult.Prepared
      lastMatchLine = line

      val replaceResult = performReplace(
        editor,
        caret,
        context,
        parent,
        preparationResult.match,
        preparationResult.matchRange,
        preparationResult.doReplace,
        line,
        preparationResult.newEndLine,
        hasExpression,
        substituteString,
        exceptions
      )
      line = replaceResult.line
      column = replaceResult.column
      line2 = replaceResult.endLine
    }
    postSubstitute(editor, caret, pattern, gotQuit = false, lastMatchLine, exceptions)
  }

  private fun performReplace(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    parent: VimLContext,
    match: String,
    matchRange: TextRange,
    doReplace: Boolean,
    line: Int,
    endLine: Int,
    hasExpression: Boolean,
    substituteString: String,
    exceptions: MutableList<ExException>,
  ): ReplaceResult {
    caret.moveToOffset(matchRange.startOffset)
    setLatestMatch(editor.getText(TextRange(matchRange.startOffset, matchRange.endOffset)))
    val finalMatch = if (hasExpression) evaluateExpression(
      substituteString.substring(2),
      editor,
      context,
      parent,
      exceptions
    ) else match

    val newColumn: Int
    var newLine = line
    var newEndLine = endLine

    var didReplace = false

    if (doReplace) {
      val endPositionWithoutReplace = editor.offsetToBufferPosition(matchRange.endOffset)

      injector.application.runWriteAction {
        (editor as MutableVimEditor).replaceString(matchRange.startOffset, matchRange.endOffset, finalMatch)
      }
      didReplace = true

      val endPositionWithReplace = editor.offsetToBufferPosition(matchRange.startOffset + finalMatch.length)
      newLine += max(0, endPositionWithReplace.line - endPositionWithoutReplace.line)
      newEndLine += endPositionWithReplace.line - endPositionWithoutReplace.line
    }

    if (doAll && matchRange.startOffset != matchRange.endOffset) {
      if (didReplace) {
        // if there was a replacement, we start next search from where the new string ends
        val endPosition = editor.offsetToBufferPosition(matchRange.startOffset + finalMatch.length)
        newLine = endPosition.line
        newColumn = endPosition.column
      } else {
        // no replacement, so start next search where the match ended
        val endPosition = editor.offsetToVisualPosition(matchRange.endOffset)
        newColumn = endPosition.column
      }
    } else {
      newColumn = 0
      newLine++
    }
    return ReplaceResult(newLine, newColumn, newEndLine)
  }

  private data class ReplaceResult(val line: Int, val column: Int, val endLine: Int)

  private fun postSubstitute(
    editor: VimEditor,
    caret: VimCaret,
    pattern: String,
    gotQuit: Boolean,
    lastMatchLine: Int,
    exceptions: List<ExException>,
  ) {
    if (!gotQuit) {
      if (lastMatchLine != -1) {
        caret.moveToOffset(injector.motion.moveCaretToLineStartSkipLeading(editor, lastMatchLine))
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
  }

  private inner class SubstituteWithAskInputInterceptor(
    val editor: VimEditor,
    val caret: VimCaret,
    val nextSubstitute: Pair<VimMatchResult.Success, String>,
    val highlight: SearchHighlight,
    var line: Int,
    var column: Int,
    val parent: VimLContext,
    val pattern: String,
    val regex: VimRegex,
    val oldLastSubstituteString: String,
    var endLine: Int,
    val hasExpression: Boolean,
    val substituteString: String,
    val options: EnumSet<VimRegexOptions>,
    var lastMatchLine: Int,
    val exceptions: MutableList<ExException>,
  ) : VimInputInterceptorBase<ReplaceConfirmationChoice>() {
    private val modalInput: VimModalInput
      get() = injector.modalInput.getCurrentModalInput()!!
    private var gotQuit = false
    private var doReplace = true

    override fun buildInput(key: KeyStroke): ReplaceConfirmationChoice? {
      if (key.isCloseKeyStroke()) return ReplaceConfirmationChoice.QUIT
      return when (key.keyChar) {
        'q' -> ReplaceConfirmationChoice.QUIT
        'y' -> ReplaceConfirmationChoice.SUBSTITUTE_THIS
        'l' -> ReplaceConfirmationChoice.SUBSTITUTE_LAST
        'n' -> ReplaceConfirmationChoice.SKIP
        'a' -> ReplaceConfirmationChoice.SUBSTITUTE_ALL
        else -> null
      }
    }

    override fun executeInput(input: ReplaceConfirmationChoice, editor: VimEditor, context: ExecutionContext) {
      injector.application.runWriteCommand(editor, "substitute-with-confirmation", modalInput) {
        highlight.remove()
        injector.jumpService.saveJumpLocation(editor)
        val matchRange = nextSubstitute.first.range
        val match = nextSubstitute.second

        when (input) {
          ReplaceConfirmationChoice.SUBSTITUTE_THIS -> {}
          ReplaceConfirmationChoice.SKIP -> doReplace = false
          ReplaceConfirmationChoice.SUBSTITUTE_ALL -> {
            doAsk = false
            performSubstituteInLines(
              editor,
              caret,
              context,
              parent,
              regex,
              pattern,
              oldLastSubstituteString,
              line,
              endLine,
              column,
              hasExpression,
              substituteString,
              exceptions,
              options
            )
            closeModalInputPrompt()
            return@runWriteCommand
          }

          ReplaceConfirmationChoice.QUIT -> {
            doReplace = false
            gotQuit = true
          }

          ReplaceConfirmationChoice.SUBSTITUTE_LAST -> {
            doAll = false
            endLine = line
          }
        }
        lastMatchLine = line

        val replaceResult = performReplace(
          editor,
          caret,
          context,
          parent,
          match,
          matchRange,
          doReplace,
          line,
          endLine,
          hasExpression,
          substituteString,
          exceptions
        )
        line = replaceResult.line
        endLine = replaceResult.endLine
        column = replaceResult.column

        goToNextIteration()
      }
    }

    private fun goToNextIteration() {
      if (gotQuit) {
        afterAllSubstitutes()
        return
      }

      val lineToNextSubstitute = getNextSubstitute(
        editor,
        regex,
        oldLastSubstituteString,
        line,
        endLine,
        column,
        hasExpression,
        substituteString,
        options
      )
      if (lineToNextSubstitute == null) {
        afterAllSubstitutes()
      } else {
        val matchRange = lineToNextSubstitute.second.first.range
        caret.moveToOffset(matchRange.startOffset)
        val highlight = addSubstitutionConfirmationHighlight(editor, matchRange.startOffset, matchRange.endOffset)
        modalInput.inputInterceptor = SubstituteWithAskInputInterceptor(
          editor,
          caret,
          lineToNextSubstitute.second,
          highlight,
          lineToNextSubstitute.first,
          column,
          parent,
          pattern,
          regex,
          oldLastSubstituteString,
          endLine,
          hasExpression,
          substituteString,
          options,
          lastMatchLine,
          exceptions,
        )
      }
    }

    private fun afterAllSubstitutes() {
      postSubstitute(editor, caret, pattern, gotQuit, lastMatchLine, exceptions)
      closeModalInputPrompt()
    }
  }

  private fun evaluateExpression(
    exprString: String,
    editor: VimEditor,
    context: ExecutionContext,
    parent: VimLContext,
    exceptions: MutableList<ExException>,
  ): String {
    val expression = injector.vimscriptParser.parseExpression(exprString)
    return if (expression == null) {
      exceptions.add(ExException("E15: Invalid expression: $exprString"))
      ""
    } else {
      try {
        expression.evaluate(editor, context, parent).toInsertableString()
      } catch (e: Exception) {
        exceptions.add(e as ExException)
        ""
      }
    }
  }

  private fun parseSubstituteCommand(
    editor: VimEditor,
    range: LineRange,
    excmd: String,
    exarg: String,
  ): SubstituteCommandArguments? {
    var patternType = if ("~" == excmd) {
      // use last used regexp
      lastPatternType
    } else {
      PatternType.SUBSTITUTE // use last substitute regexp
    }

    var pattern: String? = ""
    val sub: String
    val delimiter: Char
    var trailingOptionsStartIndex = 0
    // new pattern and substitution
    if (excmd[0] == 's' && exarg.isNotEmpty() && !exarg.first()
        .isWhitespace() && !"0123456789cegriIp|\"".contains(exarg.first())
    ) {
      // don't accept alphanumeric for separator
      if (exarg.first().isLetter()) {
        injector.messages.showStatusBarMessage(null, "E146: Regular expressions can't be delimited by letters")
        return null
      }

      /*
       * undocumented vi feature:
       *  "\/sub/" and "\?sub?" use last used search pattern (almost like
       *  //sub/r).  "\&sub&" use last substitute pattern (like //sub/).
       */
      var substituteStringStartIndex = 0
      if (exarg.first() == '\\') {
        if (exarg.length < 2 || !"/?&".contains(exarg[1])) {
          injector.messages.showStatusBarMessage(null, "E10: \\ should be followed by /, ? or &")
          return null
        }
        if (exarg[1] != '&') {
          patternType = PatternType.SEARCH // use last search pattern
        }
        delimiter = exarg[1] // remember delimiter character
        substituteStringStartIndex += 2
      } else {
        // find the end of the regexp
        patternType = lastPatternType // use last used regexp
        delimiter = exarg.first() // remember delimiter character
        val endOfPattern = findEndOfPattern(exarg, delimiter, 1)
        pattern = exarg.substring(1, endOfPattern)
        if (pattern.isEmpty()) pattern = null
        substituteStringStartIndex = endOfPattern
        if (endOfPattern < exarg.length && exarg[endOfPattern] == delimiter) substituteStringStartIndex++
      }

      /*
       * Small incompatibility: vi sees '\n' as end of the command, but in
       * Vim we want to use '\n' to find/substitute a NUL.
       */
      var substituteStringEndIndex = exarg.length
      var i = substituteStringStartIndex
      while (i < exarg.length) {
        if (exarg[i] == delimiter) {
          substituteStringEndIndex = i
          break
        } else if (exarg[i] == '\\' && (i + 1) < exarg.length) {
          i++
        }
        i++
      }
      sub = exarg.substring(substituteStringStartIndex, substituteStringEndIndex)
      trailingOptionsStartIndex = substituteStringEndIndex + 1
    } else {
      // use previous pattern and substitution
      if (lastSubstituteString == null) {
        // there is no previous command
        injector.messages.showStatusBarMessage(null, "E33: No previous substitute regular expression")
        return null
      }
      pattern = null
      sub = lastSubstituteString!! + ""
    }

    // Find trailing options.  When '&' is used, keep old options.
    if (trailingOptionsStartIndex < exarg.length && exarg[trailingOptionsStartIndex] == '&') {
      trailingOptionsStartIndex++
    } else {
      // :h :&& - "Note that :s and :& don't keep the flags"
      doAll = injector.options(editor).gdefault
      doAsk = false
      doError = true
      doIgnorecase = null
    }
    var trailingOptionsEndIndex: Int = trailingOptionsStartIndex
    for (i in trailingOptionsStartIndex until exarg.length) {
      /*
       * Note that 'g' and 'c' are always inverted, also when p_ed is off.
       * 'r' is never inverted.
       */
      if (exarg[i] == 'g') {
        doAll = !doAll
      } else if (exarg[i] == 'c') {
        doAsk = !doAsk
      } else if (exarg[i] == 'e') {
        doError = !doError
      } else if (exarg[i] == 'r') {
        // use last used regexp
        patternType = lastPatternType
      } else if (exarg[i] == 'i') {
        // ignore case
        doIgnorecase = true
      } else if (exarg[i] == 'I') {
        // don't ignore case
        doIgnorecase = false
      } else if (exarg[i] != 'p' && exarg[i] != 'l' && exarg[i] != '#' && exarg[i] != 'n') {
        // TODO: Support printing last changed line, with options for line number/list format
        // TODO: Support 'n' to report number of matches without substituting
        break
      }
      trailingOptionsEndIndex++
    }

    var line1 = range.startLine
    var line2 = range.endLine

    if (line1 < 0 || line2 < 0) {
      return null
    }

    // check for a trailing count
    for (i in trailingOptionsEndIndex until exarg.length) if (exarg[i].isWhitespace()) trailingOptionsEndIndex++
    if (trailingOptionsEndIndex < exarg.length && exarg[trailingOptionsEndIndex].isDigit()) {
      var count = 0
      while (trailingOptionsEndIndex < exarg.length && exarg[trailingOptionsEndIndex].isDigit()) {
        count = count * 10 + exarg[trailingOptionsEndIndex].digitToInt()
        trailingOptionsEndIndex++
      }
      if (count <= 0 && doError) {
        injector.messages.showStatusBarMessage(null, "Zero count")
        return null
      }
      line1 = line2
      line2 = editor.normalizeLine(line1 + count - 1)
    }

    // check for trailing command or garbage
    if (trailingOptionsEndIndex < exarg.length && exarg[trailingOptionsEndIndex] != '"') {
      // if not end-of-line or comment
      injector.messages.showStatusBarMessage(null, "Trailing characters")
      return null
    }

    // check for trailing command or garbage
    if (trailingOptionsEndIndex < exarg.length && exarg[trailingOptionsEndIndex] != '"') {
      // if not end-of-line or comment
      injector.messages.showStatusBarMessage(null, "Trailing characters")
      return null
    }


    var isNewPattern = true
    if (pattern == null) {
      isNewPattern = false
      val errorMessage: String? = when (patternType) {
        PatternType.SEARCH -> {
          pattern = lastSearchPattern
          "E33: No previous substitute regular expression"
        }

        PatternType.SUBSTITUTE -> {
          pattern = lastSubstitutePattern
          "E35: No previous regular expression"
        }

        else -> null
      }

      // Pattern was never defined
      if (pattern == null) {
        injector.messages.showStatusBarMessage(null, errorMessage)
        return null
      }
    }

    // Set last substitute pattern, but only for explicitly typed patterns. Reused patterns are not saved/updated
    setLastUsedPattern(pattern, PatternType.SUBSTITUTE, isNewPattern)
    lastReplaceString = sub

    // Always reset after checking, only set for nv_ident
    lastIgnoreSmartCase = false

    // TODO: allow option to force (no)ignore case in a better way
    pattern = when (doIgnorecase) {
      true -> "\\c$pattern"
      false -> "\\C$pattern"
      null -> pattern
    }

    return SubstituteCommandArguments(
      pattern,
      sub,
      LineRange(line1, line2)
    )
  }
  /****************************************************************************/
  /* Helper methods                                                           */
  /****************************************************************************/

  /**
   * Set the last used pattern
   *
   * <p>Only updates the last used flag if the pattern is new. This prevents incorrectly setting the last used pattern
   * when search or substitute doesn't explicitly set the pattern but uses the last saved value. It also ensures the
   * last used pattern is updated when a new pattern with the same value is used.</p>
   *
   * <p>Also saves the text to the search register and history.</p>
   *
   * @param pattern       The pattern to remember
   * @param patternType   Which pattern to save - RE_SEARCH, RE_SUBST or RE_BOTH
   * @param isNewPattern  Flag to indicate if the pattern is new, or comes from a last used pattern. True means to
   *                      update the last used pattern index
   */
  private fun setLastUsedPattern(
    pattern: String,
    patternType: PatternType,
    isNewPattern: Boolean,
  ) {
    // Only update the last pattern with a new input pattern. Do not update if we're reusing the last pattern
    if (isNewPattern) {
      if (patternType == PatternType.SEARCH || patternType == PatternType.BOTH) {
        lastSearchPattern = pattern
        lastPatternType = PatternType.SEARCH
      }
      if (patternType == PatternType.SUBSTITUTE || patternType == PatternType.BOTH) {
        lastSubstitutePattern = pattern
        lastPatternType = PatternType.SUBSTITUTE
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
   * Searches for the last saved pattern, applying the last saved pattern trailing. Will loop over trailing search
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
   * @return              Pair containing the offset to the next occurrence of the pattern, and the [MotionType] based
   *                      on the search offset. The value will be `null` if no result is found.
   */
  private fun findItOffset(
    editor: VimEditor,
    startOffset: Int,
    count: Int,
    dir: Direction,
  ): Pair<Int, MotionType>? {

    var startOffsetMutable = startOffset
    var offset = 0
    var offsetIsLineOffset = false
    var hasEndOffset = false
    var pp = ParsePosition(0)
    if (lastPatternTrailing!!.isNotEmpty()) {
      if (Character.isDigit(lastPatternTrailing!![0]) || lastPatternTrailing!![0] == '+' || lastPatternTrailing!![0] == '-') {
        offsetIsLineOffset = true
        if (lastPatternTrailing == "+") {
          offset = 1
        } else if (lastPatternTrailing == "-") {
          offset = -1
        } else {
          if (lastPatternTrailing!![0] == '+') {
            lastPatternTrailing = lastPatternTrailing!!.substring(1)
          }
          val nf = NumberFormat.getIntegerInstance()
          pp = ParsePosition(0)
          val num = nf.parse(lastPatternTrailing, pp)
          if (num != null) {
            offset = num.toInt()
          }
        }
      } else if ("ebs".indexOf(lastPatternTrailing!![0]) != -1) {
        if (lastPatternTrailing!!.length >= 2) {
          if ("+-".indexOf(lastPatternTrailing!![1]) != -1) {
            offset = 1
          }
          val nf = NumberFormat.getIntegerInstance()
          pp = ParsePosition(if (lastPatternTrailing!![1] == '+') 2 else 1)
          val num = nf.parse(lastPatternTrailing, pp)
          if (num != null) {
            offset = num.toInt()
          }
        }
        hasEndOffset = lastPatternTrailing!![0] == 'e'
      }
    }

    // `/{pattern}/{offset}` is inclusive if offset contains `e`, and linewise if there's a line offset
    val motionType = when {
      offset != 0 && !hasEndOffset -> MotionType.LINE_WISE
      hasEndOffset -> MotionType.INCLUSIVE
      else -> MotionType.EXCLUSIVE
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
    if (injector.globalOptions().wrapscan) searchOptions.add(SearchOptions.WRAP)
    if (hasEndOffset) searchOptions.add(SearchOptions.WANT_ENDPOS)

    val pattern = getLastUsedPattern()
    if (!pattern.isNullOrEmpty()) {
      injector.messages.showStatusBarMessage(editor, (if (dir === Direction.FORWARDS) "/" else "?") + pattern)
    }

    // Uses last pattern. We know this is always set before being called
    val range =
      injector.searchHelper.findPattern(editor, pattern, startOffsetMutable, count, searchOptions) ?: return null

    var res = range.startOffset
    if (offsetIsLineOffset) {
      val line: Int = editor.offsetToBufferPosition(range.startOffset).line
      val newLine: Int = editor.normalizeLine(line + offset)
      res = injector.motion.moveCaretToLineStart(editor, newLine)
    } else if (hasEndOffset || offset != 0) {
      val base = if (hasEndOffset) range.endOffset - 1 else range.startOffset
      res = max(0, min((base + offset), (editor.text().length - 1)))
    }
    var ppos = pp.index
    if (ppos < lastPatternTrailing!!.length - 1 && lastPatternTrailing!![ppos] == ';') {
      val nextDir: Direction = if (lastPatternTrailing!![ppos + 1] == '/') {
        Direction.FORWARDS
      } else if (lastPatternTrailing!![ppos + 1] == '?') {
        Direction.BACKWARDS
      } else {
        return if (res == -1) null else Pair(res, motionType)
      }
      if (lastPatternTrailing!!.length - ppos > 2) {
        ppos++
      }
      res = processSearchCommand(editor, lastPatternTrailing!!.substring(ppos + 1), res, 1, nextDir)?.first ?: -1
    }
    return if (res == -1) null else Pair(res, motionType)
  }

  /**
   * Sets the last search state purely for tests
   *
   * @param pattern         The pattern to save. This is the last search pattern, not the last substitute pattern
   * @param patternOffset   The pattern offset, e.g. `/{pattern}/{offset}`
   * @param direction       The direction to search
   */
  @TestOnly
  fun setLastSearchState(
    pattern: String,
    patternOffset: String,
    direction: Direction,
  ) {
    setLastUsedPattern(pattern, PatternType.SEARCH, true)
    lastIgnoreSmartCase = false
    lastPatternTrailing = patternOffset
    lastDirection = direction
  }

  /**
   * Resets the last state, purely for tests
   */
  @TestOnly
  open fun resetState() {
    lastPatternType = PatternType.SEARCH
    lastSubstitutePattern = null
    lastSearchPattern = null
    lastPatternTrailing = ""
    lastIgnoreSmartCase = false
    lastDirection = Direction.FORWARDS
  }


  protected abstract class SearchHighlight {
    abstract fun remove()
  }

  protected enum class PatternType {
    SEARCH,
    SUBSTITUTE,
    BOTH
  }

  protected enum class ReplaceConfirmationChoice {
    SUBSTITUTE_THIS,
    SKIP,
    SUBSTITUTE_ALL,
    QUIT,
    SUBSTITUTE_LAST,
  }
}

private data class SubstituteCommandArguments(
  val pattern: String,
  val substituteString: String,
  val range: LineRange,
)
