/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.vimscript.model.expressions.Register
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate

class ExpressionTest : VimTestCase() {

  fun `test multiline register content`() {
    configureByText("${c}Oh\nHi\nMark\n")
    typeText(parseKeys("VGy"))
    assertEquals("Oh\nHi\nMark\n", Register('"').evaluate().toString())
  }
}
