/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.maddyhome.idea.vim.listener.BufNewFileTracker
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class UpdateCommandTest : VimTestCase() {

  @Test
  fun `update saves modified buffer`() {
    val editor = openFile("hello.txt")
    modifyDocument(editor)
    val fdm = FileDocumentManager.getInstance()
    assertTrue(fdm.isDocumentUnsaved(editor.document))

    enterCommand("update")

    assertPluginError(false)
    assertFalse(fdm.isDocumentUnsaved(editor.document))
  }

  @Test
  fun `update is noop when buffer is not modified`() {
    val editor = openFile("hello.txt")
    val fdm = FileDocumentManager.getInstance()
    assertFalse(fdm.isDocumentUnsaved(editor.document))

    enterCommand("update")

    assertPluginError(false)
    assertFalse(fdm.isDocumentUnsaved(editor.document))
  }

  @Test
  fun `update short form saves modified buffer`() {
    val editor = openFile("hello.txt")
    modifyDocument(editor)
    val fdm = FileDocumentManager.getInstance()
    assertTrue(fdm.isDocumentUnsaved(editor.document))

    enterCommand("up")

    assertPluginError(false)
    assertFalse(fdm.isDocumentUnsaved(editor.document))
  }

  private fun openFile(filename: String, content: String = "initial content"): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      val file = fixture.createFile(filename, content)
      BufNewFileTracker.consumeIfNew(file.path)
      fixture.openFileInEditor(file)
    }
    return fixture.editor
  }

  private fun modifyDocument(editor: Editor) {
    ApplicationManager.getApplication().invokeAndWait {
      WriteCommandAction.runWriteCommandAction(fixture.project) {
        editor.document.insertString(0, "x")
      }
    }
  }
}
