/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FileOpsSplitTest : IdeaVimStarterTestBase() {

  @Test
  fun `save file with write command`() {
    openFile(createFile("src/Save1.txt", "Line 1\nLine 2\nLine 3\n"))
    clickEditor()
    typeVimAndEscape("0iSAVED ")
    pause()
    exCommand("w")
    pause()
    assertEditorContains("SAVED", "Text should be present after save")
  }

  @Test
  fun `file info with Ctrl-G`() {
    openFile(createFile("src/Info1.txt", "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\n"))
    goToLine(3)
    ctrlG()
    // If Ctrl-G didn't crash, the RPC call to buildFileInfoMessage worked
  }

  @Test
  fun `shell echo command`() {
    openFile(createFile("src/Shell1.txt", "hello world\n"))
    exCommand("!echo split-mode-test")
    pause(1000)
    typeVim("\n") // dismiss "Press ENTER" prompt
    // No crash = ProcessRemoteApi works
  }

  @Test
  fun `yank line and paste`() {
    openFile(
      createFile(
        "src/Paste1.java", """
      public class Paste1 {
          int a = 1;
          int b = 2;
      }
    """.trimIndent() + "\n"
      )
    )

    goToLine(2)
    typeVim("yyp")
    pause()

    val lines = editorText().lines().filter { it.contains("int a = 1") }
    assertTrue(lines.size >= 2) { "Should have duplicated line. Text: ${editorText()}" }
  }

  @Test
  fun `format with motion`() {
    openFile(
      createFile(
        "src/Format1.java", """
      public class Format1 {
          int a = 1;
          int b = 2;
      }
    """.trimIndent() + "\n"
      )
    )

    goToLine(2)
    typeVim("=j")
    pause()

    assertEditorContains("int a", "File should still contain first line")
    assertEditorContains("int b", "File should still contain second line")
  }

  @Test
  fun `format entire file and undo`() {
    openFile(
      createFile(
        "src/Format2.java", """
      public class Format2 {
      int x = 1;
      int y = 2;
      int z = 3;
      }
    """.trimIndent() + "\n"
      )
    )

    val before = editorText()
    typeVim("gg=G")
    pause()

    val after = editorText()
    assertTrue(after != before) { "Formatting should change text" }

    typeVim("u")
    pause()

    assertEquals(before, editorText()) { "Undo should restore original text" }
  }
}
