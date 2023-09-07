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
import com.maddyhome.idea.vim.regexp.nfa.NFA
import com.maddyhome.idea.vim.regexp.parser.VimRegexParser
import com.maddyhome.idea.vim.regexp.parser.VimRegexParserResult
import com.maddyhome.idea.vim.regexp.parser.visitors.PatternVisitor
import org.mockito.Mockito
import org.mockito.kotlin.whenever

internal object VimRegexTestUtils {

  const val CARET: String = "<caret>"

  fun mockEditorFromText(text: CharSequence) : VimEditor {
    val caretIndices = mutableListOf<Int>()

    val processedText = StringBuilder(text)
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

  private fun mockEditorText(editor: VimEditor, text: CharSequence) {
    whenever(editor.text()).thenReturn(text)
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

  fun buildNFA(pattern: String) : NFA? {
    val parserResult = VimRegexParser.parse(pattern)
    return when (parserResult) {
      is VimRegexParserResult.Failure -> null
      is VimRegexParserResult.Success -> PatternVisitor.visit(parserResult.tree)
    }
  }
}