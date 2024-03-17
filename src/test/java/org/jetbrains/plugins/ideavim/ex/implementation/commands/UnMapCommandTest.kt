/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.command.MappingMode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class UnMapCommandTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText(
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }

  @Test
  fun testMapKtoJ() {
    putMapping(MappingMode.N, "k", "j", false)

    typeText(commandToKeys("unmap k"))

    assertNoMapping("k")
  }

  @Test
  fun `test mappings in insert mode`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    typeText(commandToKeys("iunmap jk"))

    assertNoMapping("jk")
  }

  @Test
  fun `test removing only part of keys`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    typeText(commandToKeys("unmap j"))

    assertNoMapping("j")
    assertMappingExists("jk", "<Esc>", MappingMode.I)
  }

  @Test
  fun `test removing keys from a different mode`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    typeText(commandToKeys("unmap jk"))

    assertMappingExists("jk", "<Esc>", MappingMode.I)
  }
}
