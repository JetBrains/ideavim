/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionBackspaceActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test whichwrap in the same line`() {
    injector.optionGroup.setOptionValue(OptionScope.GLOBAL, OptionConstants.whichwrap, "b")
    doTest(
      listOf("<BS>"),
      """
          Oh, hi Ma${c}rk
      """.trimIndent(),
      """
          Oh, hi M${c}ark
      """.trimIndent(),
    )
    injector.optionGroup.resetDefault(OptionScope.GLOBAL, OptionConstants.whichwrap)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test whichwrap at file start`() {
    injector.optionGroup.setOptionValue(OptionScope.GLOBAL, OptionConstants.whichwrap, "b")
    doTest(
      listOf("<BS>"),
      """
          ${c}Oh, hi Mark
      """.trimIndent(),
      """
          ${c}Oh, hi Mark
      """.trimIndent(),
    )
    injector.optionGroup.resetDefault(OptionScope.GLOBAL, OptionConstants.whichwrap)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test whichwrap to previous line`() {
    injector.optionGroup.setOptionValue(OptionScope.GLOBAL, OptionConstants.whichwrap, "b")
    doTest(
      listOf("<BS>"),
      """
          Oh, hi Mark
          ${c}You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mar${c}k
          You are my favourite customer
      """.trimIndent(),
    )
    injector.optionGroup.resetDefault(OptionScope.GLOBAL, OptionConstants.whichwrap)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test from empty line to empty line`() {
    injector.optionGroup.setOptionValue(OptionScope.GLOBAL, OptionConstants.whichwrap, "b")
    doTest(
      listOf("<BS>"),
      """
          Oh, hi Mark
          
          $c
          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mark
          $c
          
          You are my favourite customer
      """.trimIndent(),
    )
    injector.optionGroup.resetDefault(OptionScope.GLOBAL, OptionConstants.whichwrap)
  }
}
