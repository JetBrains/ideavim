/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.text

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.SpellcheckerService
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for the `zw` command (remove the word under the cursor from the dictionary, marking it as spelled wrong).
 */
@Suppress("SpellCheckingInspection")
class RemoveWordFromDictionaryActionTest : VimTestCase() {

  private class FakeSpellcheckerService : SpellcheckerService {
    val removedWords = mutableListOf<String>()

    override fun addWordToDictionary(word: String, editor: VimEditor) {
    }

    override fun selectSuggestion(word: String, editor: VimEditor, caret: VimCaret) {
    }

    override fun removeWordFromDictionary(word: String, editor: VimEditor) {
      removedWords.add(word)
    }
  }

  private lateinit var fakeSpellcheckerService: FakeSpellcheckerService

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    fakeSpellcheckerService = FakeSpellcheckerService()
    ApplicationManager.getApplication()
      .replaceService(SpellcheckerService::class.java, fakeSpellcheckerService, fixture.testRootDisposable)
  }

  @Test
  fun `test remove word under cursor from dictionary`() {
    configureByText("I have a ${c}xqwzptu here")

    typeText("zw")

    assertEquals(listOf("xqwzptu"), fakeSpellcheckerService.removedWords)
  }

  @Test
  fun `test remove word from dictionary with caret in the middle of the word`() {
    configureByText("Some qzw${c}xytp text")

    typeText("zw")

    assertEquals(listOf("qzwxytp"), fakeSpellcheckerService.removedWords, "the whole word under the cursor should be removed, not just a fragment")
  }

  @Test
  fun `test remove word from dictionary with caret at the end of the word`() {
    configureByText("Some vqxzpt${c}w text")

    typeText("zw")

    assertEquals(listOf("vqxzptw"), fakeSpellcheckerService.removedWords)
  }

  @Test
  fun `test remove word from dictionary does not change the buffer`() {
    configureByText("I have a ${c}xqwzptu here")

    typeText("zw")

    assertState("I have a ${c}xqwzptu here")
  }

  @Test
  fun `test only the word under the cursor is removed`() {
    configureByText("I have a ${c}xqwzptu and vqxzptw here")

    typeText("zw")

    assertEquals(listOf("xqwzptu"), fakeSpellcheckerService.removedWords, "only the word under the cursor should be removed")
  }
}
