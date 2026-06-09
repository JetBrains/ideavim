/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.action.editor.OpenControlCharsEditorAction
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.helper.CmdwinKeys
import com.maddyhome.idea.vim.listener.VimListenerManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ControlCharsEditorTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test action opens an editor`() {
    configureByText("hello world\n")

    val before = FileEditorManager.getInstance(fixture.project).openFiles.size
    ApplicationManager.getApplication().invokeAndWait {
      fixture.testAction(OpenControlCharsEditorAction())
    }
    val after = FileEditorManager.getInstance(fixture.project).openFiles.size

    assertTrue(after > before, "Expected OpenControlCharsEditorAction to open a new editor")
  }

  @Test
  fun `test pasting register with control chars inserts printable caret notation`() {
    configureByText("${c}\n")

    typeText("qa", "iX<CR>Y<Esc>", "q")

    ApplicationManager.getApplication().invokeAndWait {
      fixture.testAction(OpenControlCharsEditorAction())
    }
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(openedControlCharsBuffer())
    }

    typeText("\"ap")

    assertEquals("iX^MY^[", fixture.editor.document.text)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test yanking from control chars editor parses caret notation into keystrokes`() {
    configureByText("${c}1\n2\n3\n4")
    val originalFile: VirtualFile = fixture.file.virtualFile

    ApplicationManager.getApplication().invokeAndWait {
      fixture.testAction(OpenControlCharsEditorAction())
    }
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(openedControlCharsBuffer())
      VimListenerManager.EditorListeners.addAll()
    }

    setText(":d^M")
    typeText("\"ayy")

    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(originalFile)
    }
    typeText("@a")

    assertState("${c}2\n3\n4")
  }

  private fun openedControlCharsBuffer(): VirtualFile =
    FileEditorManager.getInstance(fixture.project).openFiles
      .first { it.getUserData(CmdwinKeys.KIND) == VirtualBufferKind.ControlCharsEditor }
}
