/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Test

class BookmarkSplitTest : IdeaVimStarterTestBase() {

  @Test
  fun `set global mark in FileA then jump back from FileB`() {
    createFile("src/MarkA1.txt", "Line 1 of A\nLine 2 of A\nLine 3 of A\n")
    createFile("src/MarkB1.txt", "Line 1 of B\nLine 2 of B\nLine 3 of B\n")

    openFile("src/MarkA1.txt")
    goToLine(2)
    typeVim("mA")

    openFile("src/MarkB1.txt")
    assertEditorContains("of B")
    typeVim("`A")

    assertEditorContains("of A", "Should have jumped back to FileA")
    assertCaretAtLine(2, "Mark A should be at line 2")
  }

  @Test
  fun `mark survives file close and reopen`() {
    createFile("src/MarkA2.txt", "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\n")
    createFile("src/MarkB2.txt", "Other content\n")

    openFile("src/MarkA2.txt")
    goToLine(3)
    typeVim("mB")

    openFile("src/MarkB2.txt")
    assertEditorContains("Other")

    openFile("src/MarkA2.txt")
    typeVim("`B")

    assertCaretAtLine(3, "Mark B should be at line 3 after reopen")
  }

  @Test
  fun `multiple global marks across files`() {
    createFile("src/MarkA3.txt", "Line 1 of A\nLine 2 of A\nLine 3 of A\n")
    createFile("src/MarkB3.txt", "Line 1 of B\nLine 2 of B\nLine 3 of B\n")

    openFile("src/MarkA3.txt")
    goToLine(2)
    typeVim("mC")

    openFile("src/MarkB3.txt")
    goToLine(1)
    typeVim("mD")

    typeVim("`C")
    assertEditorContains("of A", "Should be in FileA")
    assertCaretAtLine(2)

    typeVim("`D")
    assertEditorContains("of B", "Should be in FileB")
    assertCaretAtLine(1)
  }

  @Test
  fun `mark deletion propagated to backend`() {
    createFile("src/MarkA4.txt", "Line 1\nLine 2\nLine 3\n")
    createFile("src/MarkB4.txt", "Other content\n")

    openFile("src/MarkA4.txt")
    goToLine(3)
    typeVim("mE")

    openFile("src/MarkB4.txt")
    typeVim("`E")
    assertEditorContains("Line 1", "Should have jumped to FileA")

    exCommand("delmarks E")

    openFile("src/MarkB4.txt")
    typeVim("`E")
    assertEditorContains("Other content", "Should still be in FileB since mark deleted")
  }

  @Test
  fun `set five marks on different lines and jump to each`() {
    val lines = (1..20).joinToString("\n") { "Line $it of content" }
    createFile("src/RapidMarks.txt", lines + "\n")

    val markLines = mapOf('F' to 2, 'G' to 5, 'H' to 8, 'I' to 12, 'J' to 16)

    openFile("src/RapidMarks.txt")
    for ((mark, line) in markLines) {
      goToLine(line)
      typeVim("m$mark")
    }
    pause()

    for ((mark, expectedLine) in markLines) {
      typeVim("`$mark")
      pause(200)
      assertCaretAtLine(expectedLine, "Mark $mark should be at line $expectedLine")
    }
  }
}
