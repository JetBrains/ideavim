/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.editor.read

import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.api.VimSearchHelper
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import org.jetbrains.plugins.ideavim.mock.MockTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import kotlin.test.assertTrue

class ReadTest : MockTestCase() {
  private lateinit var vimScope: VimScope
  private lateinit var mockSearchHelper: VimSearchHelper
  private lateinit var vimEditor: VimEditor
  private lateinit var mockInjector: VimInjector
  private lateinit var realInjector: VimInjector

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    vimScope = VimScopeImpl(listenerOwner, mappingOwner)

    mockInjector = spy(injector)

    mockSearchHelper = mockService(VimSearchHelper::class.java)
    Mockito.`when`(mockInjector.searchHelper).thenReturn(mockSearchHelper)

    vimEditor = fixture.editor.vim

    realInjector = injector
    injector = mockInjector
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(testInfo)
    // reset mocks
    Mockito.reset(mockSearchHelper)
    Mockito.reset(mockInjector)

    injector = realInjector
  }

  fun assertEqualsEditor(expected: VimEditor, actual: VimEditor) {
    assertTrue(
      expected.getPath() == actual.getPath(),
      "Expected and actual VimEditor instances do not match in file path"
    )
  }

  @Test
  fun `test getNextParagraphBoundOffset calls findNextParagraph`() {
    val startLine = 1
    val count = 2
    val includeWhitespaceLines = true

    vimScope.editor {
      read {
        getNextParagraphBoundOffset(startLine, count, includeWhitespaceLines)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findNextParagraph(
      editorCaptor.capture(),
      eq(startLine),
      eq(count),
      eq(includeWhitespaceLines)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getNextSentenceStart calls findNextSentenceStart`() {
    val startOffset = 10
    val count = 2
    val includeCurrent = true
    val requireAll = false

    vimScope.editor {
      read {
        getNextSentenceStart(startOffset, count, includeCurrent, requireAll)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findNextSentenceStart(
      editorCaptor.capture(),
      eq(startOffset),
      eq(count),
      eq(includeCurrent),
      eq(requireAll)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getNextSectionStart calls findSection`() {
    val startLine = 5
    val marker = '{'
    val count = 3

    vimScope.editor {
      read {
        getNextSectionStart(startLine, marker, count)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findSection(
      editorCaptor.capture(),
      eq(startLine),
      eq(marker),
      eq(1),
      eq(count)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getPreviousSectionStart calls findSection`() {
    val startLine = 10
    val marker = '}'
    val count = 2

    vimScope.editor {
      read {
        getPreviousSectionStart(startLine, marker, count)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findSection(
      editorCaptor.capture(),
      eq(startLine),
      eq(marker),
      eq(-1),
      eq(count)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getNextSentenceEnd calls findNextSentenceEnd`() {
    val startOffset = 15
    val count = 1
    val includeCurrent = false
    val requireAll = true

    vimScope.editor {
      read {
        getNextSentenceEnd(startOffset, count, includeCurrent, requireAll)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findNextSentenceEnd(
      editorCaptor.capture(),
      eq(startOffset),
      eq(count),
      eq(includeCurrent),
      eq(requireAll)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getNextWordStartOffset calls findNextWord`() {
    val startOffset = 5
    val count = 3
    val isBigWord = true

    vimScope.editor {
      read {
        getNextWordStartOffset(startOffset, count, isBigWord)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findNextWord(
      editorCaptor.capture(),
      eq(startOffset),
      eq(count),
      eq(isBigWord)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getNextWordEndOffset calls findNextWordEnd`() {
    val startOffset = 7
    val count = 2
    val isBigWord = false
    val stopOnEmptyLine = true

    vimScope.editor {
      read {
        getNextWordEndOffset(startOffset, count, isBigWord)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findNextWordEnd(
      editorCaptor.capture(),
      eq(startOffset),
      eq(count),
      eq(isBigWord),
      eq(stopOnEmptyLine)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getNextCharOnLineOffset calls findNextCharacterOnLine`() {
    val startOffset = 3
    val count = 2
    val char = 'a'

    vimScope.editor {
      read {
        getNextCharOnLineOffset(startOffset, count, char)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findNextCharacterOnLine(
      editorCaptor.capture(),
      eq(startOffset),
      eq(count),
      eq(char)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getNearestWordOffset calls findWordNearestCursor`() {
    val startOffset = 12

    vimScope.editor {
      read {
        getNearestWordOffset(startOffset)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findWordNearestCursor(editorCaptor.capture(), eq(startOffset))

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getParagraphRange calls findParagraphRange`() {
    val line = 8
    val count = 1
    val isOuter = true

    vimScope.editor {
      read {
        getParagraphRange(line, count, isOuter)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findParagraphRange(
      editorCaptor.capture(),
      eq(line),
      eq(count),
      eq(isOuter)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test getBlockQuoteInLineRange calls findBlockQuoteInLineRange`() {
    val startOffset = 10
    val quote = '"'
    val isOuter = false

    vimScope.editor {
      read {
        getBlockQuoteInLineRange(startOffset, quote, isOuter)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findBlockQuoteInLineRange(
      editorCaptor.capture(),
      eq(startOffset),
      eq(quote),
      eq(isOuter)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test findAll calls findAll`() {
    val pattern = "test"
    val startLine = 1
    val endLine = 10
    val ignoreCase = true

    vimScope.editor {
      read {
        findAll(pattern, startLine, endLine, ignoreCase)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findAll(
      editorCaptor.capture(),
      eq(pattern),
      eq(startLine),
      eq(endLine),
      eq(ignoreCase)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test findPattern calls findPattern with forward search`() {
    val pattern = "search"
    val startOffset = 5
    val count = 2
    val backwards = false
    val vimSearchOptions = enumSetOf<SearchOptions>()

    vimScope.editor {
      read {
        findPattern(pattern, startOffset, count, backwards)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findPattern(
      editorCaptor.capture(),
      eq(pattern),
      eq(startOffset),
      eq(count),
      eq(vimSearchOptions)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test findPattern calls findPattern with backward search`() {
    val pattern = "search"
    val startOffset = 15
    val count = 1
    val backwards = true
    val vimSearchOptions = enumSetOf(SearchOptions.BACKWARDS)

    vimScope.editor {
      read {
        findPattern(pattern, startOffset, count, backwards)
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.searchHelper).findPattern(
      editorCaptor.capture(),
      eq(pattern),
      eq(startOffset),
      eq(count),
      eq(vimSearchOptions)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }
}