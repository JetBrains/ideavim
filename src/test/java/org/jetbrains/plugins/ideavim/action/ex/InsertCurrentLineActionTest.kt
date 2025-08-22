/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class InsertCurrentLineActionTest : VimExTestCase() {
  override fun configureByText() {
    configureByText("""
      |  Lorem ipsum dolor sit amet,
      |  consectetur ${c}adipiscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test insert current line`() {
    typeText(":<C-R><C-L>")
    assertExText("  consectetur adipiscing elit")
  }

  @Test
  fun `test shows prompt after CTRL-R`() {
    typeText(":<C-R>")
    assertRenderedExText("\"")
  }

  @Test
  fun `test insert current line after existing text`() {
    typeText(":set <C-R><C-L>")
    assertExText("set   consectetur adipiscing elit")
  }

  @Test
  fun `test insert current line before existing text`() {
    typeText(":set <Home><C-R><C-L>")
    assertExText("  consectetur adipiscing elitset ")
  }

  @Test
  fun `test insert current line in overstrike mode replaces text`() {
    typeText(":<Ins>set<Home><C-R><C-L>")
    assertExText("  consectetur adipiscing elit")
  }

  @Test
  fun `test insert current line on blank line`() {
    typeText("jj", ":<C-R><C-L>")
    assertExText("")
  }
}
