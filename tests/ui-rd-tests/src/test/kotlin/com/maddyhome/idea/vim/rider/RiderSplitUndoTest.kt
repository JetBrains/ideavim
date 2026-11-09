/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.awt.event.KeyEvent

/**
 * Undo in Rider split mode, where the undo stack is on the backend (VIM-4370).
 *
 * The backend merged all the changes made on the frontend into one undo step, and after leaving Insert mode, which
 * moves the caret back by one, spent the first `u` on restoring the caret only.
 */
@EnabledOnOs(OS.LINUX)
class RiderSplitUndoTest : IdeaVimRiderTestBase() {

  override val splitMode: Boolean = true

  override fun ideaVimRcContent(): String = "nnoremap Q xx"

  @AfterEach
  fun resetMode() {
    esc()
  }

  @Test
  fun `undo append at line end`() {
    setText(TEXT)

    typeVim("ABBB")
    esc()
    assertText("aaa bbbBBB\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
  }

  @Test
  fun `undo change word`() {
    setText(TEXT)

    typeVim("cwQQ")
    esc()
    assertText("QQ bbb\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
    assertEquals(0, caretOffset())
  }

  @Test
  fun `undo insert`() {
    setText(TEXT, caretAt = "bbb")

    typeVim("iZZ")
    esc()
    assertText("aaa ZZbbb\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
    assertEquals(4, caretOffset())
  }

  @Test
  fun `undo insert after opening the command line`() {
    setText(TEXT)

    typeVim(":")
    esc()
    typeVim("iZZ")
    esc()
    assertText("ZZaaa bbb\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
    assertEquals(0, caretOffset())
  }

  @Test
  fun `undo delete character`() {
    setText(TEXT)

    typeVim("x")
    assertText("aa bbb\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
    assertEquals(0, caretOffset())
  }

  @Test
  fun `undo delete line`() {
    setText(TEXT)

    typeVim("dd")
    assertText("ccc ddd\n")

    typeVim("u")
    assertText(TEXT)
    assertEquals(0, caretOffset())
  }

  @Test
  fun `undo two deletions one at a time`() {
    setText(TEXT)

    typeVim("x")
    typeVim("x")
    assertText("a bbb\nccc ddd\n")

    typeVim("u")
    assertText("aa bbb\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
  }

  @Test
  fun `undo two inserts one at a time`() {
    setText(TEXT)

    typeVim("iZZ")
    esc()
    typeVim("wiYY")
    esc()
    assertText("ZZaaa YYbbb\nccc ddd\n")

    typeVim("u")
    assertText("ZZaaa bbb\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
  }

  // The changes of a mapping are made at once, without waiting for the backend to get the previous ones
  @Test
  fun `undo the changes of a mapping one at a time`() {
    setText(TEXT)

    typeVim("Q")
    assertText("a bbb\nccc ddd\n")
    typeVim("x")
    assertText(" bbb\nccc ddd\n")

    typeVim("u")
    assertText("a bbb\nccc ddd\n")

    typeVim("u")
    assertText("aa bbb\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
  }

  @Test
  fun `undo the changes of a macro one at a time`() {
    setText(TEXT)

    typeVim("qaxxq")
    assertText("a bbb\nccc ddd\n")
    typeVim("j0@a")
    assertText("a bbb\nc ddd\n")

    typeVim("u")
    assertText("a bbb\ncc ddd\n")

    typeVim("u")
    assertText("a bbb\nccc ddd\n")

    typeVim("u")
    assertText("aa bbb\nccc ddd\n")
  }

  @Test
  fun `redo append at line end`() {
    setText(TEXT)

    typeVim("ABBB")
    esc()
    typeVim("u")
    assertText(TEXT)

    ctrl(KeyEvent.VK_R)
    assertText("aaa bbbBBB\nccc ddd\n")
  }

  companion object {
    private const val TEXT = "aaa bbb\nccc ddd\n"
  }
}
