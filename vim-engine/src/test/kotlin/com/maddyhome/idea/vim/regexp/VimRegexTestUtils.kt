/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.LocalMarkStorage
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.mark.VimMark
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import kotlin.test.assertNotEquals
import kotlin.test.fail

internal object VimRegexTestUtils {

  const val START: String = "<start>"
  const val END: String = "<end>"
  const val CARET: String = "<caret>"
  const val VISUAL_START = "<vstart>"
  const val VISUAL_END = "<vend>"
  private const val MARK = "<mark.>"
  fun MARK(mark: Char): CharSequence {
    return "<mark$mark>"
  }

  fun mockEditorFromText(text: CharSequence): VimEditor {
    val cleanText = getTextWithoutEditorTags(getTextWithoutRangeTags(text))
    val lines = cleanText.split("\n").map { it + "\n" }

    val editorMock = Mockito.mock<VimEditor>()
    mockEditorText(editorMock, cleanText)
    mockEditorOffsetToBufferPosition(editorMock, lines)
    mockEditorBufferPositionToOffset(editorMock, lines)
    mockEditorLineStartOffset(editorMock)
    mockEditorLineEndOffset(editorMock, lines)
    mockEditorLineCount(editorMock, lines)

    val textWithoutRangeTags = getTextWithoutRangeTags(text)

    val carets = mutableListOf<VimCaret>()
    val textWithOnlyCarets = getTextWithoutVisualTags(getTextWithoutMarkTags(textWithoutRangeTags))
    val textWithOnlyVisuals = getTextWithoutCaretTags(getTextWithoutMarkTags(textWithoutRangeTags))
    val textWithOnlyMarks = getTextWithoutCaretTags(getTextWithoutVisualTags(textWithoutRangeTags))

    val visualStart = textWithOnlyVisuals.indexOf(VISUAL_START)
    val visualEnd = if (visualStart >= 0) textWithOnlyVisuals.indexOf(VISUAL_END) - VISUAL_START.length
    else -1

    val marks = mutableMapOf<Char, BufferPosition>()

    var nextMark = MARK.toRegex().find(textWithOnlyMarks)
    var offset = 0
    while (nextMark != null) {
      val nextMarkIndex = nextMark.range.first - offset
      offset += MARK.length
      marks[nextMark.value[5]] = editorMock.offsetToBufferPosition(nextMarkIndex)
      nextMark = nextMark.next()
    }

    var nextCaretIndex = textWithOnlyCarets.indexOf(CARET)
    offset = 0

    while (nextCaretIndex != -1) {
      carets.add(mockCaret(nextCaretIndex - offset, Pair(visualStart, visualEnd), marks))
      nextCaretIndex = textWithOnlyCarets.indexOf(CARET, nextCaretIndex + CARET.length)
      offset += CARET.length
    }

    if (carets.isEmpty()) {
      // if no carets are provided, place one at the start of the text
      val caret = mockCaret(0, Pair(visualStart, visualEnd), marks)
      whenever(editorMock.carets()).thenReturn(listOf(caret))
      whenever(editorMock.currentCaret()).thenReturn(caret)
    } else {
      whenever(editorMock.carets()).thenReturn(carets)
      whenever(editorMock.currentCaret()).thenReturn(carets.first())
    }

    return editorMock
  }

  fun mockEditor(text: CharSequence, carets: List<VimCaret>): VimEditor {
    assertNotEquals(0, carets.size, "Expected at least one caret.")
    val cleanText = getTextWithoutEditorTags(getTextWithoutRangeTags(text))
    val lines = cleanText.split("\n").map { it + "\n" }

    val editorMock = Mockito.mock<VimEditor>()
    mockEditorText(editorMock, cleanText)
    mockEditorOffsetToBufferPosition(editorMock, lines)
    mockEditorBufferPositionToOffset(editorMock, lines)
    mockEditorLineStartOffset(editorMock)
    mockEditorLineEndOffset(editorMock, lines)
    mockEditorLineCount(editorMock, lines)
    whenever(editorMock.carets()).thenReturn(carets)
    whenever(editorMock.currentCaret()).thenReturn(carets.first())

    return editorMock
  }

  fun mockCaret(
    caretOffset: Int,
    visualOffset: Pair<Int, Int> = Pair(-1, -1),
    marks: Map<Char, BufferPosition> = emptyMap(),
  ): VimCaret {
    val caretMock = Mockito.mock<VimCaret>()
    whenever(caretMock.offset).thenReturn(caretOffset)
    whenever(caretMock.selectionStart).thenReturn(visualOffset.first)
    whenever(caretMock.selectionEnd).thenReturn(visualOffset.second)
    val markStorage = mockMarkStorage(marks)
    whenever(caretMock.markStorage).thenReturn(markStorage)

    return caretMock
  }

  private fun mockMarkStorage(marks: Map<Char, BufferPosition>): LocalMarkStorage {
    val markStorage = Mockito.mock<LocalMarkStorage>()
    whenever(markStorage.getMark(Mockito.anyChar())).thenAnswer { invocation ->
      val key = invocation.arguments[0] as Char
      val position = marks[key] ?: return@thenAnswer null
      VimMark(key, position.line, position.column, "", "")
    }
    return markStorage
  }

  private fun getTextWithoutCaretTags(text: CharSequence): CharSequence {
    return text.replace(CARET.toRegex(), "")
  }

  private fun getTextWithoutVisualTags(text: CharSequence): CharSequence {
    return text.replace("$VISUAL_START|$VISUAL_END".toRegex(), "")
  }

  private fun getTextWithoutMarkTags(text: CharSequence): CharSequence {
    return text.replace(MARK.toRegex(), "")
  }

  private fun getTextWithoutEditorTags(text: CharSequence): CharSequence {
    return getTextWithoutMarkTags(
      getTextWithoutVisualTags(
        getTextWithoutCaretTags(
          text
        )
      )
    )
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
    whenever(editor.offsetToBufferPosition(anyInt())).thenAnswer { invocation ->
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

  private fun mockEditorBufferPositionToOffset(editor: VimEditor, lines: List<String>) {
    whenever(editor.bufferPositionToOffset(any(BufferPosition::class.java))).thenAnswer { invocation ->
      val position = invocation.arguments[0] as BufferPosition
      return@thenAnswer lines.subList(0, position.line).sumOf { it.length } + position.column
    }
  }

  private fun mockEditorLineStartOffset(editor: VimEditor) {
    whenever(editor.getLineStartOffset(anyInt())).thenAnswer { invocation ->
      val line = invocation.arguments[0] as Int
      editor.bufferPositionToOffset(BufferPosition(line, 0))
    }
  }

  private fun mockEditorLineEndOffset(editor: VimEditor, lines: List<String>) {
    whenever(editor.getLineEndOffset(anyInt())).thenAnswer { invocation ->
      val line = invocation.arguments[0] as Int
      editor.bufferPositionToOffset(BufferPosition(line, lines[line].length))
    }
  }

  private fun mockEditorLineCount(editor: VimEditor, lines: List<String>) {
    whenever(editor.lineCount()).thenReturn(lines.size)
  }

  private fun <T> any(type: Class<T>): T = Mockito.any(type)
}