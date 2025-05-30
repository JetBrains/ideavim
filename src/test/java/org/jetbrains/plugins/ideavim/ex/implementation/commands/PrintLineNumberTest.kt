/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class PrintLineNumberTest : VimTestCase() {
  @Test
  fun `test print last line number`() {
    configureByLines(10, "Lorem ipsum dolor sit amet")
    enterCommand("=")
    assertStatusLineMessageContains("10")
  }

  @Test
  fun `test print current line number`() {
    configureByLines(10, "Lorem ipsum dolor sit amet")
    typeText("4j")
    enterCommand(".=")
    assertStatusLineMessageContains("5")
  }

  @Test
  fun `test print specific line number`() {
    configureByLines(10, "Lorem ipsum dolor sit amet")
    enterCommand("7=")
    assertStatusLineMessageContains("7")
  }

  @Test
  fun `test print line number of last part of range`() {
    configureByLines(10, "Lorem ipsum dolor sit amet")
    enterCommand("1,5=")
    assertStatusLineMessageContains("5")
  }

  @Test
  fun `test trailing characters raises an error`() {
    configureByLines(10, "Lorem ipsum dolor sit amet")
    enterCommand("=foo")
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: foo")
  }

  @Test
  fun `test # flag prints line content and number`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |...consectetur adipiscing elit
      |Maecenas efficitur nec odio vel malesuada
    """.trimMargin().dotToTab())
    enterCommand("2=#")
    assertStatusLineMessageContains("2 \t\t\tconsectetur adipiscing elit")
  }

  @Test
  fun `test l flag prints line content as printable string and number`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |...consectetur adipiscing elit
      |Maecenas efficitur nec odio vel malesuada
    """.trimMargin().dotToTab())
    enterCommand("2=l")
    assertStatusLineMessageContains("2 ^I^I^Iconsectetur adipiscing elit")
  }

  @Test
  fun `test l and p flags print line content as printable string and number`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |...consectetur adipiscing elit
      |Maecenas efficitur nec odio vel malesuada
    """.trimMargin().dotToTab())
    enterCommand("2=lp")
    assertStatusLineMessageContains("2 ^I^I^Iconsectetur adipiscing elit")
  }

  @Test
  fun `test p flag prints line content and number`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |...consectetur adipiscing elit
      |Maecenas efficitur nec odio vel malesuada
    """.trimMargin().dotToTab())
    enterCommand("2=p")
    assertStatusLineMessageContains("2 \t\t\tconsectetur adipiscing elit")
  }

  @Test
  fun `test p and # flag prints line content and number`() {
    configureByText("""
      |Lorem ipsum dolor sit amet
      |...consectetur adipiscing elit
      |Maecenas efficitur nec odio vel malesuada
    """.trimMargin().dotToTab())
    enterCommand("2=p#")
    assertStatusLineMessageContains("2 \t\t\tconsectetur adipiscing elit")
  }
}
