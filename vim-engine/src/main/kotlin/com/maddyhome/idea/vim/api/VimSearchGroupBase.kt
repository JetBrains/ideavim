/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.history.HistoryConstants
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.regexp.CharacterClasses
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_SEARCH_REGISTER
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.*
import kotlin.math.max
import kotlin.math.min

public abstract class VimSearchGroupBase : VimSearchGroup {

  private var lastPatternOffset: String? = ""
  private var lastSearch: String? = ""
  private var lastSubstitute: String? = ""
  private var lastDirection: Direction = Direction.FORWARDS
  private var lastIgnoreSmartCase: Boolean = false
  private var lastPatternType: PatternType? = null
  private var showSearchHighlight: Boolean = injector.globalOptions().hlsearch

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