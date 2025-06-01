/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.register

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class RegisterMacroTest : VimTestCase() {

  @Test
  fun `test that macros work correctly with arrows`() {
    doTest(
      "qa\$as<esc><down>as<esc>q<down>@a", // Register a macro that adds s at the end of two lines and reruns it
      """
        1
        2
        3
        4
      """.trimMargin(),
      """
        1s
        2s
        3s
        4s
      """.trimMargin()
    )
  }
}