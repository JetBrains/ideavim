/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.TextRange
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import kotlin.test.fail

internal object VimRegexTestUtils {

  const val START: String = "<start>"
  const val END: String = "<end>"
  const val CARET: String = "<caret>"

  fun mockEditorFromText(text: CharSequence) : VimEditor {
    val textWithoutRangeTags = getTextWithoutRangeTags(text)

    val caretIndices = mutableListOf<Int>()
    val processedText = StringBuilder(textWithoutRangeTags)
    var currentIndex = processedText.indexOf(CARET)

    while (currentIndex != -1) {
      caretIndices.add(currentIndex)
      processedText.delete(currentIndex, currentIndex + CARET.length)
      currentIndex = processedText.indexOf(CARET, currentIndex)
    }
    return mockEditor(processedText, caretIndices)
  }

  private fun mockEditor(text: CharSequence, caretOffsets: List<Int> = emptyList()) : VimEditor {
    val lines = text.split("\n").map { it + "\n" }

    val editorMock = Mockito.mock<VimEditor>()
    mockEditorText(editorMock, text)
    mockEditorOffsetToBufferPosition(editorMock, lines)
    mockEditorCarets(editorMock, caretOffsets)

    return editorMock
  }

  private fun getTextWithoutEditorTags(text: CharSequence): CharSequence {
    val textWithoutEditorTags = StringBuilder(text)
    var currentIndex = textWithoutEditorTags.indexOf(CARET)

    while (currentIndex != -1) {
      textWithoutEditorTags.delete(currentIndex, currentIndex + CARET.length)
      currentIndex = textWithoutEditorTags.indexOf(CARET, currentIndex)
    }
    return textWithoutEditorTags
  }

  private fun mockEditorText(editor: VimEditor, text: CharSequence) {
    whenever(editor.text()).thenReturn(text)
  }

  fun getMatchRanges(text: CharSequence): List<TextRange> {
    val textWithoutEditorTags = getTextWithoutEditorTags(text)
    val matchRanges = mutableListOf<TextRange>()
    var offset = 0
    var oldOffset = 0

    var startIndex = textWithoutEditorTags.indexOf(START)
    while (startIndex != -1) {
      val endIndex = textWithoutEditorTags.indexOf(END, startIndex + START.length)
      if (endIndex != -1) {
        offset += START.length
        matchRanges.add(TextRange(startIndex - oldOffset, endIndex - offset))
        startIndex = textWithoutEditorTags.indexOf(START, endIndex + END.length)
        offset += END.length
        oldOffset = offset
      } else {
        fail("Please provide the same number of START and END tags!")
      }
    }
    return matchRanges
  }

  private fun getTextWithoutRangeTags(text: CharSequence): CharSequence {
    val newText = StringBuilder(text)
    var index = newText.indexOf(START)
    while (index != -1) {
      newText.delete(index, index + START.length)
      index = newText.indexOf(START, index)
    }

    index = newText.indexOf(END)
    while (index != -1) {
      newText.delete(index, index + END.length)
      index = newText.indexOf(END, index)
    }

    return newText
  }

  private fun mockEditorOffsetToBufferPosition(editor: VimEditor, lines: List<String>) {
    whenever(editor.offsetToBufferPosition(Mockito.anyInt())).thenAnswer { invocation ->
      val offset = invocation.arguments[0] as Int
      var lineCounter = 0
      var currentOffset = 0

      while (lineCounter < lines.size && currentOffset + lines[lineCounter].length <= offset) {
        currentOffset += lines[lineCounter].length
        lineCounter++
      }

      if (lineCounter < lines.size) {
        val column = offset - currentOffset
        BufferPosition(lineCounter, column)
      } else {
        BufferPosition(-1, -1)
      }
    }
  }

  private fun mockEditorCarets(editor: VimEditor, caretOffsets: List<Int>) {
    val trueCarets = ArrayList<VimCaret>()
    for (caret in caretOffsets) {
      val caretMock = Mockito.mock<VimCaret>()
      whenever(caretMock.offset).thenReturn(Offset(caret))
      trueCarets.add(caretMock)
    }
    whenever(editor.carets()).thenReturn(trueCarets)
    whenever(editor.currentCaret()).thenReturn(trueCarets.firstOrNull())
  }
}