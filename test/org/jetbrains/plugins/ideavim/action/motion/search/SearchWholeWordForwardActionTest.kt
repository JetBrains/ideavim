package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SearchWholeWordForwardActionTest : VimTestCase() {
  fun `test with tabs`() {
    val before = dotToTab("""
      .he${c}llo 1
      .hello 2
      .hello 3
    """.trimIndent())
    val keys = parseKeys("**")
    val after = dotToTab("""
      .hello 1
      .hello 2
      .${c}hello 3
    """.trimIndent())
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test backward search on empty string`() {
    doTest(parseKeys("*"), "", "", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertPluginError(false)
  }
}
