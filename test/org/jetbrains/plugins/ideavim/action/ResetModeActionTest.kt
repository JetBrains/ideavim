package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

class ResetModeActionTest : VimTestCase() {
  fun `test reset from normal mode`() {
    val keys = StringHelper.parseKeys("<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from insert mode`() {
    val keys = StringHelper.parseKeys("i", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from visual mode`() {
    val keys = StringHelper.parseKeys("V", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from select mode`() {
    val keys = StringHelper.parseKeys("gH", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from operator-pending mode`() {
    val keys = StringHelper.parseKeys("d", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }
}
