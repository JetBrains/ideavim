/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import org.junit.jupiter.api.Test
import javax.swing.KeyStroke

@Suppress("SpellCheckingInspection")
class InsertRegisterActionTest : VimExTestCase() {
  @Test
  fun `test insert register`() {
    VimPlugin.getRegister().setKeys('c', injector.parser.parseKeys("hello world"))
    VimPlugin.getRegister().setKeys('5', injector.parser.parseKeys("greetings programs"))

    typeText(":<C-R>c")
    assertExText("hello world")

    deactivateExEntry()

    typeText(":<C-R>5")
    assertExText("greetings programs")

    deactivateExEntry()

    typeText(":set<Home><C-R>c")
    assertExText("hello worldset")
    assertExOffset(11) // Just before 'set'
  }

  @Test
  fun `test insert multi-line register`() {
    // parseKeys parses <CR> in a way that Register#getText doesn't like
    val keys = mutableListOf<KeyStroke>()
    keys.addAll(injector.parser.parseKeys("hello<CR>world"))
    VimPlugin.getRegister().setKeys('c', keys)

    typeText(":<C-R>c")
    assertExText("hello\u000Dworld")
  }

  // TODO: Test inserting special characters, such as ^H
  // TODO: Test other special registers, if/when supported
  // E.g. '.' '%' '#', etc.

  @Test
  fun `test insert last command`() {
    typeText(":set incsearch<CR>")
    typeText(":<C-R>:")
    assertExText("set incsearch")
  }

  @Test
  fun `test insert last search command`() {
    typeText("/hello<CR>")
    typeText(":<C-R>/")
    assertExText("hello")
  }

  @Test
  @TestFor(issues = ["VIM-3506"])
  fun `test render quote prompt when awaiting for register`() {
    injector.registerGroup.setKeys('w', injector.parser.parseKeys("world"))
    configureByText("")
    typeText(":hello <C-R>")
    assertRenderedExText("hello \"")
    typeText("w")
    assertRenderedExText("hello world")
  }
}
