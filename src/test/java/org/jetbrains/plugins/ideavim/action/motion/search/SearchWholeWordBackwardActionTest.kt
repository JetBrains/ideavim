/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class SearchWholeWordBackwardActionTest : VimTestCase() {
  fun `test backward search on empty string`() {
    doTest("#", "", "", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    assertPluginError(false)
  }
}
