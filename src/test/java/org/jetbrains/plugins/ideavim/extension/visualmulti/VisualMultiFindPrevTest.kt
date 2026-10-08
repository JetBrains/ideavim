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
 * Find Prev (`N`) inside the session, and Skip Region (`q`) following its direction
 */
class VisualMultiFindPrevTest : VisualMultiTestCase() {

  @Test
  fun `test N adds previous occurrence`() {
    configureByText(
      """qwe
      |asd
      |qwe
      |asd
      |q${c}we
      """.trimMargin(),
    )

    typeText("<C-n>" + "N")

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
  fun `test N again adds occurrence before the last added one`() {
    configureByText(
      """qwe
      |asd
      |qwe
      |asd
      |q${c}we
      """.trimMargin(),
    )

    typeText("<C-n>" + "NN")

    assertState(
      """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test count for N`() {
    configureByText(
      """qwe
      |asd
      |qwe
      |asd
      |q${c}we
      """.trimMargin(),
    )

    typeText("<C-n>" + "2N")

    assertState(
      """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test N wraps around`() {
    configureByText(
      """q${c}we
      |asd
      |qwe
      """.trimMargin(),
    )

    typeText("<C-n>" + "N")

    assertState(
      """${s}qwe$se
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test N does not wrap around with nowrapscan`() {
    configureByText(
      """q${c}we
      |asd
      |qwe
      """.trimMargin(),
    )
    enterCommand("set nowrapscan")

    typeText("<C-n>" + "N")

    assertState(
      """${s}qwe$se
      |asd
      |qwe
      """.trimMargin(),
    )
  }

  @Test
  fun `test N stops when all occurrences are selected`() {
    configureByText(
      """q${c}we
      |asd
      |qwe
      """.trimMargin(),
    )

    typeText("<C-n><C-n>" + "N")

    assertState(
      """${s}qwe$se
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test N uses whole word search of ctrl-n`() {
    configureByText(
      """qwe
      |qwerty
      |q${c}we
      """.trimMargin(),
    )

    typeText("<C-n>" + "N")

    assertState(
      """${s}qwe$se
      |qwerty
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test q after N skips backwards`() {
    configureByText(
      """qwe
      |qwe
      |qwe
      |q${c}we
      """.trimMargin(),
    )

    // The second region from the bottom is skipped to the one above it
    typeText("<C-n>" + "N" + "q")

    assertState(
      """qwe
      |${s}qwe$se
      |qwe
      |${s}qwe$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test N outside session keeps its Vim meaning`() {
    configureByText("${c}bar foo bar foo")
    enterSearch("bar")

    typeText("$" + "vN")

    assertState("bar foo ${s}${c}bar foo$se")
  }

  @Test
  fun `test VM_maps changes Find Prev key`() {
    configureByText(
      """qwe
      |asd
      |q${c}we
      """.trimMargin(),
    )
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Prev'] = '<C-p>'")

    typeText("<C-n>" + "<C-p>")

    assertState(
      """${s}qwe$se
      |asd
      |${s}qwe$se
      """.trimMargin(),
    )
  }
}
