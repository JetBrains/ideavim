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

class MoveCommandTest : VimTestCase() {

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
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      """.trimIndent(),
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
      See, nothing.
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
      See, not${c}hing.
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
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      """.trimIndent(),
    )
  }
}
