/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.cursorFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ColFunctionTest : VimTestCase() {
  // XXX virtualedit is not tested
  @Test
  fun `test col`() {
    configureByText(
      """
  1
  2
  1234${c}567890
  4
  5
      """.trimIndent(),
    )

    assertCommandOutput("echo col('.')", "5")
    assertCommandOutput("echo col('$')", "10")

    typeText("ma")
    assertCommandOutput("""echo col("'a") col("'z")""", "5 0")

    // Without selection - current line
    assertCommandOutput("""echo col("v")""", "5")

    // With selection - make sure to delete the '<,'> that is automatically prepended when entering Command-line mode
    // with a selection
    typeText("vll")
    assertCommandOutput("""<C-U>echo col("v")""", "5")

    // Remove selection and check again - note that exiting Command-line mode removes selection and switches back to
    // Normal. This <esc> does nothing
    typeText("<esc>")
    assertCommandOutput("""echo col("v")""", "5")

    assertCommandOutput("echo col('$')", "10")
    assertCommandOutput("""echo col("abs") col(1) col([])""", "0 0 0")
    assertCommandOutput("""echo col([1, 1]) col([3, '$'])  col(['.', '$']) col(['$', '$'])""", "1 11 0 0")
    assertCommandOutput("""echo col([0, 1]) col([1, 1]) col([5, 1]) col([6, 1]) col([5, 2])""", "0 1 1 0 2")
  }
}
