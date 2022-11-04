/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.command.MappingMode
import org.jetbrains.plugins.ideavim.VimTestCase

class UnMapCommandTest : VimTestCase() {
  override fun setUp() {
    super.setUp()
    configureByText(
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent()
    )
  }

  fun testMapKtoJ() {
    putMapping(MappingMode.N, "k", "j", false)

    typeText(commandToKeys("unmap k"))

    assertNoMapping("k")
  }

  fun `test mappings in insert mode`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    typeText(commandToKeys("iunmap jk"))

    assertNoMapping("jk")
  }

  fun `test removing only part of keys`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    typeText(commandToKeys("unmap j"))

    assertNoMapping("j")
    assertMappingExists("jk", "<Esc>", MappingMode.I)
  }

  fun `test removing keys from a different mode`() {
    putMapping(MappingMode.I, "jk", "<Esc>", false)

    typeText(commandToKeys("unmap jk"))

    assertMappingExists("jk", "<Esc>", MappingMode.I)
  }
}
