/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class VisualBlockAppendActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test visual block append`() {
    val before = """
            ${c}int a;
            int b;
            int c;
    """.trimIndent()
    typeTextInFile(injector.parser.parseKeys("<C-V>" + "2j" + "e" + "A" + " const" + "<Esc>"), before)
    val after = """
            int const a;
            int const b;
            int const c;
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test visual block append with dollar motion`() {
    val before = """
            ${c}int a;
            private String b;
            int c;
    """.trimIndent()
    typeTextInFile(injector.parser.parseKeys("<C-V>" + "2j" + "$" + "A" + " // My variables" + "<Esc>"), before)
    val after = """
            int a; // My variables
            private String b; // My variables
            int c; // My variables
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test append in non block mode`() {
    doTest(
      "vwAHello<esc>",
      """
                ${c}A Discovery

                ${c}I found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A DiscoveryHell${c}o

                I found it in a legendary landHell${c}o
                all rocks and lavender and tufted grass,Hell${c}o
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
    assertMode(Mode.NORMAL())
  }
}
