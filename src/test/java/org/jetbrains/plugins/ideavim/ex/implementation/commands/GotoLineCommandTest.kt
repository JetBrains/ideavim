/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class GotoLineCommandTest : VimTestCase() {
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

  fun `test goto using forward search range`() {
    val before = """
      A Discovery

      I found it ${c}in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("/settled")
    val after = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      ${c}where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  fun `test goto using backward search range`() {
    val before = """
      A Discovery

      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    enterCommand("/lavender")
    val after = """
      A Discovery

      I found it in a legendary land
      ${c}all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
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
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startofline)
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
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startofline)
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
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("30")
    assertPosition(29, 4)
    assertTopLogicalLine(5)
  }

  fun `test goto relative line with scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("+30")
    assertPosition(30, 4)
    assertTopLogicalLine(6)
  }
}
