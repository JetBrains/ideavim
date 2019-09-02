package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class SearchWholeWordBackwardActionTest : VimTestCase() {
  fun `test backward search on empty string`() {
    doTest(parseKeys("#"), "", "", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertPluginError(false)
  }
}
