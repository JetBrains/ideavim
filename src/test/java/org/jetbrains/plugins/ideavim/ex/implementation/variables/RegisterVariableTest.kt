/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.variables

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterVariableTest : VimTestCase() {

  @Test
  fun `test default v register`() {
    configureByText("abcd")
    typeText(commandToKeys("echo v:register"))
    assertExOutput("\"")
  }

  @Test
  fun `test default v register in expression mapping`() {
    configureByText("abcd")
    enterCommand("""nnoremap <expr> X ':echo v:register<CR>'""")
    typeText("X")
    assertExOutput("\"")
  }

  @Test
  fun `test named v register`() {
    configureByText("abcd")
    typeText("\"w")
    typeText(commandToKeys("echo v:register"))
    assertExOutput("w")
  }

  @Test
  fun `test named v register in expression mapping`() {
    configureByText("abcd")
    enterCommand("""vnoremap <expr> y '"' . v:register . 'y'""")
    typeText("vl\"zy")
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val register = injector.registerGroup.getRegisters(vimEditor, context).first { reg -> reg.name == 'z' }
    assertEquals("ab", register.text)
  }

}