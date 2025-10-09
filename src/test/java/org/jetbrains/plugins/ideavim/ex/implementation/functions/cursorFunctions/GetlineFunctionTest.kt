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

class GetlineFunctionTest : VimTestCase() {
  @Test
  fun `test getline numeric strings and method`() {
    configureByText(
      """
      one
      two
      thre${c}e
      four
      five
      """.trimIndent(),
    )

    // Numeric string and with trailing chars
    assertCommandOutput("echo getline('3') getline('3foo')", "three three")

    // Method call form
    assertCommandOutput("echo 3->getline() '$'->getline()", "three five")

    // Range with string args
    assertCommandOutput("echo getline('2','4')", "['two', 'three', 'four']")
  }

  @Test
  fun `test getline current and last`() {
    configureByText(
      """
      one
      two
      thre${c}e
      four
      five
      """.trimIndent(),
    )

    // Current line and last line
    assertCommandOutput("echo getline('.')", "three")
    assertCommandOutput("echo getline('$')", "five")

    // Mark and visual start behaviour
    typeText("ma")
    assertCommandOutput("""echo getline("'a")""", "three")

    // Without selection - current line
    assertCommandOutput("""echo getline("v")""", "three")

    // With selection - make sure to delete the '<,'> automatically added when there is a selection
    typeText("vj")
    assertCommandOutput("""<C-U>echo getline("v")""", "three")

    // Remove selection and check again
    typeText("<esc>")
    assertCommandOutput("""echo getline("v")""", "three")

    // Invalid reference returns empty string
    assertCommandOutput("echo getline('abs') getline(0)", " ")
  }

  @Test
  fun `test getline ranges`() {
    configureByText(
      """
      one
      two
      thre${c}e
      four
      five
      """.trimIndent(),
    )

    // Numeric range
    assertCommandOutput("echo getline(1, 3)", "['one', 'two', 'three']")

    // Mixed references
    assertCommandOutput("echo getline('.', '$')", "['three', 'four', 'five']")

    // Out of range and reversed ranges produce empty list
    assertCommandOutput("echo getline(6, 7) getline(4, 2)", "[] []")
  }
}
