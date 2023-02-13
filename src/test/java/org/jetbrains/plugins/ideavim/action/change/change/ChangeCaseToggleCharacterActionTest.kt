/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class ChangeCaseToggleCharacterActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test whichwrap in the same line`() {
    injector.optionService.setOptionValue(OptionScope.GLOBAL, OptionConstants.whichwrap, "~")
    doTest(
      listOf("~"),
      """
          Oh, hi M${c}ark
        """.trimIndent(),
      """
          Oh, hi MA${c}rk
        """.trimIndent(),
    )
    injector.optionService.resetDefault(OptionScope.GLOBAL, OptionConstants.whichwrap)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test whichwrap at file end`() {
    injector.optionService.setOptionValue(OptionScope.GLOBAL, OptionConstants.whichwrap, "~")
    doTest(
      listOf("~"),
      """
          Oh, hi Mar${c}k
        """.trimIndent(),
      """
          Oh, hi Mar${c}K
        """.trimIndent(),
    )
    injector.optionService.resetDefault(OptionScope.GLOBAL, OptionConstants.whichwrap)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test whichwrap to next line`() {
    injector.optionService.setOptionValue(OptionScope.GLOBAL, OptionConstants.whichwrap, "~")
    doTest(
      listOf("~"),
      """
          Oh, hi Mar${c}k
          You are my favourite customer
        """.trimIndent(),
      """
          Oh, hi MarK
          ${c}You are my favourite customer
        """.trimIndent(),
    )
    injector.optionService.resetDefault(OptionScope.GLOBAL, OptionConstants.whichwrap)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test from empty line to empty line`() {
    injector.optionService.setOptionValue(OptionScope.GLOBAL, OptionConstants.whichwrap, "~")
    doTest(
      listOf("~"),
      """
          Oh, hi Mark
          ${c}

          You are my favourite customer
        """.trimIndent(),
      """
          Oh, hi Mark

          ${c}
          You are my favourite customer
        """.trimIndent(),
    )
    injector.optionService.resetDefault(OptionScope.GLOBAL, OptionConstants.whichwrap)
  }
}