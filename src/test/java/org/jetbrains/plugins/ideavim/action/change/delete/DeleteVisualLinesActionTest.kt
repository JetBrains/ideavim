/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class DeleteVisualLinesActionTest : VimTestCase() {
  @Test
  fun `test remove line in char visual mode`() {
    doTest(
      "vlllX",
      """
                I found ${c}it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                ${c}consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test remove line in char visual mode last line`() {
    doTest(
      "vlllX",
      """
                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                hard by ${c}the torrent of a mountain pass.
      """.trimIndent(),
      """
                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                ${c}Sed in orci mauris.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test remove line in line visual mode`() {
    doTest(
      "VX",
      """
                I found ${c}it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                ${c}consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test remove line in line visual mode line end`() {
    doTest(
      "VX",
      """
                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                hard by ${c}the torrent of a mountain pass.
      """.trimIndent(),
      """
                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                ${c}Sed in orci mauris.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test multiple line delete till the end`() {
    val keys = "Vjd"
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            
            ${c}Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            ${c}
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test multiple line delete till the end with a new line`() {
    val keys = "Vjd"
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            
            ${c}Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
            
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            
            ${c}
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }
}
