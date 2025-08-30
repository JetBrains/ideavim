/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.command

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class CommandCountTest : VimTestCase() {
  @Test
  fun `test count operator motion`() {
    configureByText("${c}1234567890")
    typeText(injector.parser.parseKeys("3dl"))
    assertState("4567890")
  }

  @Test
  fun `test operator count motion`() {
    configureByText("${c}1234567890")
    typeText(injector.parser.parseKeys("d3l"))
    assertState("4567890")
  }

  @Test
  fun `test count operator count motion`() {
    configureByText("${c}1234567890")
    typeText(injector.parser.parseKeys("2d3l"))
    assertState("7890")
  }

  // See https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/normal.c#L631
  @Test
  fun `test count resets to 999999999L if gets too large`() {
    configureByText("1")
    typeText(injector.parser.parseKeys("12345678901234567890<C-A>"))
    assertState("1000000000")
  }

  @Test
  fun `test count select register count operator count motion`() {
    configureByText("${c}123456789012345678901234567890")
    typeText(injector.parser.parseKeys("2\"a3d4l")) // Delete 24 characters
    assertState("567890")
  }

  @TestWithoutNeovim(SkipNeovimReason.TABS)
  @Test
  fun `test multiple select register counts`() {
    configureByText("${c}12345678901234567890123456789012345678901234567890")
    typeText(injector.parser.parseKeys("2\"a2\"b2\"b2d2l")) // Delete 32 characters
    assertState("345678901234567890")
  }

  @TestFor(issues = ["VIM-3960"])
  @Test
  fun `test count not accepted in multicharacter text object`() {
    doTest(
      "di3w",
      "Lorem ipsum do${c}lor sit amet",
      "Lorem ipsum dolor ${c}sit amet"
    )
  }
}
