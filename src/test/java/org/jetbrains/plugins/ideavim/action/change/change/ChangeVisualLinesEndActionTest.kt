/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeVisualLinesEndActionTest : VimTestCase() {
  @Test
  fun `test change last line`() {
    val keys = "VC"
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            ${c}Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            ${c}
    """.trimIndent()
    doTest(keys, before, after, Mode.INSERT)
  }

  @Test
  fun `test last empty line`() {
    val keys = "vC"
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
            ${c}
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
            ${c}
    """.trimIndent()
    doTest(keys, before, after, Mode.INSERT)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            ${c}
  """,
  )
  @Test
  fun `test change last two lines`() {
    val keys = "vjC"
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            hard by the torrent of a mountain pass${c}.
            
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            ${c}
            
    """.trimIndent()
    doTest(keys, before, after, Mode.INSERT)
  }
}
