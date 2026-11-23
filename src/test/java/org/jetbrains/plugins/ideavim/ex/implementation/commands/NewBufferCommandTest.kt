/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.EditNewFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NewCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NextFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.VnewCommand
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimSplitWindowTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "IdeaVim opens a scratch file as the unnamed buffer, and :new/:vnew need more than one window",
)
class NewBufferCommandTest : VimSplitWindowTestCase() {

  @Test
  fun `test enew parsing`() {
    assertIs<EditNewFileCommand>(injector.vimscriptParser.parseCommand("ene"))
    assertIs<EditNewFileCommand>(injector.vimscriptParser.parseCommand("enew"))
    assertIs<EditNewFileCommand>(injector.vimscriptParser.parseCommand("enew!"))
  }

  @Test
  fun `test new and vnew parsing`() {
    assertIs<NewCommand>(injector.vimscriptParser.parseCommand("new"))
    assertIs<VnewCommand>(injector.vimscriptParser.parseCommand("vne"))
    assertIs<VnewCommand>(injector.vimscriptParser.parseCommand("vnew"))
    assertIs<NextFileCommand>(injector.vimscriptParser.parseCommand("ne"))
  }

  @Test
  fun `test enew opens an empty scratch file in the current window`() {
    configureByText("lorem ipsum")
    val original = currentFile()

    enterCommand("enew")

    val file = currentFile()
    assertNotEquals(original, file)
    assertTrue(ScratchUtil.isScratch(file))
    assertEquals("", currentText())
    assertEquals(1, fileEditorManager.windows.size)
  }

  @Test
  fun `test enew opens a different scratch file each time`() {
    configureByText("lorem ipsum")

    enterCommand("enew")
    val first = currentFile()
    enterCommand("enew")

    assertNotEquals(first, currentFile())
  }

  @Test
  fun `test vnew splits the window with an empty scratch file`() {
    configureByText("lorem ipsum")
    val original = currentFile()

    enterCommand("vnew")

    assertEquals(2, fileEditorManager.windows.size)
    val file = currentFile()
    assertNotEquals(original, file)
    assertTrue(ScratchUtil.isScratch(file))
  }

  @Test
  fun `test new splits the window with an empty scratch file`() {
    configureByText("lorem ipsum")

    enterCommand("new")

    assertEquals(2, fileEditorManager.windows.size)
    assertTrue(ScratchUtil.isScratch(currentFile()))
  }

  private fun currentText(): String {
    var text: String? = null
    ApplicationManager.getApplication().invokeAndWait {
      text = fileEditorManager.selectedTextEditor!!.document.text
    }
    return text!!
  }

  private fun currentFile(): VirtualFile {
    var file: VirtualFile? = null
    ApplicationManager.getApplication().invokeAndWait {
      file = fileEditorManager.currentWindow!!.selectedFile
    }
    return file!!
  }
}
