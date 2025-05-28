/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class MoveCommandTest : VimTestCase() {

  @Test
  fun `test move line up and undo`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent(),
    )

    enterCommand("m 0")
    assertState(
      """
      ${c}For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      
      """.trimIndent(),
    )

    typeText("u")

    assertState(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent(),
    )
  }

  @Test
  fun `test selection marks after moving line up`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent(),
    )

    typeText("vb:m '>+1<CR>gv")
    assertState(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      For example: ${s}${c}homework$se, homework, homework, homework, homework, homework, homework, homework, homework.
      
      """.trimIndent(),
    )
  }

  @Test
  fun `test selection marks after moving line down`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent(),
    )

    typeText("vb:m '<-2<CR>gv")
    assertState(
      """
      ====
      For example: ${s}${c}homework$se, homework, homework, homework, homework, homework, homework, homework, homework.
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      
      """.trimIndent(),
    )
  }

  @Test
  fun `test marks after moving line up`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent(),
    )

    typeText("ma$:m +1<CR>`a")
    assertState(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      
      """.trimIndent(),
    )
  }

  @Test
  fun `test marks after moving line down`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent(),
    )

    typeText("ma$:m -2<CR>`a")
    assertState(
      """
      ====
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      
      """.trimIndent(),
    )
  }

  @Test
  fun `test moving multiple lines with omitted selection start mark`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent(),
    )
    enterCommand("set nowrap")

    typeText("Vj:m-2<CR>")
    assertState(
      """
      ====
      For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      
      """.trimIndent(),
    )
  }

  @Test
  fun `test moving text to first line`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      """.trimIndent(),
    )
    enterCommand("m 0")
    assertState(
      """
      ${c}For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      """.trimIndent(),
    )
  }

  @Test
  fun `test move text to below current line`() {
    doTest(
      exCommand("2,4m."),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Ut id dapibus augue.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test moving multiple lines to text start`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      """.trimIndent(),
    )
    enterCommand("set nowrap")
    typeText("Vj:m-3<CR>")
    assertState(
      """
      For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
      ${c}See, nothing.
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      """.trimIndent(),
    )
  }

  @Test
  fun `test moving last line does not create empty line`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
      See, not${c}hing.
      """.trimIndent(),
    )
    enterCommand("m-2")
    assertState(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      ${c}See, nothing.
      For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
      """.trimIndent(),
    )
  }

  @Test
  fun `test moving line to file end does not create empty line`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      """.trimIndent(),
    )
    enterCommand("m+1")
    assertState(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      ${c}For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
      """.trimIndent(),
    )
  }

  // VIM-3837
  @Test
  fun `test moving relative line positions caret correctly`() {
    doTest(
      exCommand("+2m."), // Move the line 2 lines below, to below the current line
      """
        |2
        |1
        |${c}3
        |1
        |2
      """.trimMargin(),
      """
        |2
        |1
        |3
        |${c}2
        |1
      """.trimMargin()
    )
  }

  @Test
  fun `test moving relative line positions caret correctly 2`() {
    doTest(
      exCommand("+2m."), // Move the line 2 lines below, to below the current line
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet ${c}tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Ut id dapibus augue.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test moving lines positions caret correctly with nostartofline option`() {
    doTest(
      exCommand("+2m."), // Move the line 2 lines below, to below the current line
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet ${c}tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Orci varius na${c}toque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Ut id dapibus augue.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    ) {
      enterCommand("set nostartofline")
    }
  }

  @Test
  fun `test moving lines positions caret correctly with nostartofline option on shorter line`() {
    doTest(
      exCommand("+2m."), // Move the line 2 lines below, to below the current line
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id ${c}venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Ut id dapibus augue${c}.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    ) {
      enterCommand("set nostartofline")
    }
  }

  @Test
  fun `test move line up and undo with oldundo`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent(),
    )

    try {
      enterCommand("set oldundo")
      enterCommand("m 0")
      assertState(
        """
      ${c}For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      
      """.trimIndent(),
      )

      typeText("u")

      assertState(
        """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent(),
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }
}
