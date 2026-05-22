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
  fun `should undo after insert text`() {
    openFile(createFile("src/Repeat.txt", "test"))
    typeVimAndEscape("0ddiHi ")
    assertEditorContains("Hi ", "Should have inserted 'Hi '")

    typeVim("u")
    var text = ""
    val found = waitUntil { text = editorText(); text.isEmpty() }
    assertTrue(found) {
      "Undo should revert insert. Actual: $text"
    }
  }

  @Test
  fun `substitute repeat with dot then undo`() {
    openFile(createFile("src/Repeat1.txt", "abcdef\nghijkl\n"))

    typeVimAndEscape("0sX")
    assertEditorContains("Xbcdef", "First char should be X")

    typeVim("l")
    pause()
    typeVim(".")
    assertEditorContains("XXcdef", "Second char should also be X")

    typeVim("u")
    assertEditorContains("Xbcdef", "Single undo should revert dot-repeat only (the dot is grouped)")
  }

  @Test
  fun `insert repeat with dot then undo`() {
    openFile(createFile("src/Repeat2.txt", "abcdef\nghijkl\n"))

    typeVimAndEscape("0iHi ")
    assertEditorContains("Hi ", "Should have inserted 'Hi '")

    typeVim(".")
    assertEditorContains("HiHi", "Dot repeat should insert again")

    typeVim("u")
    var text = ""
    val found = waitUntil { text = editorText(); text.contains("Hi ") && !text.contains("HiHi") }
    assertTrue(found) {
      "Undo should revert dot repeat only. Actual: $text"
    }
  }

  @Test
  fun `change word repeat with dot then undo`() {
    openFile(createFile("src/Repeat3.txt", "foo bar baz\nfoo bar baz\n"))

    typeVimAndEscape("0cwHELLO")
    assertEditorContains("HELLO", "Should have changed word")

    typeVim("w")
    pause()
    typeVim(".")
    var helloCount = 0
    waitUntil { helloCount = editorText().lines().first().split("HELLO").size - 1; helloCount >= 2 }
    assertTrue(helloCount >= 2) { "Should have two HELLOs. Actual: ${editorText()}" }

    typeVim("u")
    pause()
    assertEditorContains("HELLO", "Original change should remain")
  }

  @Test
  fun `block insert across multiple lines undoes as single group`() {
    // Vim block-insert: Ctrl-V <down>×9, then `I#<Esc>` prepends `#` to all 10
    // selected lines. The vim engine replays the `#` across lines via
    // ChangeGroup.repeatInsert; on JBC that needs to be wrapped in one undo
    // mark group so a single `u` reverts the whole block insert.
    val lines = (1..15).joinToString("\n") { "line $it" }
    openFile(createFile("src/BlockInsert.txt", lines + "\n"))

    typeVim("gg")
    pause()
    ctrlV()
    typeVim("9j")
    typeVim("I#")
    esc()
    pause(1000)

    val afterInsert = editorText()
    val firstTen = afterInsert.lines().take(10)
    assertTrue(firstTen.all { it.startsWith("#") }) {
      "Expected first 10 lines to start with '#' after block insert. Actual:\n$afterInsert"
    }

    typeVim("u")
    pause(1000)

    val afterUndo = editorText()
    val firstTenAfterUndo = afterUndo.lines().take(10)
    assertTrue(firstTenAfterUndo.none { it.startsWith("#") }) {
      "Single `u` should undo the block insert across all 10 lines. Actual:\n$afterUndo"
    }
  }
}
