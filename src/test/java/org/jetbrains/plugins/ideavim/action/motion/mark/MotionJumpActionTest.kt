/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.mark

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.api.addJump
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimSplitWindowTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MotionJumpActionTest : VimSplitWindowTestCase() {
  private val text = """
    |line 1
    |line 2
    |line 3
    |line 4
    |line 5
  """.trimMargin()

  @Test
  fun `test control-O does not stay where the caret already is`() {
    configureByText(text)
    typeText("4G")
    // The IDE records the place of the caret too, e.g. when the focus comes back from a tool window
    recordJumpAtCaret()

    typeText("<C-O>")

    assertState(text.replace("line 1", "${c}line 1"))
  }

  @Test
  fun `test control-I comes back to the place control-O left, when the IDE recorded it too`() {
    configureByText(text)
    typeText("4G")
    recordJumpAtCaret()

    typeText("<C-O>", "<C-I>")

    assertState(text.replace("line 4", "${c}line 4"))
  }

  @Test
  fun `test the place control-O left is in the jump list once`() {
    configureByText(text)
    typeText("4G")
    recordJumpAtCaret()

    typeText("<C-O>")

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |>  0     1    0 line 1
        |   1     4    0 line 4
      """.trimMargin(),
    )
  }

  @Test
  fun `test control-O to another file is not an error`() {
    val main = configureByText(text)
    enterCommand("set noideaunifyjumps")
    typeText("4G")
    recordJumpAtCaret()
    val other = openNewBufferWindow("Other.txt", "other text")
    selectWindow(other)

    typeText("<C-O>")

    var selected: VirtualFile? = null
    ApplicationManager.getApplication().invokeAndWait { selected = fileEditorManager.selectedEditor?.file }
    assertEquals(main.virtualFile, selected, "<C-O> should go to the other file")
    assertPluginError(false)
  }

  private fun recordJumpAtCaret() {
    ApplicationManager.getApplication().invokeAndWait {
      injector.jumpService.addJump(fixture.editor.vim, reset = true)
    }
  }
}
