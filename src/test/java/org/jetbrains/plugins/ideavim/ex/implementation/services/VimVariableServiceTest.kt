/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.services

import org.jetbrains.plugins.ideavim.VimTestCase

class VimVariableServiceTest : VimTestCase() {
  fun `test v count variable without count specified`() {
    configureByText("\n")
    enterCommand("nnoremap <expr> n ':echo ' .. v:count .. \"\\<CR>\"")
    typeText("n")
    assertExOutput("0\n")
  }

  fun `test v count variable`() {
    configureByText("\n")
    enterCommand("nnoremap <expr> n ':' .. \"\\<C-u>\" .. 'echo ' .. v:count .. \"\\<CR>\"")
    typeText("5n")
    assertExOutput("5\n")
  }

  fun `test v count1 variable without count specified`() {
    configureByText("\n")
    enterCommand("nnoremap <expr> n ':echo ' .. v:count1 .. \"\\<CR>\"")
    typeText("n")
    assertExOutput("1\n")
  }

  fun `test v count1 variable`() {
    configureByText("\n")
    enterCommand("nnoremap <expr> n ':' .. \"\\<C-u>\" .. 'echo ' .. v:count1 .. \"\\<CR>\"")
    typeText("5n")
    assertExOutput("5\n")
  }

  fun `test mapping with updating jumplist`() {
    configureByText("${c}1\n2\n3\n4\n5\n6\n7\n8\n9\n")
    enterCommand("""
      nnoremap <expr> j (v:count > 2 ? "m'" . v:count : "") . 'j'
    """.trimIndent())
    typeText("3j" + "3j" + "<C-o>")
    assertState("1\n2\n3\n${c}4\n5\n6\n7\n8\n9\n")
  }
}