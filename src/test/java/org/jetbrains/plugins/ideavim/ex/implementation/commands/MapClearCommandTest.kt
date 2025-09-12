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

class MapClearCommandTest : VimTestCase() {
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

    enterCommand("mapclear")

    assertNoMapping("k")
  }

  @Test
  fun `test mappings in insert mode`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    enterCommand("imapclear")

    assertNoMapping("jk")
  }

  @Test
  fun `test removing only part of keys`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)
    putMapping(MappingMode.N, "jk", "<Esc>", false)

    enterCommand("imapclear")

    assertNoMapping("jk", MappingMode.I)
    assertMappingExists("jk", "<Esc>", MappingMode.N)
  }

  @Test
  fun `test removing IC mappings`() {
    putMapping(MappingMode.IC, "foo", "bar", false)
    putMapping(MappingMode.N, "foo", "bar", false)

    enterCommand("mapclear!")

    assertNoMapping("foo", MappingMode.IC)
    assertMappingExists("foo", "bar", MappingMode.N)
  }

  @Test
  fun `test removing IC mappings with abbreviated command`() {
    putMapping(MappingMode.IC, "foo", "bar", false)
    putMapping(MappingMode.N, "foo", "bar", false)

    enterCommand("mapc!")

    assertNoMapping("foo", MappingMode.IC)
    assertMappingExists("foo", "bar", MappingMode.N)
  }

  @Test
  fun `test error using bang with mapclear commands`() {
    enterCommand("cmapclear!")

    assertPluginError(true)
    assertPluginErrorMessage("E477: No ! allowed")
  }
}
