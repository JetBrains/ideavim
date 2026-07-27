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
import com.maddyhome.idea.vim.api.VimEditor
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for the `zg` command (add the word under the cursor to the dictionary as a good word).
 *
 * `zg` is Vim's "good word" command: it takes the word under the caret and hands it to the IDE spellchecker so it is
 * no longer reported as misspelled.
 */
@Suppress("SpellCheckingInspection")
class AddGoodWordToDictionaryActionTest : VimTestCase() {

  private class FakeSpellcheckerService : SpellcheckerService {
    val addedWords = mutableListOf<String>()

    override fun addWordToDictionary(word: String, editor: VimEditor) {
      addedWords.add(word)
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
  fun `test add word under cursor to dictionary`() {
    configureByText("I have a ${c}xqwzptu here")

    typeText("zg")

    assertEquals(listOf("xqwzptu"), fakeSpellcheckerService.addedWords)
  }

  @Test
  fun `test add word to dictionary with caret in the middle of the word`() {
    configureByText("Some qzw${c}xytp text")

    typeText("zg")

    assertEquals(listOf("qzwxytp"), fakeSpellcheckerService.addedWords, "the whole word under the cursor should be added, not just a fragment")
  }

  @Test
  fun `test add word to dictionary with caret at the end of the word`() {
    configureByText("Some vqxzpt${c}w text")

    typeText("zg")

    assertEquals(listOf("vqxzptw"), fakeSpellcheckerService.addedWords)
  }

  @Test
  fun `test add word to dictionary does not change the buffer`() {
    configureByText("I have a ${c}xqwzptu here")

    typeText("zg")

    assertState("I have a ${c}xqwzptu here")
  }

  @Test
  fun `test only the word under the cursor is added`() {
    configureByText("I have a ${c}xqwzptu and vqxzptw here")

    typeText("zg")

    assertEquals(listOf("xqwzptu"), fakeSpellcheckerService.addedWords, "only the word under the cursor should be added")
  }
}
