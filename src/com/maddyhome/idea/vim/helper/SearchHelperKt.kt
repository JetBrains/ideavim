/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.helper.SearchHelper.findPositionOfFirstCharacter
import com.maddyhome.idea.vim.option.OptionsManager.ignorecase
import com.maddyhome.idea.vim.option.OptionsManager.smartcase

enum class Direction(private val value: Int) {
  BACKWARDS(-1), FORWARDS(1);

  fun toInt(): Int = value
  fun reverse(): Direction = when (this) {
    BACKWARDS -> FORWARDS
    FORWARDS -> BACKWARDS
  }

  companion object {
    fun fromInt(value: Int) = when (value) {
      BACKWARDS.value -> BACKWARDS
      FORWARDS.value -> FORWARDS
      else -> FORWARDS
    }
  }
}

enum class SearchOptions {
  BACKWARDS, WANT_ENDPOS, WRAP, SHOW_MESSAGES, WHOLE_FILE, IGNORE_SMARTCASE
}

private data class State(val position: Int, val trigger: Char, val inQuote: Boolean?, val lastOpenSingleQuotePos: Int)

// bounds are considered inside corresponding quotes
fun checkInString(chars: CharSequence, currentPos: Int, str: Boolean): Boolean {
  val begin = findPositionOfFirstCharacter(chars, currentPos, setOf('\n'), false, Direction.BACKWARDS)?.second ?: 0
  val changes = quoteChanges(chars, begin)
  // TODO: here we need to keep only the latest element in beforePos (if any) and
  //   don't need atAndAfterPos to be eagerly collected
  var (beforePos, atAndAfterPos) = changes.partition { it.position < currentPos }

  var (atPos, afterPos) = atAndAfterPos.partition { it.position == currentPos }
  assert(atPos.size <= 1) { "Multiple characters at position $currentPos in string $chars" }
  if (atPos.isNotEmpty()) {
    val atPosChange = atPos[0]
    if (afterPos.isEmpty()) {
      // it is situation when cursor is on closing quote, so we must consider that we are inside quotes pair
      afterPos = afterPos.toMutableList()
      afterPos.add(atPosChange)
    } else {
      // it is situation when cursor is on opening quote, so we must consider that we are inside quotes pair
      beforePos = beforePos.toMutableList()
      beforePos.add(atPosChange)
    }
  }

  val lastBeforePos = beforePos.lastOrNull()

  // if opening quote was found before pos (inQuote=true), it doesn't mean pos is in string, we need
  // to find closing quote to be sure
  var posInQuote = lastBeforePos?.inQuote?.let { if (it) null else it }

  val lastOpenSingleQuotePosBeforeCurrentPos = lastBeforePos?.lastOpenSingleQuotePos ?: -1
  var posInChar = if (lastOpenSingleQuotePosBeforeCurrentPos == -1) false else null

  var inQuote: Boolean? = null

  for((_, trigger, inQuoteAfter, lastOpenSingleQuotePosAfter) in afterPos) {
    inQuote = inQuoteAfter
    if (posInQuote != null && posInChar != null) break
    if (posInQuote == null && inQuoteAfter != null) {
      // if we found double quote
      if (trigger == '"') {
        // then previously it has opposite value
        posInQuote = !inQuoteAfter
        // if we found single quote
      } else if (trigger == '\'') {
        // then we found closing single quote
        posInQuote = inQuoteAfter
      }
    }
    if (posInChar == null && lastOpenSingleQuotePosAfter != lastOpenSingleQuotePosBeforeCurrentPos) {
      // if we found double quote and we reset position of last single quote
      if (trigger == '"' && lastOpenSingleQuotePosAfter == -1) {
        // then it means previously there supposed to be open single quote
        posInChar = false
        // if we found single quote
      } else if (trigger == '\'') {
        // if we reset position of last single quote
        // it means we found closing single quote
        // else it means we found opening single quote
        posInChar = lastOpenSingleQuotePosAfter == -1
      }
    }
  }

  return if (str) posInQuote != null && posInQuote && (inQuote == null || !inQuote) else posInChar != null && posInChar
}

// yields changes of inQuote and lastOpenSingleQuotePos during while iterating over chars
// rules are that:
// - escaped quotes are skipped
// - single quoted group may enclose only one character, maybe escaped,
// - so distance between opening and closing single quotes cannot be more than 3
// - bounds are considered inside corresponding quotes
private fun quoteChanges(chars: CharSequence, begin: Int) = sequence {
  // position of last found unpaired single quote
  var lastOpenSingleQuotePos = -1
  // whether we are in double quotes
  // true - definitely yes
  // false - definitely no
  // null - maybe yes, in case we found such combination: '"
  //   in that situation it may be double quote inside single quotes, so we cannot threat it as double quote pair open/close
  var inQuote: Boolean? = false
  val charsToSearch = setOf('\'', '"', '\n')
  var found = findPositionOfFirstCharacter(chars, begin, charsToSearch, false, Direction.FORWARDS)
  while (found != null && found.first != '\n') {
    val i = found.second

    val c = found.first
    when (c) {
      '"' -> {
        // if [maybe] in quote, then we know we found closing quote, so now we surely are not in quote
        if (inQuote == null || inQuote) {
          // we just found closing double quote
          inQuote = false
          // reset last found single quote, as it was in string literal
          lastOpenSingleQuotePos = -1
          // if we previously found unclosed single quote
        } else if (lastOpenSingleQuotePos >= 0) {
          // ...but we are too far from it
          if (i - lastOpenSingleQuotePos > 2) {
            // then it definitely was not opening single quote
            lastOpenSingleQuotePos = -1
            // and we found opening double quote
            inQuote = true
          } else {
            // else we don't know if we inside double or single quotes or not
            inQuote = null
          }
          // we were not in double nor in single quote, so now we are in double quote
        } else {
          inQuote = true
        }
      }
      '\'' -> {
        // if we previously found unclosed single quote
        if (lastOpenSingleQuotePos >= 0) {
          // ...but we are too far from it
          if (i - lastOpenSingleQuotePos > 3) {
            // ... forget about it and threat current one as unclosed
            lastOpenSingleQuotePos = i
          } else {
            // else we found closing single quote
            lastOpenSingleQuotePos = -1
            // and if we didn't know whether we are in double quote or not
            if (inQuote == null) {
              // then now we are definitely not in
              inQuote = false
            }
          }
        } else {
          // we found opening single quote
          lastOpenSingleQuotePos = i
        }
      }
    }
    yield(State(i, c, inQuote, lastOpenSingleQuotePos))
    found = findPositionOfFirstCharacter(chars, i + Direction.FORWARDS.toInt(), charsToSearch, false, Direction.FORWARDS)
  }
}

/**
 * Check ignorecase and smartcase options to see if a case insensitive search should be performed with the given pattern.
 *
 * When ignorecase is not set, this will always return false - perform a case sensitive search.
 *
 * Otherwise, check smartcase. When set, the search will be case insensitive if the pattern contains only lowercase
 * characters, and case sensitive (returns false) if the pattern contains any lowercase characters.
 *
 * The smartcase option can be ignored, e.g. when searching for the whole word under the cursor. This always performs a
 * case insensitive search, so `\<Work\>` will match `Work` and `work`. But when choosing the same pattern from search
 * history, the smartcase option is applied, and `\<Work\>` will only match `Work`.
 */
fun shouldIgnoreCase(pattern: String, ignoreSmartCaseOption: Boolean): Boolean {
  val sc = smartcase.isSet && !ignoreSmartCaseOption
  return ignorecase.isSet && !(sc && StringHelper.containsUpperCase(pattern))
}
