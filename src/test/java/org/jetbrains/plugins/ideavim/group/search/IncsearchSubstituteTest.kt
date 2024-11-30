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

@Suppress("SpellCheckingInspection")
class IncsearchSubstituteTest : VimTestCase() {
  @Test
  fun `test incsearch highlight with force sensitive case atom`() {
    configureByText("lorem ipsum Lorem Ipsum lorem ipsum")
    enterCommand("set hlsearch incsearch")

    typeText("/", "\\Clorem")

    assertSearchHighlights("\\Clorem", "«lorem» ipsum Lorem Ipsum ‷lorem‴ ipsum")
  }

  @Test
  fun `test incsearch highlights for substitute command`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "%s/and")

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
  fun `test incsearch only highlights for substitute command after valid argument`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    // E.g., don't remove highlights when trying to type :set
    enterSearch("and")
    typeText(":s")

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
  fun `test incsearch highlights for substitute command only highlights in range`() {
    configureByText(
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |${c}hard by the torrent and rush of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "2,3s/and")

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
  fun `test incsearch highlights for substitute command if range larger than file`() {
    configureByText(
      """
        |I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |${c}hard by the torrent and rush of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "1,300s/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l‷and‴
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent «and» rush of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch highlights for substitute command with reversed range`() {
    configureByText(
      """
        |I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |${c}hard by the torrent and rush of a mountain pass.
        |I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent and rush of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "8,2s/and")

    assertSearchHighlights(
      "and",
      """
        |I found it in a legendary land
        |all rocks ‷and‴ lavender «and» tufted grass,
        |where it was settled on some sodden s«and»
        |hard by the torrent «and» rush of a mountain pass.
        |I found it in a legendary l«and»
        |all rocks «and» lavender «and» tufted grass,
        |where it was settled on some sodden s«and»
        |hard by the torrent «and» rush of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch highlights nothing for substitute with range after end of file`() {
    configureByText(
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |${c}hard by the torrent and rush of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "100,300s/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent and rush of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch highlights for substitute command in current line with no range`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "s/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch for substitute command starts at beginning of range not caret position`() {
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

    typeText(":", "2,8s/and")

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
  fun `test incsearch highlights for substitute command clears highlights on backspace`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "%s/and", "<BS><BS><BS>")

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
  fun `test incsearch highlights for substitute command resets highlights on backspace`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")  // Moves the caret to "and" on the second line: (1, 10)
    typeText(":", "%s/roc", "<BS><BS><BS>")

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
  fun `test cancelling incsearch highlights for substitute command shows previous highlights`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")  // Moves the care to "and" on the second line: (1, 10)
    typeText(":", "%s/ass", "<Esc>")

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
