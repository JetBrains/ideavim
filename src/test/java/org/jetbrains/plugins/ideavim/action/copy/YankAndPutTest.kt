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
import org.jetbrains.plugins.ideavim.TestOptionConstants
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.annotations.TestWithoutPrimaryClipboard
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption

@TraceOptions(TestOptionConstants.clipboard)
class YankAndPutTest : VimTestCase() {
  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [OptionConstants.clipboard_unnamedplus]))
  fun `test yank to number register with unnamedplus`() {
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

  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [OptionConstants.clipboard_unnamed]))
  @TestWithoutPrimaryClipboard
  fun `test yank to number register with unnamed`() {
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

  @OptionTest(
    VimOption(
      TestOptionConstants.clipboard,
      limitedValues = [OptionConstants.clipboard_unnamedplus + "," + OptionConstants.clipboard_ideaput]
    )
  )
  fun `test yank to number register with unnamedplus and ideaput`() {
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

  @OptionTest(
    VimOption(
      TestOptionConstants.clipboard,
      limitedValues = [OptionConstants.clipboard_unnamed + "," + OptionConstants.clipboard_ideaput]
    )
  )
  @TestWithoutPrimaryClipboard
  fun `test yank to number register with unnamed and ideaput`() {
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

  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [""]))
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
