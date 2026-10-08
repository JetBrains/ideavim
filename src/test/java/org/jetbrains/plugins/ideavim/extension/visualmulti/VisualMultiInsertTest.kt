/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.state.mode.Mode
import org.junit.jupiter.api.Test

/**
 * `i`, `a`, `I` and `A` on the regions: Insert mode before or after each region, or at the start or end of its line
 */
class VisualMultiInsertTest : VisualMultiTestCase() {

  @Test
  fun `test i inserts before each region`() {
    configureByText("${c}qwe asd qwe")

    typeText("<C-n><C-n>" + "ix<Esc>")

    assertState("${c}xqwe asd ${c}xqwe")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test a appends after each region`() {
    configureByText("${c}qwe asd qwe")

    typeText("<C-n><C-n>" + "ax<Esc>")

    assertState("qwe${c}x asd qwe${c}x")
  }

  @Test
  fun `test I inserts at first non-blank character of each line`() {
    configureByText(
      """  ${c}qwe
      |  qwe
      """.trimMargin(),
    )

    typeText("<C-n><C-n>" + "Ix<Esc>")

    assertState(
      """  ${c}xqwe
      |  ${c}xqwe
      """.trimMargin(),
    )
  }

  @Test
  fun `test A appends at end of each line`() {
    configureByText(
      """${c}qwe asd
      |qwe asd
      """.trimMargin(),
    )

    typeText("<C-n><C-n>" + "Ax<Esc>")

    assertState(
      """qwe asd${c}x
      |qwe asd${c}x
      """.trimMargin(),
    )
  }

  @Test
  fun `test i outside session keeps text object meaning`() {
    configureByText("foo b${c}ar")

    typeText("viw")

    assertState("foo ${s}ba${c}r$se")
  }
}
