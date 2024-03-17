/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionInnerBlockDoubleQuoteActionTest : VimTestCase() {
  @Test
  fun `test change outside quotes`() {
    doTest("di\"", "${c}print(\"hello\")", "print(\"$c\")", Mode.NORMAL())
  }
}
