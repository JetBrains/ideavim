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
 * The `<Plug>` mappings of Skip Region and Remove Region
 */
class VisualMultiSkipAndRemoveRegionTest : VisualMultiTestCase() {

  @Test
  fun `test skip region plug mapping`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("xmap <C-x> <Plug>(VM-Skip-Region)")

    // Select the first occurrence, skip it to the second, then add the third
    typeText("<C-n>" + "<C-x>" + "<C-n>")

    val after = """qwe
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test remove region plug mapping`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("xmap <C-p> <Plug>(VM-Remove-Region)")

    // Add three cursors, then remove the last one
    typeText("<C-n>".repeat(3) + "<C-p>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test removing the last region exits visual mode`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("xmap <C-p> <Plug>(VM-Remove-Region)")

    typeText("<C-n><C-n>" + "<C-p><C-p>")

    assertMode(Mode.NORMAL())
  }
}
