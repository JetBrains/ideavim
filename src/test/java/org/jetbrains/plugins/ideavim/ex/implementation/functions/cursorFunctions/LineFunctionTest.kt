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

class LineFunctionTest : VimTestCase() {
  @Test
  fun `test line`() {
    configureByText("1\n2\n${c}3\n4\n5")

    assertCommandOutput("echo line('.')", "3")
    assertCommandOutput("echo line('$')", "5")

    typeText("ma")
    assertCommandOutput("""echo line("'a") line("'x")""", "3 0")

    setEditorVisibleSize(screenWidth, 3)
    setPositionAndScroll(2, 2)
    assertCommandOutput("""echo line("w0") line("w$")""", "3 5")

    // Without selection - current line
    assertCommandOutput("""echo line("v")""", "3")

    // With selection - make sure to delete the '<,'> that is automatically prepended when entering Command-line mode
    // with a selection
    typeText("vj")
    assertCommandOutput("""<C-U>echo line("v")""", "3")

    // Remove selection and check again - note that exiting Command-line mode removes selection and switches back to
    // Normal. This <esc> does nothing
    typeText("<esc>")
    assertCommandOutput("""echo line("v")""", "3")
    assertCommandOutput("""echo line("abs") line(1) line([])""", "0 0 0")
    assertCommandOutput("""echo line([1, 1]) line(['.', '$']) line(['$', '$'])""", "1 0 0")
    assertCommandOutput(
      """echo line([0, 1]) line([1, 1]) line([5, 1]) line([6, 1]) line([5, 2]) line([5, 3])""",
      "0 1 5 0 5 0"
    )
  }
}
