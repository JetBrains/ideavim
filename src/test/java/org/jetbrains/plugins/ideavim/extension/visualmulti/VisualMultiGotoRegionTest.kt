/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import org.junit.jupiter.api.Test

/**
 * Goto Next and Goto Prev (`]` and `[`): change the current region without adding any. Remove Region (`Q`) shows which
 * region is the current one.
 */
class VisualMultiGotoRegionTest : VisualMultiTestCase() {

  private val text = """q${c}we
    |asd
    |qwe
    |asd
    |qwe
  """.trimMargin()

  @Test
  fun `test bracket does not add regions`() {
    configureByText(text)

    typeText("<C-n><C-n>" + "]")

    assertState(
      """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
      """.trimMargin(),
    )
  }

  @Test
  fun `test closing bracket goes to next region and wraps around`() {
    configureByText(text)

    // The last region is the current one, so ] wraps around to the first one
    typeText("<C-n><C-n><C-n>" + "]" + "Q")

    assertState(
      """qwe
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test opening bracket goes to previous region`() {
    configureByText(text)

    typeText("<C-n><C-n><C-n>" + "[" + "Q")

    assertState(
      """${s}qwe$se
      |asd
      |qwe
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test opening bracket wraps around`() {
    configureByText(text)

    typeText("<C-n><C-n><C-n>" + "[[[" + "Q")

    assertState(
      """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
      """.trimMargin(),
    )
  }

  @Test
  fun `test count for bracket`() {
    configureByText(text)

    typeText("<C-n><C-n><C-n>" + "2[" + "Q")

    assertState(
      """qwe
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test q after opening bracket skips the current region backwards`() {
    configureByText(text)

    // [ goes to the first region, and q skips it backwards, wrapping around to the last occurrence
    typeText("<C-n><C-n>" + "[" + "q")

    assertState(
      """qwe
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test regions are still changed together`() {
    configureByText(text)

    typeText("<C-n><C-n>" + "]" + "cx<Esc>")

    assertState(
      """${c}x
      |asd
      |${c}x
      |asd
      |qwe
      """.trimMargin(),
    )
  }

  @Test
  fun `test bracket outside session keeps its Vim meaning`() {
    configureByText("(abc d${c}ef)")

    typeText("v[(")

    assertState("${s}${c}(abc de${se}f)")
  }

  @Test
  fun `test VM_maps changes Goto Prev key`() {
    configureByText(text)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Goto Prev'] = '<C-k>'")

    typeText("<C-n><C-n><C-n>" + "<C-k>" + "Q")

    assertState(
      """${s}qwe$se
      |asd
      |qwe
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }
}
