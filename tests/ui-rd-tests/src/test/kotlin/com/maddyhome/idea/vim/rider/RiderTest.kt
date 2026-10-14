/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RiderTest : IdeaVimRiderTestBase() {

  // The reporter's mapping. Configured up front, typing it in every test is slow
  override fun ideaVimRcContent(): String = "vnoremap v :action EditorSelectWord<CR>"

  @AfterEach
  fun resetMode() {
    esc()
  }

  @Test
  fun `enter in normal mode moves to the next line`() {
    setText(PROGRAM_TEXT, caretAt = " for more information")

    enter()

    assertText(PROGRAM_TEXT)
    assertEquals(PROGRAM_TEXT.indexOf("\n") + 1, caretOffset())
  }

  @Test
  fun `enter in insert mode inserts a line break`() {
    setText(PROGRAM_TEXT, caretAt = " for more information")

    typeVim("A")
    enter()

    assertText(
      """
      |// See https://aka.ms/new-console-template for more information
      |
      |
      |Console.WriteLine("Hello, World!");
      """.trimMargin()
    )
  }

  // Freezes Rider: the EDT parks in ActionManagerImpl.tryToExecute (runBlockingForActionExpand) when the mapping runs
  // ReformatCode, and every later test fails with it
  @Disabled("Freezes Rider when <Action>(ReformatCode) runs from the mapping")
  @Test
  fun `mapping with space runs a Rider action`() {
    setText(PROGRAM_TEXT, caretAt = "Console")
    typeVim(">>")
    assertText(PROGRAM_TEXT.replace("Console", "    Console"))

    exCommand("map <Space>x <Action>(ReformatCode)")
    typeVim(" x")

    assertText(PROGRAM_TEXT)
    exCommand("unmap <Space>x")
  }

  // Control for the test below: starting on any other letter, the selection grows past `foo`
  @Test
  fun `extend selection from the first letter of a word before a dot`() {
    setText(SELECT_WORD_TEXT, caretAt = "foo.Length")

    assertSelectionExtendsPastFoo()
  }

  // Reported: with `vnoremap v :action EditorSelectWord<CR>` and the caret on the last `o` of `foo` in `foo.Length`,
  // the selection doesn't expand further than `foo`. Rider implements EditorSelectWord on its backend
  @Test
  fun `extend selection from the last letter of a word before a dot`() {
    setText(SELECT_WORD_TEXT, caretAt = "o.Length")

    assertSelectionExtendsPastFoo()
  }

  // Rider's EditorSelectWord leaves the caret after the selection. In Visual mode the caret has to be on a selected
  // character, the first or the last one, so IdeaVim moves it back (see RiderActionListener)
  @Test
  fun `extend selection leaves the caret on a selected character`() {
    setText(SELECT_WORD_TEXT, caretAt = "foo.Length")

    typeVim("v")
    typeVim("v")

    assertSelection("foo")
    assertCaretOnSelectionEnd()
  }

  /** Expands the selection with the reporter's mapping: `v` enters Visual mode, then each `v` runs EditorSelectWord */
  private fun assertSelectionExtendsPastFoo() {
    typeVim("v")
    typeVim("v")
    assertSelection("foo")

    typeVim("v")
    assertSelectionContains("foo.Length")
  }

  companion object {
    private val PROGRAM_TEXT = """
      |// See https://aka.ms/new-console-template for more information
      |
      |Console.WriteLine("Hello, World!");
    """.trimMargin()

    private val SELECT_WORD_TEXT = """
      |string foo = "foo";
      |Debug.Log(foo.Length);
    """.trimMargin()
  }
}
