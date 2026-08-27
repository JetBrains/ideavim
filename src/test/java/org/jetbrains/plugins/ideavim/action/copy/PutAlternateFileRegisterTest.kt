/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for the '#' (alternate buffer) register, the counterpart of the '%' register covered by
 * [PutFileNameRegisterTest]. Vim's '#' holds the name of the alternate buffer, i.e. the buffer we last switched away
 * from - the same file that Ctrl-^ jumps back to and that ':buffers' marks with '#'.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "The '#' register holds the name of the alternate buffer, which is a test fixture file in IdeaVim and " +
    "an unrelated scratch buffer in the Neovim instance the harness talks to. The values can never match.",
)
class PutAlternateFileRegisterTest : VimTestCase() {
  @Test
  fun `test put alternate file name from hash register`() {
    configureByText("")
    openFile("AnotherFile.txt")
    openFile("MyFile.txt")

    typeText("\"#p")

    assertState("AnotherFile.tx${c}t")
  }

  @Test
  fun `test put alternate file name before caret`() {
    configureByText("")
    openFile("AnotherFile.txt")
    openFile("MyFile.txt", "one two")

    typeText("w\"#P")

    assertState("one AnotherFile.tx${c}ttwo")
  }

  // Vim's '#' register holds the alternate buffer name as typed, i.e. a path relative to the current directory, not
  // just the file name. IdeaVim has no current directory, so the content root is the closest equivalent - this matches
  // what the '%' register reports for the same file.
  @Test
  fun `test hash register holds the path relative to the content root`() {
    configureByText("")
    openFile("subdir/AnotherFile.txt")
    openFile("MyFile.txt")

    typeText("\"#p")

    assertState("subdir/AnotherFile.tx${c}t")
  }

  // The alternate buffer is the file we last switched away from, not the first file we ever opened.
  @Test
  fun `test hash register holds the file we last switched away from`() {
    configureByText("")
    openFile("FirstFile.txt")
    openFile("SecondFile.txt")
    openFile("MyFile.txt")

    typeText("\"#p")

    assertState("SecondFile.tx${c}t")
  }

  // Switching back and forth swaps the alternate buffer, exactly like repeated Ctrl-^ in Vim.
  @Test
  fun `test hash register swaps when returning to the alternate file`() {
    configureByText("")
    val firstFile = openFile("FirstFile.txt")
    openFile("SecondFile.txt")
    selectFile(firstFile)

    typeText("\"#p")

    assertState("SecondFile.tx${c}t")
  }

  // Vim reports "E353: Nothing in register #" when there is no alternate buffer. IdeaVim silently ignores a put from
  // an empty register, so the only observable behaviour is that the text is left alone.
  @Test
  fun `test put from hash register does nothing when there is no alternate file`() {
    configureByText("one two")

    typeText("\"#p")

    assertState("${c}one two")
  }

  // ':registers' lists '#' in Vim, so a synthesised register has to show up in the listing as well as in a put.
  // Register.KeySorter orders '%' before '#'.
  @Test
  fun `test registers command lists the hash register`() {
    configureByText("")
    openFile("subdir/AnotherFile.txt")
    openFile("MyFile.txt")

    enterCommand("registers")

    assertExOutput(
      """Type Name Content
      |  c  "%   MyFile.txt
      |  c  "#   subdir/AnotherFile.txt
      """.trimMargin(),
    )
  }

  @Test
  fun `test hash register is readable as an expression`() {
    configureByText("")
    openFile("AnotherFile.txt")
    openFile("MyFile.txt")

    enterCommand("echo @#")

    assertExOutput("AnotherFile.txt")
  }

  @Test
  fun `test hash register is readonly`() {
    configureByText("")
    openFile("AnotherFile.txt")
    openFile("MyFile.txt")

    enterCommand("let @# = 'NotAFile.txt'")

    assertPluginError(true)
    assertPluginErrorMessage("E354: Invalid register name: '#'")
  }

  private fun openFile(path: String, content: String = ""): VirtualFile {
    var virtualFile: VirtualFile? = null
    ApplicationManager.getApplication().invokeAndWait {
      val file = fixture.addFileToProject(path, content)
      virtualFile = file.virtualFile
      fixture.openFileInEditor(file.virtualFile)
    }
    return virtualFile!!
  }

  private fun selectFile(file: VirtualFile) {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(file)
    }
  }
}
