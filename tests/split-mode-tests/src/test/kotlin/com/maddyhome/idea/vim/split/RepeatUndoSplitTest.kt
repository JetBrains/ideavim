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

class RepeatUndoSplitTest : IdeaVimStarterTestBase() {

  @Test
  fun `substitute repeat with dot then undo`() {
    openFile(createFile("src/Repeat1.txt", "abcdef\nghijkl\n"))

    typeVimAndEscape("0sX")
    assertEditorContains("Xbcdef", "First char should be X")

    typeVim("l.")
    assertEditorContains("XXcdef", "Second char should also be X")

    typeVim("uu")
    assertEditorContains("Xbcdef", "First undo should revert dot-repeat only")
  }

  @Test
  fun `insert repeat with dot then undo`() {
    openFile(createFile("src/Repeat2.txt", "abcdef\nghijkl\n"))

    typeVimAndEscape("0iHi ")
    assertEditorContains("Hi ", "Should have inserted 'Hi '")

    typeVim(".")
    assertEditorContains("HiHi", "Dot repeat should insert again")

    typeVim("u")
    val text = editorText()
    assertTrue(text.contains("Hi ") && !text.contains("HiHi")) {
      "Undo should revert dot repeat only. Actual: $text"
    }
  }

  @Test
  fun `change word repeat with dot then undo`() {
    openFile(createFile("src/Repeat3.txt", "foo bar baz\nfoo bar baz\n"))

    typeVimAndEscape("0cwHELLO")
    assertEditorContains("HELLO", "Should have changed word")

    typeVim("w.")
    val helloCount = editorText().lines().first().split("HELLO").size - 1
    assertTrue(helloCount >= 2) { "Should have two HELLOs. Actual: ${editorText()}" }

    typeVim("u")
    pause()
    assertEditorContains("HELLO", "Original change should remain")
  }
}
