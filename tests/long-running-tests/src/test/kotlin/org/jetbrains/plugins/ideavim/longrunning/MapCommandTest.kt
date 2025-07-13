/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.longrunning

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.keyStroke
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MapCommandTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test double recursion`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByText(text)

    typeText(commandToKeys("map b wbb"))
    typeText(injector.parser.parseKeys("b"))

    kotlin.test.assertTrue(injector.messages.isError())
  }
}
