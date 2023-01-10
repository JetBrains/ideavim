/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.longrunning

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MapCommandTest : VimTestCase() {

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test double recursion`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map b wbb"))
    typeText(injector.parser.parseKeys("b"))

    TestCase.assertTrue(VimPlugin.isError())
  }
}
