/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.annotations.TestWithPrimaryClipboard
import org.jetbrains.plugins.ideavim.annotations.TestWithoutPrimaryClipboard
import org.junit.jupiter.api.Test

class YankAndPutTest : VimTestCase() {
  // With autoselect, PRIMARY gets every line of a block, and * keeps the block type
  @TestWithPrimaryClipboard
  fun `test blockwise visual selection is put from primary register as a block`() {
    configureByText(
      """
        a${c}bc
        def
        ghi
      """.trimIndent(),
    )
    typeText("<C-V>jjl<Esc>", "gg0\"*P")
    assertState(
      """
        ${c}bcabc
        efdef
        highi
      """.trimIndent(),
    )
  }

  // With unnamed, a yank to * is copied to CLIPBOARD, and + has to know its type
  @TestWithPrimaryClipboard
  fun `test blockwise yank with unnamed keeps its type in plus register`() {
    configureByText(
      """
        a${c}bc
        def
      """.trimIndent(),
    )
    enterCommand("set clipboard=unnamed")
    typeText("<C-V>jly", "0\"+P")
    assertState(
      """
        ${c}bcabc
        efdef
      """.trimIndent(),
    )
    enterCommand("set clipboard&")
  }

  // Without PRIMARY, + is kept in the * register, as in Vim. The type of a yank to + has to be found there.
  @TestWithoutPrimaryClipboard
  fun `test blockwise yank to plus register keeps its type without primary clipboard`() {
    configureByText(
      """
        a${c}bc
        def
      """.trimIndent(),
    )
    typeText("<C-V>jl\"+y", "0\"+P")
    assertState(
      """
        ${c}bcabc
        efdef
      """.trimIndent(),
    )
  }

  @Test
  fun `test yank to number register with unnamedplus`() {
    val before = """
            I ${c}found it in a legendary land
            consectetur adipiscing elit
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamedplus")
    // Select and yank first word
    typeText(injector.parser.parseKeys("vey"))
    // Replace second word
    typeText(injector.parser.parseKeys("wvep"))
    // Replace previous word
    typeText(injector.parser.parseKeys("bbvep"))

    assertState(
      """
            I it found in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
  }

  @Test
  @TestWithoutPrimaryClipboard
  fun `test yank to number register with unnamed`() {
    val before = """
            I ${c}found it in a legendary land
            consectetur adipiscing elit
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamed")
    // Select and yank first word
    typeText(injector.parser.parseKeys("vey"))
    // Replace second word
    typeText(injector.parser.parseKeys("wvep"))
    // Replace previous word
    typeText(injector.parser.parseKeys("bbvep"))

    assertState(
      """
            I it found in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
  }

  @Test
  fun `test yank to number register with unnamedplus and ideaput`() {
    val before = """
            I ${c}found it in a legendary land
            consectetur adipiscing elit
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamedplus,ideaput")
    // Select and yank first word
    typeText(injector.parser.parseKeys("vey"))
    // Replace second word
    typeText(injector.parser.parseKeys("wvep"))
    // Replace previous word
    typeText(injector.parser.parseKeys("bbvep"))

    assertState(
      """
            I it found in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
  }

  @Test
  @TestWithoutPrimaryClipboard
  fun `test yank to number register with unnamed and ideaput`() {
    val before = """
            I ${c}found it in a legendary land
            consectetur adipiscing elit
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamed,ideaput")
    // Select and yank first word
    typeText(injector.parser.parseKeys("vey"))
    // Replace second word
    typeText(injector.parser.parseKeys("wvep"))
    // Replace previous word
    typeText(injector.parser.parseKeys("bbvep"))

    assertState(
      """
            I it found in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
  }

  @Test
  fun `test yank to number register`() {
    val before = """
            I ${c}found it in a legendary land
            consectetur adipiscing elit
    """.trimIndent()
    configureByText(before)
    // Select and yank first word
    typeText(injector.parser.parseKeys("vey"))
    // Replace second word
    typeText(injector.parser.parseKeys("wvep"))
    // Replace previous word
    typeText(injector.parser.parseKeys("bbvep"))

    assertState(
      """
            I it found in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
  }
}
