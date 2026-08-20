/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "The '%' register holds the name of the current buffer, which is a test fixture file in IdeaVim and " +
    "an unrelated scratch buffer in the Neovim instance the harness talks to. The values can never match.",
)
class PutFileNameRegisterTest : VimTestCase() {
  @Test
  fun `test put current file name from percent register`() {
    doTest(
      "\"%p",
      "$c",
      "MyFile.tx${c}t",
      Mode.NORMAL(),
      fileName = "MyFile.txt",
    )
  }

  @Test
  fun `test percent register follows the current file`() {
    doTest(
      "\"%p",
      "$c",
      "AnotherFile.tx${c}t",
      Mode.NORMAL(),
      fileName = "AnotherFile.txt",
    )
  }

  @Test
  fun `test put current file name before caret`() {
    doTest(
      "\"%P",
      "one ${c}two",
      "one MyFile.tx${c}ttwo",
      Mode.NORMAL(),
      fileName = "MyFile.txt",
    )
  }

  // Vim's '%' register holds the buffer name as typed, i.e. a path relative to the current directory, not just the
  // file name. IdeaVim has no current directory, so the content root is the closest equivalent - it is what Ctrl-G
  // and ':file' already report for the same file.
  @Test
  fun `test percent register holds the path relative to the content root`() {
    configureByText("")
    openFileInSubdirectory("subdir", "MyFile.txt")

    typeText("\"%p")

    assertState("subdir/MyFile.tx${c}t")
  }

  // ':registers' lists '%' in Vim, so a synthesised register has to show up in the listing as well as in a put.
  @Test
  fun `test registers command lists the percent register`() {
    configureByText("")
    openFileInSubdirectory("subdir", "MyFile.txt")

    enterCommand("registers")

    assertExOutput(
      """Type Name Content
      |  c  "%   subdir/MyFile.txt
      """.trimMargin(),
    )
  }

  private fun openFileInSubdirectory(directory: String, fileName: String) {
    ApplicationManager.getApplication().invokeAndWait {
      val file = fixture.addFileToProject("$directory/$fileName", "")
      fixture.openFileInEditor(file.virtualFile)
    }
  }
}
