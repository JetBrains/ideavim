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
 * Tests for the `z=` command (offer suggested replacements for the misspelled word under the cursor).
 *
 * `z=` is Vim's "spelling suggestions" command: it takes the word under the caret and asks the IDE spellchecker for
 * replacement suggestions, which are then offered to the user. Gathering and presenting suggestions is host-specific
 * (see [SpellcheckerService], backed by `SpellCheckerManager`); here we stub that boundary with a fake so we can verify
 * - fast and without the IDE's spellchecker engine - that `z=` identifies the correct word under the caret and asks
 * for suggestions for it.
 */
@Suppress("SpellCheckingInspection")
class SelectMisspelledWordSuggestionActionTest : VimTestCase() {

  private class FakeSpellcheckerService : SpellcheckerService {
    val suggestionRequests = mutableListOf<String>()

    override fun addWordToDictionary(word: String, editor: VimEditor) {
    }

    override fun selectSuggestion(word: String, editor: VimEditor, caret: VimCaret) {
      suggestionRequests.add(word)
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
  fun `test suggest replacements for word under cursor`() {
    configureByText("I have a ${c}xqwzptu here")

    typeText("z=")

    assertEquals(listOf("xqwzptu"), fakeSpellcheckerService.suggestionRequests)
  }

  @Test
  fun `test suggest replacements with caret in the middle of the word`() {
    configureByText("Some qzw${c}xytp text")

    typeText("z=")

    assertEquals(listOf("qzwxytp"), fakeSpellcheckerService.suggestionRequests, "suggestions should be requested for the whole word under the cursor, not just a fragment")
  }

  @Test
  fun `test suggest replacements with caret at the end of the word`() {
    configureByText("Some vqxzpt${c}w text")

    typeText("z=")

    assertEquals(listOf("vqxzptw"), fakeSpellcheckerService.suggestionRequests)
  }

  @Test
  fun `test suggest replacements does not change the buffer`() {
    configureByText("I have a ${c}xqwzptu here")

    typeText("z=")

    assertState("I have a ${c}xqwzptu here")
  }

  @Test
  fun `test suggestions are requested only for the word under the cursor`() {
    configureByText("I have a ${c}xqwzptu and vqxzptw here")

    typeText("z=")

    assertEquals(listOf("xqwzptu"), fakeSpellcheckerService.suggestionRequests, "suggestions should be requested only for the word under the cursor")
  }
}
