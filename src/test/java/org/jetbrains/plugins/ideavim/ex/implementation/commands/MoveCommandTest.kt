/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase

class MoveCommandTest : VimTestCase() {

  fun `test selection marks after moving line up`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent()
    )

    typeText("vb:m '>+1<CR>gv")
    assertState(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      For example: ${s}${c}homework$se, homework, homework, homework, homework, homework, homework, homework, homework.
      
      """.trimIndent()
    )
  }

  fun `test selection marks after moving line down`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent()
    )

    typeText("vb:m '<-2<CR>gv")
    assertState(
      """
      ====
      For example: ${s}${c}homework$se, homework, homework, homework, homework, homework, homework, homework, homework.
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      
      """.trimIndent()
    )
  }

  fun `test marks after moving line up`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent()
    )

    typeText("ma$:m +1<CR>`a")
    assertState(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      
      """.trimIndent()
    )
  }

  fun `test marks after moving line down`() {
    configureByText(
      """
      ====
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      See, nothing.
      
      """.trimIndent()
    )

    typeText("ma$:m -2<CR>`a")
    assertState(
      """
      ====
      For example: homewor${c}k, homework, homework, homework, homework, homework, homework, homework, homework.
      My mother taught me this trick: if you repeat something over and over again it loses its meaning.
      See, nothing.
      
      """.trimIndent()
    )
  }
}
