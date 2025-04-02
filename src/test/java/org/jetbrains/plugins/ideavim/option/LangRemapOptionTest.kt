/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class LangRemapOptionTest : VimTestCase("\n") {
  @Test
  fun `test default values of 'langremap' and 'langnoremap'`() {
    assertCommandOutput("set langremap?", "nolangremap")
    assertCommandOutput("set langnoremap?", "  langnoremap")
  }

  @Test
  fun `test changing 'langremap' updates 'langnoremap'`() {
    enterCommand("set langremap")
    assertCommandOutput("set langremap?", "  langremap")
    assertCommandOutput("set langnoremap?", "nolangnoremap")
  }

  @Test
  fun `test changing 'langnoremap' updates 'langremap'`() {
    enterCommand("set nolangnoremap")
    assertCommandOutput("set langremap?", "  langremap")
    assertCommandOutput("set langnoremap?", "nolangnoremap")
  }
}
