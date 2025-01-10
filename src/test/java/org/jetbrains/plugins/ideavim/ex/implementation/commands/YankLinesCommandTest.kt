/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.register.RegisterConstants
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Suppress("SpellCheckingInspection")
class YankLinesCommandTest : VimTestCase() {
  @Test
  fun `test yank current line with default range`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("y")
    assertRegisterString(
      RegisterConstants.UNNAMED_REGISTER,
      """
        |Morbi nec luctus tortor, id venenatis lacus.
        |""".trimMargin()
    )
  }

  @Test
  fun `test yank does not move caret`() {
    doTest(
      exCommand("yank"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus ${c}tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus ${c}tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test yank with single line range`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("4y")
    assertRegisterString(
      RegisterConstants.UNNAMED_REGISTER,
      """
        |Ut id dapibus augue.
        |""".trimMargin()
    )
  }

  @Test
  fun `test yank with range`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    typeText(commandToKeys("3,4y"))
    assertRegisterString(
      RegisterConstants.UNNAMED_REGISTER,
      """
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |""".trimMargin()
    )
  }

  @Test
  fun `test yank with one char on the last line`() {
    configureByText(
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                h
      """.trimIndent(),
    )
    typeText(commandToKeys("%y"))
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val yanked = registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text
    assertEquals(
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                h
                
      """.trimIndent(),
      yanked,
    )
  }

  @Test
  fun `test yank to register`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("y a")
    assertRegisterString(
      'a',
      """
        |Morbi nec luctus tortor, id venenatis lacus.
        |""".trimMargin()
    )
  }

  @Test
  fun `test yank appends to register`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    typeText("\"ayy")
    enterCommand("y A")
    assertRegisterString(
      'a',
      """
        |Morbi nec luctus tortor, id venenatis lacus.
        |Morbi nec luctus tortor, id venenatis lacus.
        |""".trimMargin()
    )
  }

  @Test
  fun `test yank to read only register`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("y :")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: :")
  }

  @Test
  fun `test yank to invalid register`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("y (")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: (")
  }

  @Test
  fun `test yank with count`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("y 3")
    assertRegisterString(
      RegisterConstants.UNNAMED_REGISTER,
      """
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |""".trimMargin()
    )
  }

  @Test
  fun `test yank with invalid count`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("y 3,4")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: ,4")
  }

  @Test
  fun `test yank with range and count`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("2,3y 3")
    assertRegisterString(
      RegisterConstants.UNNAMED_REGISTER,
      """
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |""".trimMargin()
    )
  }

  @Test
  fun `test yank with register and count`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("y c 3")
    assertRegisterString(
      'c',
      """
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |""".trimMargin()
    )
  }

  @Test
  fun `test yank with character-wise visual selection yanks line-wise`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet ${c}tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    typeText("vjj")
    enterCommand("y")
    assertRegisterString(
      RegisterConstants.UNNAMED_REGISTER,
      """
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |""".trimMargin()
    )
  }

  @Test
  fun `test multicaret yank`() {
    configureByText(
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |${c}Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    enterCommand("y")
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val carets = fixture.editor.vim.carets()
    assertEquals(3, carets.size)
    assertEquals(
      "Morbi nec luctus tortor, id venenatis lacus.\n",
      carets[0].registerStorage.getRegister(vimEditor, context, RegisterConstants.UNNAMED_REGISTER)?.text
    )
    assertEquals(
      "Nunc sit amet tellus vel purus cursus posuere et at purus.\n",
      carets[1].registerStorage.getRegister(vimEditor, context, RegisterConstants.UNNAMED_REGISTER)?.text
    )
    assertEquals(
      "Ut id dapibus augue.\n",
      carets[2].registerStorage.getRegister(vimEditor, context, RegisterConstants.UNNAMED_REGISTER)?.text
    )
  }
}
