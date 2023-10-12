/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchGroupBase
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeLine
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.CharacterPosition
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.TestInputModel.Companion.getInstance
import com.maddyhome.idea.vim.helper.addSubstitutionConfirmationHighlight
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.highlightSearchResults
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.helper.shouldIgnoreCase
import com.maddyhome.idea.vim.helper.updateSearchHighlights
import com.maddyhome.idea.vim.history.HistoryConstants
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.regexp.CharacterClasses
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.VimRegexOptions
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_SEARCH_REGISTER
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.ui.ModalEntry
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.handlers.SubmatchFunctionHandler
import org.jetbrains.annotations.TestOnly
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.*
import javax.swing.KeyStroke
import kotlin.math.max
import kotlin.math.min

public open class IjVimSearchGroup : VimSearchGroupBase() {

  init {
    // TODO: Investigate migrating these listeners to use the effective value change listener
    // This would allow us to update the editor we're told to update, rather than looping over all projects and updating
    // the highlights in that project's current document's open editors (see VIM-2779).
    // However, we probably only want to update the editors associated with the current document, so maybe the whole
    // code needs to be reworked. We're currently using the same update code for changes in the search term as well as
    // changes in the search options.
    VimPlugin.getOptionGroup().addGlobalOptionChangeListener(
      Options.hlsearch
    ) {
      showSearchHighlight = injector.globalOptions().hlsearch
      updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)
    }

