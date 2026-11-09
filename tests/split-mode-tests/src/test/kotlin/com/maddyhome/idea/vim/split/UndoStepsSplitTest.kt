/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Each change made on the frontend is a separate undo step (VIM-4370).
 */
class UndoStepsSplitTest : IdeaVimStarterTestBase() {

  @Test
  fun `undo two deletions one at a time`() {
    openFile(createFile("src/UndoSteps1.txt", TEXT))

    typeVim("0x")
    assertText("aa bbb\nccc ddd\n")
    typeVim("x")
    assertText("a bbb\nccc ddd\n")

    typeVim("u")
    assertText("aa bbb\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
  }

  @Test
  fun `undo insert after a deletion`() {
    openFile(createFile("src/UndoSteps2.txt", TEXT))

    typeVim("0x")
    assertText("aa bbb\nccc ddd\n")
    typeVimAndEscape("iZZ")
    assertText("ZZaa bbb\nccc ddd\n")

    typeVim("u")
    assertText("aa bbb\nccc ddd\n")

    typeVim("u")
    assertText(TEXT)
  }

  private fun assertText(expected: String) {
    var text = ""
    val found = waitUntil { text = editorText(); text == expected }
    assertTrue(found) { "Expected:\n$expected\nActual:\n$text" }
  }

  companion object {
    private const val TEXT = "aaa bbb\nccc ddd\n"
  }
}
