/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.action.change.insert.InsertDeleteActionTest
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCaseBase
import org.junit.jupiter.api.Test

class InsertDeleteActionTestImpl : InsertDeleteActionTest, VimTestCaseBase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  override fun `test insert delete`() {
    super.`test insert delete`()
  }
}