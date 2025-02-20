/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ResetModeActionTest : VimTestCase() {
  private val owner = MappingOwner.Plugin.get("ResetModeActionTest")

  @Test
  fun `test reset from normal mode`() {
    val keys = "<C-\\><C-N>"
    val before = "Lorem Ipsum"
    val after = "Lorem Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test reset from insert mode`() {
    val keys = listOf("i", "<C-\\><C-N>")
    val before = "Lorem Ipsum"
    val after = "Lorem Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test reset from insert mode check position`() {
    val keys = listOf("i", "<C-\\><C-N>")
    val before = "A Disc${c}overy"
    val after = "A Dis${c}covery"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test reset and execute command`() {
    val keys = listOf("i", "<C-\\><C-N>", "3l")
    val before = "${c}Lorem Ipsum"
    val after = "Lorem Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test reset from visual mode`() {
    val keys = listOf("V", "<C-\\><C-N>")
    val before = "Lorem Ipsum"
    val after = "Lorem Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test reset from select mode`() {
    val keys = listOf("gH", "<C-\\><C-N>")
    val before = "Lorem Ipsum"
    val after = "Lorem Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test reset from operator-pending mode`() {
    val keys = listOf("d", "<C-\\><C-N>")
    val before = "Lorem Ipsum"
    val after = "Lorem Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test reset from operator-pending mode with delete`() {
    val keys = "d<Esc>dw"
    val before = "Lorem Ipsum"
    val after = "Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test delete command after resetting operator-pending mode`() {
    val keys = listOf("d", "<C-\\><C-N>", "dw")
    val before = "Lorem Ipsum"
    val after = "Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test delete command after resetting operator-pending mode with esc`() {
    val keys = listOf("d", "<Esc>", "dw")
    val before = "Lorem Ipsum"
    val after = "Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  @Test
  fun `test delete command after resetting operator-pending mode with ctrl open bracket`() {
    val keys = listOf("d", "<C-[>", "dw")
    val before = "Lorem Ipsum"
    val after = "Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test delete command after resetting operator-pending mode with mapping`() {
    VimPlugin.getKey()
      .putKeyMapping(
        MappingMode.NVO,
        injector.parser.parseKeys("<C-D>"),
        owner,
        injector.parser.parseKeys("<Esc>"),
        false,
      )

    val keys = listOf("d", "<C-D>", "dw")
    val before = "Lorem Ipsum"
    val after = "Ipsum"
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test non-delete commands after resetting operator-pending mode`() {
    val keys = listOf("c", "<C-\\><C-N>", "another")
    val before = "Lorem Ipsum"
    val after = "Lnotherorem Ipsum"
    doTest(keys, before, after, Mode.INSERT)
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }

  @Test
  fun `test delete after escaping t`() {
    val keys = "dt<esc>D"
    val before = "A ${c}Discovery"
    val after = "A "
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(fixture.editor.selectionModel.hasSelection())
    }
  }
}
