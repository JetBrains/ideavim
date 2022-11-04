/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.common.editor

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.newapi.IjVimEditor
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class VimEditorTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test delete string`() {
    configureByText("01234567890")
    val vimEditor = IjVimEditor(myFixture.editor)
    WriteCommandAction.runWriteCommandAction(myFixture.project) {
      runWriteAction {
        vimEditor.deleteRange(0.offset, 5.offset)
      }
    }
    assertState("567890")
  }
}
