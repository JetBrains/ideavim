/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.spellchecker.SpellCheckerSeveritiesProvider
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.newapi.IjVimEditor
import it.unimi.dsi.fastutil.ints.IntComparator
import it.unimi.dsi.fastutil.ints.IntIterator
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import it.unimi.dsi.fastutil.ints.IntSortedSet

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
internal fun shouldIgnoreCase(pattern: String, ignoreSmartCaseOption: Boolean): Boolean {
  val sc = injector.globalOptions().smartcase && !ignoreSmartCaseOption
  return injector.globalOptions().ignorecase && !(sc && containsUpperCase(pattern))
}

private fun containsUpperCase(pattern: String): Boolean {
  for (i in pattern.indices) {
    if (Character.isUpperCase(pattern[i]) && (i == 0 || pattern[i - 1] != '\\')) {
      return true
    }
  }
  return false
}

/**
 * This counts all the words in the file.
 */
fun countWords(
  vimEditor: VimEditor,
  start: Int = 0,
  end: Long = vimEditor.fileSize(),
): CountPosition {
  val offset = vimEditor.currentCaret().offset

  var count = 1
  var position = 0
  var last = -1
  var res = start
  while (true) {
    res = injector.searchHelper.findNextWord(vimEditor, res, 1, true, false)
    if (res == start || res == 0 || res > end || res == last) {
      break
    }

    count++

    if (res == offset) {
      position = count
    } else if (last < offset && res >= offset) {
      position = if (count == 2) {
        1
      } else {
        count - 1
      }
    }

    last = res
  }

  if (position == 0 && res == offset) {
    position = count
  }

  return CountPosition(count, position)
}

/**
 * Find the word under the cursor or the next word to the right of the cursor on the current line.
 *
 * @param editor The editor to find the word in
 * @param caret  The caret to find word under
 * @return The text range of the found word or null if there is no word under/after the cursor on the line
 */
fun findWordUnderCursor(editor: Editor, caret: Caret): TextRange? {
  val vimEditor = IjVimEditor(editor)
  val chars = editor.document.charsSequence
  val stop = vimEditor.getLineEndOffset(caret.logicalPosition.line, true)

  val pos = caret.offset
  // Technically the first condition is covered by the second one, but let it be
  if (chars.length == 0 || chars.length <= pos) return null

  //if (pos == chars.length() - 1) return new TextRange(chars.length() - 1, chars.length());
  var start = pos
  val types = arrayOf(
    CharacterHelper.CharacterType.KEYWORD,
    CharacterHelper.CharacterType.PUNCTUATION
  )
  for (i in 0..1) {
    start = pos
    val type = charType(vimEditor, chars[start], false)
    if (type == types[i]) {
      // Search back for start of word
      while (start > 0 && charType(vimEditor, chars[start - 1], false) == types[i]) {
        start--
      }
    } else {
      // Search forward for start of word
      while (start < stop && charType(vimEditor, chars[start], false) != types[i]) {
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
  val end = if (start < stop &&
    (start >= chars.length - 1 ||
      charType(vimEditor, chars[start + 1], false) != CharacterHelper.CharacterType.KEYWORD)
  ) {
    start + 1
  } else {
    injector.searchHelper.findNextWordEnd(vimEditor, start, 1, false, false) + 1
  }

  return TextRange(start, end)
}

fun findMisspelledWords(
  editor: Editor,
  startOffset: Int,
  endOffset: Int,
  skipCount: Int,
  offsetOrdering: IntComparator?,
): Int {
  val project = editor.project ?: return -1

  val offsets: IntSortedSet = IntRBTreeSet(offsetOrdering)
  DaemonCodeAnalyzerEx.processHighlights(
    editor.document, project, SpellCheckerSeveritiesProvider.TYPO,
    startOffset, endOffset
  ) { highlight: HighlightInfo ->
    if (highlight.severity === SpellCheckerSeveritiesProvider.TYPO) {
      val offset = highlight.getStartOffset()
      if (offset >= startOffset && offset <= endOffset) {
        offsets.add(offset)
      }
    }
    true
  }

  if (offsets.isEmpty()) {
    return -1
  }

  if (skipCount >= offsets.size) {
    return offsets.lastInt()
  } else {
    val offsetIterator: IntIterator = offsets.iterator()
    skip(offsetIterator, skipCount)
    return offsetIterator.nextInt()
  }
}

private fun skip(iterator: IntIterator, n: Int) {
  require(n >= 0) { "Argument must be nonnegative: $n" }
  var i = n
  while (i-- != 0 && iterator.hasNext()) iterator.nextInt()
}

class CountPosition(val count: Int, val position: Int)

private val logger = logger<SearchLogger>()

private class SearchLogger