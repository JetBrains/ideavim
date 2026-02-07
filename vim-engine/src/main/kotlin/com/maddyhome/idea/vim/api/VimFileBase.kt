/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.selectionType
import java.lang.Long.toHexString

abstract class VimFileBase : VimFile {
  override fun displayHexInfo(editor: VimEditor) {
    val offset = editor.currentCaret().offset
    val text = editor.text()
    if (offset >= text.length) return

    val ch = text[offset]
    injector.messages.showStatusBarMessage(editor, toHexString(ch.code.toLong()))
  }

  override fun displayLocationInfo(editor: VimEditor) {
    val msg = buildString {

      val caret = editor.currentCaret()
      val offset = editor.currentCaret().offset
      val totalByteCount = editor.fileSize()
      val totalWordCount = countBigWords(editor, offset)

      if (!editor.inVisualMode) {
        val pos = caret.getBufferPosition()
        val line = pos.line + 1
        val col = pos.column + 1
        val lineEndOffset = editor.getLineEndOffset(pos.line)
        val lineEndCol = editor.offsetToBufferPosition(lineEndOffset).column

        // Note that Vim can have different screen columns to buffer columns, and displays these in the form "Col 1-3".
        // Vim uses screen columns to insert inlay text and symbols such as word wrap indicators, but has a single line
        // even when wrapped, unlike IntelliJ, which has a virtual line per screen line. Because of this, it's not clear
        // how IdeaVim could represent this, or even if it would be worthwhile to do so.
        append("Col ").append(col).append(" of ").append(lineEndCol)
        append("; Line ").append(line).append(" of ").append(editor.lineCount())
        append("; Word ").append(totalWordCount.currentWord).append(" of ").append(totalWordCount.count)
        append("; Byte ").append(offset + 1).append(" of ").append(totalByteCount)
      }
      else {

        append("Selected ")

        val selection = VimSelection.create(
          caret.vimSelectionStart,
          caret.offset,
          editor.mode.selectionType ?: CHARACTER_WISE,
          editor
        ).toVimTextRange()

        val selectedLineCount: Int
        val selectedWordCount: Int
        if (selection.isMultiple) {
          selectedLineCount = selection.size()

          var count = 0
          for (i in 0 until selection.size()) {
            val wordCount = countBigWords(editor, offset, selection.startOffsets[i], selection.endOffsets[i])
            count += wordCount.count
          }
          selectedWordCount = count

          append(selection.maxLength).append(" Cols; ")
        }
        else {
          val startPos = editor.offsetToBufferPosition(selection.startOffset)
          val endPos = editor.offsetToBufferPosition(selection.endOffsetInclusive)

          selectedLineCount = endPos.line - startPos.line + 1

          val wordCount = countBigWords(editor, offset, selection.startOffset, selection.endOffset)
          selectedWordCount = wordCount.count
        }

        append(selectedLineCount).append(" of ").append(editor.lineCount()).append(" Lines; ")
        append(selectedWordCount).append(" of ").append(totalWordCount.count).append(" Words; ")
        append(selection.selectionCount).append(" of ").append(totalByteCount).append(" Bytes")
      }
    }

    injector.messages.showStatusBarMessage(editor, msg)
  }
}

/**
 * Count the number of WORDs that intersect the given range, or exist in the whole file
 */
private fun countBigWords(
  editor: VimEditor,
  caretOffset: Int,
  start: Int = 0,
  endExclusive: Int = editor.text().length,
): WordCount {

  val chars = editor.text()
  var wordCount = 0
  var currentWord = 0

  var lastCharacterType: CharacterHelper.CharacterType? = null
  for (pos in start until endExclusive) {
    val characterType = charType(editor, chars[pos], punctuationAsLetters = true)
    if (characterType != lastCharacterType && characterType != CharacterHelper.CharacterType.WHITESPACE) {
      wordCount++
    }

    // The current word is the last counted word
    if (pos <= caretOffset) {
      currentWord = wordCount
    }

    lastCharacterType = characterType
  }

  return WordCount(wordCount, currentWord)
}

private data class WordCount(val count: Int, val currentWord: Int)
