/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.search

import com.intellij.idea.TestFor
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for Neovim/Vim 'maxsearchcount' search count display.
 *
 * When the total number of matches exceeds 'maxsearchcount', the status line shows '>'
 * instead of the exact total, e.g. `[2/>3]` instead of `[2/6]`.
 *
 * See `:help 'maxsearchcount'` and `:help search-count` in Vim/Neovim.
 */
class SearchMaxSearchCountTest : VimTestCase() {

  @Test
  @TestFor(issues = ["VIM-2544"])
  fun `should show exact count when matches are below maxsearchcount`() {
    configureByText(
      """
     ${c}word 1
     word 2
     word 3
     word 4
   """.trimIndent()
    )
    enterCommand("set maxsearchcount=10")
    typeText("/word<CR>")
    assertStatusLineText("[2/4]")
  }

  @Test
  @TestFor(issues = ["VIM-2544"])
  fun `should show capped total when matches exceed maxsearchcount`() {
    configureByText(
      """
     ${c}word 1
     word 2
     word 3
     word 4
     word 5
     word 6
   """.trimIndent()
    )
    enterCommand("set maxsearchcount=3")
    typeText("/word<CR>")
    assertStatusLineText("[2/>3]")
  }

  @Test
  @TestFor(issues = ["VIM-2544"])
  fun `should show capped total on first match when matches exceed maxsearchcount`() {
    configureByText("     ${c}xxx word word word word word")
    enterCommand("set maxsearchcount=3")
    typeText("/word<CR>")
    assertStatusLineText("[1/>3]")
  }

  @Test
  @TestFor(issues = ["VIM-2544"])
  fun `should show both capped when cursor is past maxsearchcount`() {
    configureByText(
      """
     ${c}word 1
     word 2
     word 3
     word 4
     word 5
     word 6
   """.trimIndent()
    )
    enterCommand("set maxsearchcount=3")
    typeText("/word<CR>nn")
    assertStatusLineText("[>3/>3]")
  }

  @Test
  @TestFor(issues = ["VIM-2544"])
  fun `should update capped count on n when matches exceed maxsearchcount`() {
    configureByText(
      """
     ${c}word 1
     word 2
     word 3
     word 4
     word 5
     word 6
   """.trimIndent()
    )
    enterCommand("set maxsearchcount=3")
    typeText("/word<CR>")
    assertStatusLineText("[2/>3]")
    typeText("n")
    assertStatusLineText("[3/>3]")
  }
}
