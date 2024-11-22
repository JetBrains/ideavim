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
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class YankLineActionTest : VimTestCase() {
  @Test
  fun `test yank to number register`() {
    val before = """
            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("\"4yy"))
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val register = VimPlugin.getRegister().getRegister(vimEditor, context, '4')!!
    kotlin.test.assertEquals("Lorem ipsum dolor sit amet,\n", register.text)
  }
}
