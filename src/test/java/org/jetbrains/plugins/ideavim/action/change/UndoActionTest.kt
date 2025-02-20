/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import com.intellij.idea.TestFor
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

class UndoActionTest : VimTestCase() {
  @Test
  fun `test simple undo`() {
    val keys = listOf("dw", "u")
    val before = """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = before
    doTest(keys, before, after, Mode.NORMAL())
    val editor = fixture.editor
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(editor.caretModel.primaryCaret.hasSelection())
    }
  }

  @Test
  @TestFor(issues = ["VIM-696"])
  fun `test undo after selection`() {
    if (!optionsIjNoEditor().oldundo) {
      val keys = listOf("dwv3eld", "u")
      val before = """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
      val after = """
                Lorem Ipsum

                ${c}ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
      doTest(keys, before, after, Mode.NORMAL())
      ApplicationManager.getApplication().runReadAction {
        kotlin.test.assertFalse(hasSelection())
      }
    }
  }

  @Test
  fun `test undo with count`() {
    val keys = listOf("dwdwdw", "2u")
    val before = """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
                Lorem Ipsum

                ${c}ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
    ApplicationManager.getApplication().runReadAction {
      kotlin.test.assertFalse(hasSelection())
    }
  }

  @Test
  @TestFor(issues = ["VIM-308"])
  fun `test cursor movements do not require additional undo`() {
    if (!optionsIjNoEditor().oldundo) {
      val keys = listOf("a1<Esc>ea2<Esc>ea3<Esc>", "uu")
      val before = """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent()
      val after = """
                Lorem Ipsum

                L1orem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent()
      doTest(keys, before, after, Mode.NORMAL())
      ApplicationManager.getApplication().runReadAction {
        kotlin.test.assertFalse(hasSelection())
      }
    }
  }

  @Test
  @TestFor(issues = ["VIM-547"])
  fun `test typed text requires one undo`() {
    assumeTrue(!injector.globalIjOptions().oldundo)
    configureByText("Lorem ipsu${c}m")
    typeText("a dolor sit amet,<CR>consectetur adipiscing elit<Esc>")
    assertState("Lorem ipsum dolor sit amet,\nconsectetur adipiscing elit")
    typeText("u")
    assertState("Lorem ipsum")
  }

  @Test
  @TestFor(issues = ["VIM-547"])
  fun `test breaking insert sequence`() {
    assumeTrue(!injector.globalIjOptions().oldundo)
    configureByText("Lorem ipsu${c}m")
    typeText("a dolor sit amet,<CR>consectetur <C-G>uadipiscing elit<Esc>")
    assertState("Lorem ipsum dolor sit amet,\nconsectetur adipiscing elit")
    typeText("u")
    assertState("Lorem ipsum dolor sit amet,\nconsectetur ")
  }

  @Test
  @TestFor(issues = ["VIM-547"])
  fun `test moving caret breaks insert sequence`() {
    assumeTrue(!injector.globalIjOptions().oldundo)
    configureByText("Lorem ipsu${c}m")
    typeText("a dolor sit amet,<CR>consectetur  <Left>adipiscing elit<Esc>")
    assertState("Lorem ipsum dolor sit amet,\nconsectetur adipiscing elit ")
    typeText("u")
    assertState("Lorem ipsum dolor sit amet,\nconsectetur  ")
  }

  @Test
  @TestFor(issues = ["VIM-547"])
  fun `test change action is included in insert sequence`() {
    configureByText("Lorem ${c}ipsum dolor sit amet")
    typeText("celorem<Esc>")
    assertState("Lorem lore${c}m dolor sit amet")
    typeText("u")
    assertState("Lorem ${c}ipsum dolor sit amet")
  }

  @Test
  @TestFor(issues = ["VIM-3552"])
  fun `test paste is not part of undo sequence`() {
    configureByText("test")
    typeText("yypoxxxu")
    configureByText("Lorem lorem${c} dolor sit amet")
    typeText("u")
    configureByText("Lorem ${c}ipsum dolor sit amet")
  }

  @Test
  @TestFor(issues = ["VIM-3671"])
  fun `test undo scrolls caret to reset scrolloff`() {
    configureByLines(200, "lorem ipsum dolor sit amet")
    enterCommand("set scrolloff=10")
    typeText("50G", "dd", "G", "u")
    assertPosition(49, 0)
    assertVisibleArea(39, 73)
  }

  private fun hasSelection(): Boolean {
    val editor = fixture.editor
    return editor.caretModel.primaryCaret.hasSelection()
  }
}
