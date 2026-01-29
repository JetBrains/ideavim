/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.services

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class VimVariableServiceTest : VimTestCase() {
  @Test
  fun `test v count variable without count specified`() {
    configureByText("\n")
    enterCommand("""nnoremap <expr> n ':echo ' .. v:count .. "\<CR>"""")
    typeText("n")
    assertOutput("0")
  }

  @Test
  fun `test v count variable`() {
    configureByText("\n")
    enterCommand("""nnoremap <expr> n ':' .. "\<C-u>" .. 'echo ' .. v:count .. "\<CR>"""")
    typeText("5n")
    assertOutput("5")
  }

  @Test
  fun `test v count variable with additional count during select register`() {
    configureByText("\n")
    enterCommand("""nnoremap <expr> n ':' .. "\<C-u>" .. 'echo ' .. v:count .. "\<CR>"""")
    typeText("2\"a5n")
    assertOutput("10")
  }

  @Test
  fun `test v count variable with additional pathological count during select register`() {
    configureByText("\n")
    enterCommand("""nnoremap <expr> n ':' .. "\<C-u>" .. 'echo ' .. v:count .. "\<CR>"""")
    typeText("2\"a3\"b4\"c5n")
    assertOutput("120")
  }

  @Test
  fun `test v count1 variable without count specified`() {
    configureByText("\n")
    enterCommand("""nnoremap <expr> n ':echo ' .. v:count1 .. "\<CR>"""")
    typeText("n")
    assertOutput("1")
  }

  @Test
  fun `test v count1 variable`() {
    configureByText("\n")
    enterCommand("""nnoremap <expr> n ':' .. "\<C-u>" .. 'echo ' .. v:count1 .. "\<CR>"""")
    typeText("5n")
    assertOutput("5")
  }

  @Test
  fun `test v count1 variable with additional count during select register`() {
    configureByText("\n")
    enterCommand("""nnoremap <expr> n ':' .. "\<C-u>" .. 'echo ' .. v:count1 .. "\<CR>"""")
    typeText("2\"a5n")
    assertOutput("10")
  }

  @Test
  fun `test v count1 variable with additional pathological count during select register`() {
    configureByText("\n")
    enterCommand("""nnoremap <expr> n ':' .. "\<C-u>" .. 'echo ' .. v:count1 .. "\<CR>"""")
    typeText("2\"a3\"b4\"c5n")
    assertOutput("120")
  }

  @Test
  fun `test mapping with updating jumplist`() {
    configureByText("${c}1\n2\n3\n4\n5\n6\n7\n8\n9\n")
    enterCommand(
      """
      nnoremap <expr> j (v:count > 2 ? "m'" . v:count : "") . 'j'
      """.trimIndent(),
    )
    typeText("3j" + "3j" + "<C-o>")
    assertState("1\n2\n3\n${c}4\n5\n6\n7\n8\n9\n")
  }

  @Test
  fun `test v searchforward with forwards search`() {
    configureByText("1\n2\n1\n2\n${c}1\n2\n1\n2\n1\n")
    enterCommand(
      """
      nnoremap <expr> n 'Nn'[v:searchforward]
      """.trimIndent(),
    )
    typeText("/1<CR>" + "n")
    assertState("1\n2\n1\n2\n1\n2\n1\n2\n${c}1\n")
  }

  @Test
  fun `test v searchforward with backwards search`() {
    configureByText("1\n2\n1\n2\n1\n2\n1\n2\n${c}1\n")
    enterCommand(
      """
      nnoremap <expr> n 'Nn'[v:searchforward]
      """.trimIndent(),
    )
    typeText("?1<CR>")
    assertState("1\n2\n1\n2\n1\n2\n${c}1\n2\n1\n")
    typeText("n")
    assertState("1\n2\n1\n2\n1\n2\n1\n2\n${c}1\n")
  }

}
