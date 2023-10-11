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
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.history.HistoryConstants
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.regexp.CharacterClasses
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_SEARCH_REGISTER
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.*
import kotlin.math.max
import kotlin.math.min

public open class VimSearchGroupBase : VimSearchGroup {

  private var lastPatternOffset: String? = ""
  private var lastSearch: String? = ""
  private var lastSubstitute: String? = ""
  private var lastDirection: Direction = Direction.FORWARDS
  private var lastIgnoreSmartCase: Boolean = false
  private var lastPatternType: PatternType? = null
  private var showSearchHighlight: Boolean = injector.globalOptions().hlsearch
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
    // Will set RE_LAST, required by findItOffset
    // IgnoreSmartCase and Direction are always reset.
    // PatternOffset is cleared before searching. ExRanges will add/subtract the line offset from the final search range
    // pattern, but we need the value to update lastPatternOffset for future searches.
    // TODO: Consider improving pattern offset handling
    lastSearch = pattern
    lastPatternType = PatternType.SEARCH
    lastIgnoreSmartCase = false
    lastPatternOffset = "" // Do not apply a pattern offset yet!

    lastDirection = direction

    // TODO: Highlighting!
    // resetShowSearchHighlight()
    // forceUpdateSearchHighlights()

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
    // TODO: Highlighting!
    // resetShowSearchHighlight()
    // updateSearchHighlights()
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
    if (isNewPattern) {
      lastSearch = pattern
      lastPatternType = PatternType.SEARCH
    }

    // Vim never actually sets this register, but looks it up on request
    injector.registerGroup.storeTextSpecial(LAST_SEARCH_REGISTER, pattern)
    // This will remove an existing entry and add it back to the end, and is expected to do so even if the string value is the same
    injector.historyGroup.addEntry(HistoryConstants.SEARCH, pattern)

    lastIgnoreSmartCase = false
    lastPatternOffset = patternOffset // This might include extra search patterns separated by `;`

    lastDirection = dir

    showSearchHighlight = injector.globalOptions().hlsearch
    // TODO: Update search highlights. We don't have access to the SearchHighlightsHelper class here!
    // forceUpdateSearchHighlights()

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

    // Updates RE_LAST, ready for findItOffset
    // Direction is always saved
    // IgnoreSmartCase is always set to true
    // There is no pattern offset available
    lastSearch = pattern
    lastPatternType = PatternType.SEARCH
    lastIgnoreSmartCase = true
    lastPatternOffset = ""
    lastDirection = dir

    // TODO: Highlighting!
    // resetShowSearchHighlight()
    // forceUpdateSearchHighlights()

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
      val type = charType(editor, editor.text()[start], false)
      if (type === types[i]) {
        // Search back for start of word
        while (start > 0 && charType(editor, editor.text()[start - 1], false) === types[i]) {
          start--
        }
      } else {
        // Search forward for start of word
        while (start < stop && charType(editor, editor.text()[start], false) !== types[i]) {
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
        charType(editor, editor.text()[start + 1], false) !== CharacterHelper.CharacterType.KEYWORD)
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
    TODO("Not yet implemented")
  }

  override fun search_regcomp(
    pat: CharPointer?,
    which_pat: Int,
    patSave: Int,
  ): Pair<Boolean, Triple<Any, String, Any>> {
    TODO("Not yet implemented")
  }

  override fun findDecimalNumber(line: String): Int? {
    val regex = Regex("\\d+")
    val range = regex.find(line)?.range ?: return null

    return line.substring(range.first, range.last + 1).toInt()
  }

  override fun clearSearchHighlight() {
    showSearchHighlight = false
    // TODO: Highlighting
    // updateSearchHighlights()
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

  private fun getLastUsedPattern(): String? {
    return when (lastPatternType) {
      PatternType.SEARCH -> lastSearch
      PatternType.SUBSTITUTE -> lastSubstitute
      else -> null
    }
  }
}

private enum class PatternType {
  SEARCH,
  SUBSTITUTE,
}