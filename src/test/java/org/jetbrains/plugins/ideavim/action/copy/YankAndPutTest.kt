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
import org.jetbrains.plugins.ideavim.annotations.TestWithoutPrimaryClipboard
import org.junit.jupiter.api.Test

class YankAndPutTest : VimTestCase() {
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
