/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.autocmd

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.maddyhome.idea.vim.listener.BufNewFileTracker
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class BufWriteAutoCmdTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    ApplicationManager.getApplication().invokeAndWait {
      configureByText("\n")
    }
    enterCommand("autocmd!")
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    try {
      enterCommand("autocmd!")
    } finally {
      super.tearDown(testInfo)
    }
  }

  @Test
  fun `should fire BufWritePre on save`() {
    enterCommand("autocmd BufWritePre * echo \"pre\"")
    modifyAndSave(openFile("hello.txt"))
    assertExOutput("pre")
  }

  @Test
  fun `should fire BufWritePost on save`() {
    enterCommand("autocmd BufWritePost * echo \"post\"")
    modifyAndSave(openFile("hello.txt"))
    assertExOutput("post")
  }

  @Test
  fun `BufWrite should be alias for BufWritePre`() {
    enterCommand("autocmd BufWrite * echo \"write\"")
    modifyAndSave(openFile("hello.txt"))
    assertExOutput("write")
  }

  @Test
  fun `should fire BufWritePre before BufWritePost`() {
    enterCommand("autocmd BufWritePre * echo \"1-pre\"")
    enterCommand("autocmd BufWritePost * echo \"2-post\"")
    modifyAndSave(openFile("hello.txt"))
    assertExOutput("1-pre\n2-post")
  }

  @Test
  fun `should not fire for non-matching pattern`() {
    enterCommand("autocmd BufWritePre *.py echo \"py\"")
    modifyAndSave(openFile("hello.txt"))
    assertNoExOutput()
  }

  @Test
  fun `should match pattern against file extension`() {
    enterCommand("autocmd BufWritePre *.txt echo \"txt\"")
    modifyAndSave(openFile("hello.txt"))
    assertExOutput("txt")
  }

  private fun openFile(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      val file = fixture.createFile(filename, "initial content")
      // Clear newly-created marker so this isn't treated as BufNewFile.
      BufNewFileTracker.consumeIfNew(file.path)
      fixture.openFileInEditor(file)
    }
    return fixture.editor
  }

  private fun modifyAndSave(editor: Editor) {
    ApplicationManager.getApplication().invokeAndWait {
      WriteCommandAction.runWriteCommandAction(fixture.project) {
        editor.document.insertString(0, "x")
      }
      FileDocumentManager.getInstance().saveDocument(editor.document)
    }
  }
}
