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

    enterCommand("unmap k")

    assertNoMapping("k")
  }

  @Test
  fun `test mappings in insert mode`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    enterCommand("iunmap jk")

    assertNoMapping("jk")
  }

  @Test
  fun `test removing only part of keys`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    enterCommand("iunmap j")

    assertNoMapping("j")
    assertMappingExists("jk", "<Esc>", MappingMode.I)
  }

  @Test
  fun `test removing mapping that is also a prefix`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)
    putMapping(MappingMode.I, "jkl", "<Esc>", false)

    enterCommand("iunmap jk")

    assertNoMapping("jk")
    assertMappingExists("jkl", "<Esc>", MappingMode.I)
  }

  @Test
  fun `test removing keys from a different mode`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    enterCommand("unmap jk")

    assertMappingExists("jk", "<Esc>", MappingMode.I)
  }

  @Test
  fun `test removing IC mapping`() {
    putMapping(MappingMode.IC, "foo", "bar", false)
    putMapping(MappingMode.IC, "quux", "baz", false)
    putMapping(MappingMode.N, "foo", "bar", false)

    // We've just mapped "foo" to "bar" in Command-line mode. We can't type it directly!
    // And enterCommand doesn't parse special keys!
    typeText(":unmap! fox<BS>o<CR>")

    assertNoMapping("foo", MappingMode.IC)
    assertMappingExists("quux", "baz", MappingMode.IC)
    assertMappingExists("foo", "bar", MappingMode.N)
  }

  @Test
  fun `test removing IC mapping with abbreviated command`() {
    putMapping(MappingMode.IC, "foo", "bar", false)
    putMapping(MappingMode.IC, "quux", "baz", false)
    putMapping(MappingMode.N, "foo", "bar", false)

    // We've just mapped "foo" to "bar" in Command-line mode. We can't type it directly!
    // And enterCommand doesn't parse special keys!
    typeText(":unmap! fox<BS>o<CR>")

    assertNoMapping("foo", MappingMode.IC)
    assertMappingExists("quux", "baz", MappingMode.IC)
    assertMappingExists("foo", "bar", MappingMode.N)
  }

  @Test
  fun `test error using bang with unmap commands`() {
    enterCommand("vunmap! foo")

    assertPluginError(true)
    assertPluginErrorMessage("E477: No ! allowed")
  }
}
