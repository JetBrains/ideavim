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
import kotlin.test.assertEquals

class SearchHighlightsTest : VimTestCase() {
  @Test
  fun `test highlight search results`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test search removes previous search highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch("mountain")
    enterSearch(pattern)

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test no highlights for unmatched search`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")
    enterSearch("zzzz")
    assertNoSearchHighlights()
  }

  @Test
  fun `test nohlsearch command removes highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")
    enterSearch("and")
    enterCommand("nohlsearch")
    assertNoSearchHighlights()
  }

  @Test
  fun `test set 'hlsearch' option after nohlsearch command shows highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    enterCommand("nohlsearch")
    enterCommand("set hlsearch")

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test find next after nohlsearch command shows highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    enterCommand("nohlsearch")
    typeText("n")

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test nohlsearch correctly resets incsearch highlights after deleting last occurrence`() {
    // Crazy edge case bug. With incsearch enabled, search for something with only one occurrence, delete it, call
    // :nohlsearch, undo and search next - highlights don't work anymore
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    val pattern = "lavender"
    enterSearch(pattern)
    typeText("dd")
    enterCommand("nohlsearch")
    typeText("u", "n")

    assertSearchHighlights(
      pattern,
      """I found it in a legendary land
        |all rocks and «lavender» and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test nohlsearch option hides search highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")
    enterSearch("and")
    enterCommand("set nohlsearch")
    assertNoSearchHighlights()
  }

  @Test
  fun `test setting hlsearch option shows search highlights for last search`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )

    val pattern = "and"
    enterSearch(pattern)
    enterCommand("set hlsearch")

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test deleting text moves search highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    typeText("b", "dw") // deletes "rocks "

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test deleting match removes search highlight`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    typeText("dw") // deletes first "and " on line 2

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test deleting part of match removes search highlight`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    typeText("xx") // deletes "an" from first "and" on line 2

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks d lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test deleting part of match keeps highlight if pattern still matches`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = """\<s\w*d\>""" // Should match "settled" and "sand"
    enterSearch(pattern)
    typeText("l", "xxx") // Change "settled" to "sled"

    assertSearchHighlights(
      pattern,
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was «sled» on some sodden «sand»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test inserting text moves search highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    typeText("h", "i", ", trees") // inserts ", trees" before first "and" on line 2

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks, trees «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test inserting text inside match removes search highlight`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    typeText("l", "i", "FOO") // inserts "FOO" inside first "and" - "aFOOnd"

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks aFOOnd lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test inserting text inside match keeps highlight if pattern still matches`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = """\<s\w*d\>""" // Should match "settled" and "sand"
    enterSearch(pattern)
    typeText("l", "i", "FOO", "<Esc>") // Change "settled" to "sFOOettled"

    assertSearchHighlights(
      pattern,
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was «sFOOettled» on some sodden «sand»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test inserting text shows highlight if it contains matches`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    typeText("o", "and then I saw a cat and a dog", "<Esc>")

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |«and» then I saw a cat «and» a dog
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test replacing text moves search highlights`() {
    val pattern = "and"
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    enterSearch(pattern)
    typeText("b", "cw", "boulders", "<Esc>") // Replaces "rocks" with "boulders" on line 2

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all boulders «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test replacing text inside match removes search highlight`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    typeText("l", "cw", "lso", "<Esc>") // replaces "nd" in first "and" with "lso" on line 2

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
         |all rocks also lavender «and» tufted grass,
         |where it was settled on some sodden s«and»
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test replacing text shows highlight if it contains matches`() {
    configureByText(
      """I found it in a legendary land
          |${c}all rocks and lavender and tufted grass,
          |where it was settled on some sodden sand
          |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)
    typeText("w", "cw", "trees and boulders", "<Esc>")

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
          |all rocks «and» trees «and» boulders «and» tufted grass,
          |where it was settled on some sodden s«and»
          |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test replacing text inside match keeps highlight if pattern still matches`() {
    configureByText(
      """I found it in a legendary land
            |${c}all rocks and lavender and tufted grass,
            |where it was settled on some sodden sand
            |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch")

    val pattern = """\<s\w*d\>""" // Should match "settled" and "sand"
    enterSearch(pattern)
    typeText("l", "ctl", "huff", "<Esc>") // Change "settled" to "shuffled"

    assertSearchHighlights(
      pattern,
      """I found it in a legendary land
            |all rocks and lavender and tufted grass,
            |where it was «shuffled» on some sodden «sand»
            |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test search highlight with tabs`() {
    configureByText("\tfoo")
    enterCommand("set hlsearch")
    val pattern = "foo"
    enterSearch(pattern)
    assertSearchHighlights(pattern, "\t«foo»")
  }

  private fun assertNoSearchHighlights() {
    assertEquals(0, fixture.editor.markupModel.allHighlighters.size)
  }
}
