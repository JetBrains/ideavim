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
 * `p` and `P` with the text yanked by each cursor, or with the same text for all of them
 */
class VisualMultiPasteTest : VisualMultiTestCase() {

  private val text = """${c}abc
    |def
  """.trimMargin()

  @Test
  fun `test p pastes text yanked by each cursor`() {
    configureByText(text)

    typeText("Vj" + "<Bslash><Bslash>a" + "y" + "$" + "p")

    assertState(
      """abcab${c}c
      |defde${c}f
      """.trimMargin(),
    )
  }

  @Test
  fun `test P pastes text yanked by each cursor`() {
    configureByText(text)

    typeText("Vj" + "<Bslash><Bslash>a" + "y" + "P")

    assertState(
      """ab${c}cabc
      |de${c}fdef
      """.trimMargin(),
    )
  }

  @Test
  fun `test p replaces each region with its yanked text`() {
    configureByText(text)

    typeText("Vj" + "<Bslash><Bslash>a" + "y" + "<Tab>" + "p")

    assertState(
      """ab${c}cbc
      |de${c}fef
      """.trimMargin(),
    )
  }

  @Test
  fun `test p pastes text yanked before the session at each cursor`() {
    configureByText(text)

    typeText("yiw" + "<C-Down>" + "$" + "p")

    assertState(
      """abcab${c}c
      |defab${c}c
      """.trimMargin(),
    )
  }
}
