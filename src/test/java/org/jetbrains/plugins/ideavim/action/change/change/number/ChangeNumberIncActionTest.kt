/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeNumberIncActionTest : VimTestCase() {
  @VimBehaviorDiffers(originalVimAfter = "11X0")
  @Test
  fun `test inc fancy number`() {
    doTest("<C-A>", "1${c}0X0", "10X1", Mode.NORMAL())
  }
}
