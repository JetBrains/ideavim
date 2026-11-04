/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import com.intellij.idea.TestFor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.editorId
import com.maddyhome.idea.vim.group.change.ChangeRemoteApi
import com.maddyhome.idea.vim.group.rpc
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Undo and redo through [ChangeRemoteApi], which a frontend without speculative undo (e.g. the Rider frontend in split
 * mode) uses for `u` and `<C-R>`.
 *
 * Leaving Insert mode moves the caret back by one. The platform undo treats that caret movement as a separate undo
 * step by default, so the first undo only moved the caret and the text stayed (VIM-4370).
 *
 * Only the text is checked: the backend undo puts the caret where it was before the change, past the line end.
 */
@TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
class BackendUndoTest : VimTestCase() {
  @Test
  @TestFor(issues = ["VIM-4370"])
  fun `undo append at line end`() {
    configureByText("${c}Lorem ipsum")
    typeText("A", " dolor", "<Esc>")
    assertState("Lorem ipsum dolo${c}r")

    backendUndo()

    assertEquals("Lorem ipsum", fixture.editor.document.text)
  }

  @Test
  @TestFor(issues = ["VIM-4370"])
  fun `redo append at line end`() {
    configureByText("${c}Lorem ipsum")
    typeText("A", " dolor", "<Esc>")
    backendUndo()
    assertEquals("Lorem ipsum", fixture.editor.document.text)

    backendRedo()

    assertEquals("Lorem ipsum dolor", fixture.editor.document.text)
  }

  private fun backendUndo() {
    val editorId = fixture.editor.editorId()
    ApplicationManager.getApplication().invokeAndWait {
      rpc { ChangeRemoteApi.getInstance().undo(editorId) }
    }
  }

  private fun backendRedo() {
    val editorId = fixture.editor.editorId()
    ApplicationManager.getApplication().invokeAndWait {
      rpc { ChangeRemoteApi.getInstance().redo(editorId) }
    }
  }
}