    val updateHighlightsIfVisible = GlobalOptionChangeListener {
      if (showSearchHighlight) {
        updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)
      }
    }
    VimPlugin.getOptionGroup().addGlobalOptionChangeListener(Options.ignorecase, updateHighlightsIfVisible)
    VimPlugin.getOptionGroup().addGlobalOptionChangeListener(Options.smartcase, updateHighlightsIfVisible)
  }

  public companion object {
    private var lastPatternOffset: String? = ""
    private var lastSearch: String? = ""
    private var lastSubstitute: String? = ""
    private var lastDirection: Direction = Direction.FORWARDS
    private var lastIgnoreSmartCase: Boolean = false
    private var lastPatternType: PatternType? = null
    private var showSearchHighlight: Boolean = injector.globalOptions().hlsearch
    private var lastSubstituteString: String? = null


    public fun highlightSearchLines(
      editor: VimEditor,
      startLine: Int,
      endLine: Int,
    ) {
      val pattern = getLastUsedPattern()
      if (pattern != null) {
        val results = injector.searchHelper.findAll(
          editor, pattern, startLine, endLine,
          shouldIgnoreCase(pattern, lastIgnoreSmartCase)
        )
        highlightSearchResults(editor.ij, pattern, results, -1)
      }
    }

    private fun getLastUsedPattern(): String? {
      return when (lastPatternType) {
        PatternType.SEARCH -> lastSearch
        PatternType.SUBSTITUTE -> lastSubstitute
        else -> null
      }
    }
  }

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

  /**
   * Reset the search highlights to the last used pattern after highlighting incsearch results.
   */
  public open fun resetIncsearchHighlights() {
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)
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

    showSearchHighlight = injector.globalOptions().hlsearch
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)

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
    showSearchHighlight = injector.globalOptions().hlsearch
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)

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

    showSearchHighlight = injector.globalOptions().hlsearch
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)

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

    showSearchHighlight = injector.globalOptions().hlsearch
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)

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

    showSearchHighlight = injector.globalOptions().hlsearch
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)

    var lastMatch = -1
    var got_quit = false
    var column = 0
    var line = line1
    while (line <= line2 && !got_quit) {
      var newpos: CharacterPosition? = null
      val substituteResult =
        regex.substitute(editor, sub.toString(), oldLastSubstituteString, line, column, hasExpression, options)
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
        expression = com.maddyhome.idea.vim.vimscript.parser.VimscriptParser.parseExpression(exprString)
        if (expression == null) {
          exceptions.add(ExException("E15: Invalid expression: $exprString"))
          expression = SimpleExpression(VimString(""))
        }
      }
      var match = substituteResult.second
      val endpos = CharacterPosition(
        editor.offsetToBufferPosition(matchRange.endOffset).line,
        editor.offsetToBufferPosition(matchRange.endOffset).column
      )

      if (do_all || line != editor.lineCount()) {
        var doReplace = true
        if (do_ask) {
          val hl: RangeHighlighter = addSubstitutionConfirmationHighlight(
            editor.ij,
            matchRange.startOffset,
            matchRange.endOffset
          )
          editor.ij.markupModel.removeHighlighter(hl)

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
          SubmatchFunctionHandler.getInstance().latestMatch = (editor.getText(TextRange(matchRange.startOffset, matchRange.endOffset)))
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

          ApplicationManager.getApplication().runWriteAction {
            (editor as IjVimEditor).editor.document
              .replaceString(matchRange.startOffset, matchRange.endOffset, match)
          }

          lastMatch = matchRange.startOffset
          val newend = matchRange.startOffset + match.length
          newpos = CharacterPosition.fromOffset((editor as IjVimEditor).editor, newend)
          line += newpos.line - endpos.line
          line2 += newpos.line - endpos.line
        }
      }

      if (do_all && matchRange.startOffset != matchRange.endOffset) {
        if (newpos != null) {
          line = newpos.line
          column = newpos.column
        } else {
          column = endpos.column
        }
      } else {
        column = 0
        line++
      }
    }
    if (!got_quit) {
      if (lastMatch != -1) {
        caret.moveToOffset(
          injector.motion.moveCaretToLineStartSkipLeading(editor, editor.offsetToBufferPosition(lastMatch).line)
        )
      } else {
        injector.messages.showStatusBarMessage(null, "E486: Pattern not found: $pattern")
      }
    }

    SubmatchFunctionHandler.getInstance().latestMatch = ""

    // todo throw multiple exceptions at once
    if (exceptions.isNotEmpty()) {
      VimPlugin.indicateError()
      VimPlugin.showMessage(exceptions[0].toString())
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

  private fun setLastUsedPattern(
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
    VimPlugin.getRegister().storeTextSpecial(LAST_SEARCH_REGISTER, pattern)

    // This will remove an existing entry and add it back to the end, and is expected to do so even if the string value
    // is the same
    VimPlugin.getHistory().addEntry(HistoryConstants.SEARCH, pattern)

  }

  private fun confirmChoice(
    editor: VimEditor,
    match: String,
    caret: VimCaret,
    startoff: Int,
  ): ReplaceConfirmationChoice {
    val result: Ref<ReplaceConfirmationChoice> = Ref.create(ReplaceConfirmationChoice.QUIT)
    val keyStrokeProcessor: Function1<KeyStroke, Boolean> = label@{ key: KeyStroke ->
      val choice: ReplaceConfirmationChoice
      val c = key.keyChar
      choice = if (key.isCloseKeyStroke() || c == 'q') {
        ReplaceConfirmationChoice.QUIT
      } else if (c == 'y') {
        ReplaceConfirmationChoice.SUBSTITUTE_THIS
      } else if (c == 'l') {
        ReplaceConfirmationChoice.SUBSTITUTE_LAST
      } else if (c == 'n') {
        ReplaceConfirmationChoice.SKIP
      } else if (c == 'a') {
        ReplaceConfirmationChoice.SUBSTITUTE_ALL
      } else {
        return@label true
      }
      // TODO: Handle <C-E> and <C-Y>
      result.set(choice)
      false
    }
    if (ApplicationManager.getApplication().isUnitTestMode) {
      caret.moveToOffset(startoff)
      val inputModel = getInstance(editor.ij)
      var key = inputModel.nextKeyStroke()
      while (key != null) {
        if (!keyStrokeProcessor.invoke(key)) {
          break
        }
        key = inputModel.nextKeyStroke()
      }
    } else {
      // XXX: The Ex entry panel is used only for UI here, its logic might be inappropriate for this method
      val exEntryPanel: com.maddyhome.idea.vim.ui.ex.ExEntryPanel =
        com.maddyhome.idea.vim.ui.ex.ExEntryPanel.getInstanceWithoutShortcuts()
      val context = injector.executionContextManager.onEditor(editor, null)
      exEntryPanel.activate(
        editor.ij,
        (context as IjEditorExecutionContext).context,
        MessageHelper.message("replace.with.0", match),
        "",
        1
      )
      caret.moveToOffset(startoff)
      ModalEntry.activate(editor, keyStrokeProcessor)
      exEntryPanel.deactivate(true, false)
    }
    return result.get()
  }

  override fun findDecimalNumber(line: String): Int? {
    val regex = Regex("\\d+")
    val range = regex.find(line)?.range ?: return null

    return line.substring(range.first, range.last + 1).toInt()
  }

  override fun clearSearchHighlight() {
    showSearchHighlight = false
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, false)
  }

  override fun getLastSearchDirection(): Direction {
    return lastDirection
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

  @TestOnly
  public open fun resetState() {
    lastPatternType = PatternType.SEARCH
    lastSubstitute = null
    lastSearch = null
    lastPatternOffset = ""
    lastIgnoreSmartCase = false
    lastDirection = Direction.FORWARDS
    showSearchHighlight = injector.globalOptions().hlsearch
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
}

private enum class PatternType {
  SEARCH,
  SUBSTITUTE,
}

private enum class ReplaceConfirmationChoice {
  SUBSTITUTE_THIS,
  SKIP,
  SUBSTITUTE_ALL,
  QUIT,
  SUBSTITUTE_LAST ,
}