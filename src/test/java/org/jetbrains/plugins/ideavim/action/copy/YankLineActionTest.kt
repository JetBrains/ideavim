/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class YankLineActionTest : VimTestCase() {
  @Test
  fun `test yank to number register`() {
    val before = """
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("\"4yy"))
    val register = VimPlugin.getRegister().getRegister('4')!!
    kotlin.test.assertEquals("I found it in a legendary land\n", register.text)
  }
}
