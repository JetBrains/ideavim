/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.search

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class IncsearchVGlobalTest : VimTestCase() {
  @Test
  fun `test incsearch highlights for vglobal command with range`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "%v/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l‷and‴
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch highlights for vglobal command in whole file with default range`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "v/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l‷and‴
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch only highlights for vglobal command after valid argument`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    // E.g., don't remove highlights when trying to type :vmap
    enterSearch("and")
    typeText(":v")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch highlights for vglobal command only highlights in range`() {
    configureByText(
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |${c}hard by the torrent and rush of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "2,3v/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent and rush of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch for vglobal command starts at beginning of range not caret position`() {
    configureByText(
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
           |${c}I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
           |I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "2,8v/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
           |I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
           |I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch highlights for vglobal command clears highlights on backspace`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "v/and", "<BS><BS><BS>")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
             |all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch highlights for vglobal command resets highlights on backspace`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")  // Moves the caret to "and" on the second line: (1, 10)
    typeText(":", "v/roc", "<BS><BS><BS>")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )

    // Make sure the caret is reset too
    assertPosition(1, 10)
  }

  @Test
  fun `test cancelling incsearch highlights for vglobal command shows previous highlights`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")  // Moves the caret to "and" on the second line: (1, 10)
    typeText(":", "v/ass", "<Esc>")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )

    // Make sure the caret is reset too
    assertPosition(1, 10)
  }
}
