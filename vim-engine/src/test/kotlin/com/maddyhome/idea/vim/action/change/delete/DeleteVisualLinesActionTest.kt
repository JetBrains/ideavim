/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package com.maddyhome.idea.vim.action.change.delete

import com.maddyhome.idea.vim.model.VimTestCase
import com.maddyhome.idea.vim.state.mode.Mode
import org.junit.jupiter.api.Test

interface DeleteVisualLinesActionTest : VimTestCase {
  @Test
  fun `test remove line in char visual mode`() {
    configureByText(
      """
        I found ${c}it in a legendary land
        consectetur adipiscing elit
        Sed in orci mauris.
        Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    typeText("vlllX")
    assertState(
      """
        ${c}consectetur adipiscing elit
        Sed in orci mauris.
        Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL()
    )
  }

  @Test
  fun `test remove line in char visual mode last line`() {
    configureByText(
      """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        hard by ${c}the torrent of a mountain pass.
      """.trimIndent(),
    )
    typeText("vlllX")
    assertState(
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
    configureByText(
      """
        I found ${c}it in a legendary land
        consectetur adipiscing elit
        Sed in orci mauris.
        Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    typeText("VX")
    assertState(
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
    configureByText(
      """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        hard by ${c}the torrent of a mountain pass.
      """.trimIndent(),
    )
    typeText("VX")
    assertState(
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
    configureByText(
      """
        Lorem Ipsum

        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        
        ${c}Sed in orci mauris.
        Cras id tellus in ex imperdiet egestas.
      """.trimIndent()
    )
    typeText("Vjd")
    assertState(
      """
        Lorem Ipsum

        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        ${c}
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test multiple line delete till the end with a new line`() {
    configureByText(
      """
        Lorem Ipsum

        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        
        ${c}Sed in orci mauris.
        Cras id tellus in ex imperdiet egestas.
        
    """.trimIndent()
    )
    typeText("Vjd")
    assertState(
      """
        Lorem Ipsum

        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        
        ${c}
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}
