/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import org.jetbrains.plugins.ideavim.OptionValueType
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption

class YankAndPutTest : VimOptionTestCase(OptionConstants.clipboard) {
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.clipboard, OptionValueType.STRING, OptionConstants.clipboard_unnamed),
  )
  fun `test yank to number register with unnamed`() {
    val before = """
            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
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
            all rocks and lavender and tufted grass,
      """.trimIndent(),
    )
  }

  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.clipboard,
      OptionValueType.STRING,
      OptionConstants.clipboard_unnamed + "," + OptionConstants.clipboard_ideaput,
    ),
  )
  fun `test yank to number register with unnamed and ideaput`() {
    val before = """
            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
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
            all rocks and lavender and tufted grass,
      """.trimIndent(),
    )
  }

  @VimOptionTestConfiguration(VimTestOption(OptionConstants.clipboard, OptionValueType.STRING, ""))
  fun `test yank to number register`() {
    val before = """
            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
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
            all rocks and lavender and tufted grass,
      """.trimIndent(),
    )
  }
}
