/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.search

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SearchHighlightsTest : VimTestCase() {
  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim highlights the match under the cursor after search; Vim does not",
  )
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

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim highlights the match under the cursor after search; Vim does not",
  )
  @Test
  fun `test star search with smartcase highlights case-insensitive matches`() {
    configureByText("${c}Lorem ipsum lorem ipsum")
    enterCommand("set hlsearch")
    enterCommand("set ignorecase")
    enterCommand("set smartcase")

    // `*` ignores smartcase, so the lowercase occurrence must be highlighted too
    typeText("*")

    assertSearchHighlights(
      "\\<Lorem\\>",
      "«Lorem» ipsum «lorem» ipsum",
    )
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim highlights the match under the cursor after search; Vim does not",
  )
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
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim highlights the match under the cursor after n/N; Vim does not",
  )
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

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim highlights the match under the cursor after n/N; Vim does not",
  )
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
        |all rocks and ‷lavender‴ and tufted grass,
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
           |all rocks ‷and‴ lavender «and» tufted grass,
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
           |all ‷and‴ lavender «and» tufted grass,
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
           |where it was ‷sled‴ on some sodden «sand»
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
           |where it was ‷sFOOettled‴ on some sodden «sand»
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
            |where it was ‷shuffled‴ on some sodden «sand»
            |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim tracks caret to show current match; Vim only shows it during incsearch",
  )
  @Test
  fun `test moving caret does not show or move current match highlight`() {
    configureByText("${c}lorem ipsum lorem ipsum")
    enterCommand("set hlsearch")

    val pattern = "lorem"
    enterSearch(pattern) // Moves the caret to the second occurrence
    assertSearchHighlights(pattern, "«lorem» ipsum «lorem» ipsum")

    typeText("gg0") // Back to the first occurrence
    assertSearchHighlights(pattern, "«lorem» ipsum «lorem» ipsum")
  }

  @Test
  fun `test editing text moves current match highlight under caret`() {
    configureByText("${c}lorem ipsum XXlorem ipsum")
    enterCommand("set hlsearch")

    val pattern = "lorem"
    enterSearch(pattern) // Moves the caret to the second occurrence
    typeText("hh") // Move to the "XX" before the match
    assertSearchHighlights(pattern, "«lorem» ipsum XX«lorem» ipsum")

    // Deleting "XX" doesn't move the caret, but it does move the match under it
    typeText("2x")
    assertSearchHighlights(pattern, "«lorem» ipsum ‷lorem‴ ipsum")
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim highlights the match under the cursor after search; Vim does not",
  )
  @Test
  fun `test search highlight with tabs`() {
    configureByText("\tfoo")
    enterCommand("set hlsearch")
    val pattern = "foo"
    enterSearch(pattern)
    assertSearchHighlights(pattern, "\t«foo»")
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim keeps current match highlight after search; Vim removes it on accept",
  )
  @Test
  fun `accepting search should not show current match highlight`() {
    configureByText("${c}lorem ipsum lorem ipsum")
    enterCommand("set hlsearch")

    val pattern = "lorem"
    enterSearch(pattern)

    assertSearchHighlights(pattern, "«lorem» ipsum «lorem» ipsum")
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim keeps current match highlight after n/N; Vim does not",
  )
  @Test
  fun `n does not show current match highlight`() {
    configureByText("${c}lorem ipsum lorem ipsum")
    enterCommand("set hlsearch")

    val pattern = "lorem"
    enterSearch(pattern)
    typeText("n")

    assertSearchHighlights(pattern, "«lorem» ipsum «lorem» ipsum")
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim keeps current match highlight after n/N; Vim does not",
  )
  @Test
  fun `N does not show current match highlight`() {
    configureByText("lorem ipsum ${c}lorem ipsum")
    enterCommand("set hlsearch")

    val pattern = "lorem"
    enterSearch(pattern)
    typeText("N")

    assertSearchHighlights(pattern, "«lorem» ipsum «lorem» ipsum")
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim tracks caret position to update current match; Vim does not",
  )
  @Test
  fun `moving caret onto a match does not show current match highlight`() {
    configureByText("${c}lorem ipsum lorem ipsum")
    enterCommand("set hlsearch")

    val pattern = "lorem"
    enterSearch(pattern)
    typeText("gg0")

    assertSearchHighlights(pattern, "«lorem» ipsum «lorem» ipsum")
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim tracks caret position to update current match; Vim does not",
  )
  @Test
  fun `moving caret off a match does not affect current match highlight`() {
    configureByText("${c}lorem ipsum lorem ipsum")
    enterCommand("set hlsearch")

    val pattern = "lorem"
    enterSearch(pattern)
    typeText("$")

    assertSearchHighlights(pattern, "«lorem» ipsum «lorem» ipsum")
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Neovim keeps current match highlight after search; Vim removes it on accept",
  )
  @Test
  fun `accepting incsearch removes current match highlight`() {
    configureByText("${c}lorem ipsum lorem ipsum")
    enterCommand("set hlsearch incsearch")

    val pattern = "lorem"
    typeText("/$pattern")
    typeText("<CR>")

    assertSearchHighlights(pattern, "«lorem» ipsum «lorem» ipsum")
  }
}
