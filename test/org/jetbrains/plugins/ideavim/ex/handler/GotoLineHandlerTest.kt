/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class GotoLineHandlerTest : VimTestCase() {
  fun `test goto explicit line`() {
    val before = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("3")
    val after = """
      A Discovery

      ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  fun `test goto explicit line check history`() {
    val before = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("3")
    val after = """
      A Discovery

      ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)

    val register = VimPlugin.getRegister().getRegister(':')
    kotlin.test.assertNotNull(register)
    kotlin.test.assertEquals("3", register.text)
  }

  fun `test goto positive relative line`() {
    val before = """
      A Discovery

      I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("+2")
    val after = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      ${c}where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  fun `test goto negative relative line`() {
    val before = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it ${c}was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("-2")
    val after = """
      A Discovery

      ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  fun `test goto line moves to first non-blank char`() {
    val before = """
      A Discovery

          I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("3")
    val after = """
      A Discovery

          ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  fun `test goto zero relative line moves to first non-blank char on current line`() {
    val before = """
      A Discovery

          I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("+0")
    val after = """
      A Discovery

          ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test goto line moves to same column with nostartofline option`() {
    OptionsManager.startofline.reset()
    val before = """
      A Discovery

          I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("3")
    val after = """
      A Discovery

          I found ${c}it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test goto zero relative line with nostartofline option does not move caret`() {
    OptionsManager.startofline.reset()
    val before = """
      A Discovery

          I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("+0")
    val after = """
      A Discovery

          I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  fun `test goto line with scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("30")
    assertPosition(29, 4)
    assertTopLogicalLine(5)
  }

  fun `test goto relative line with scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("+30")
    assertPosition(30, 4)
    assertTopLogicalLine(6)
  }
}
