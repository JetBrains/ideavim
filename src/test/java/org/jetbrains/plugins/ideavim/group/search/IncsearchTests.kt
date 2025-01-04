/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.search

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class IncsearchTests : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test incsearch moves caret to start of first match`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set incsearch")
    typeText("/", "la")
    assertPosition(1, 14)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test incsearch + hlsearch moves caret to start of first match`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")
    typeText("/", "la")
    assertPosition(1, 14)
  }

  @Test
  fun `test incsearch + hlsearch at bottom of file with wrapscan`() {
    // Make sure the caret wraps during incsearch
    configureByText(
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci ${c}mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin()
    )
    enterCommand("set incsearch hlsearch wrapscan")
    typeText("/it")
    assertPosition(0, 19)
  }

  @Test
  fun `test incsearch + hlsearch at bottom of file with nowrapscan`() {
    // This will highlight the occurrences above/before the caret, but should not move the caret
    configureByText(
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci ${c}mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin()
    )
    enterCommand("set incsearch hlsearch nowrapscan")
    typeText("/it")
    assertPosition(2, 12)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test incsearch + hlsearch moves caret to start of first match (backwards)`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")
    typeText("?", "la")
    assertPosition(0, 26)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test incsearch moves caret to start of first match (backwards)`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set incsearch")
    typeText("?", "la")
    assertPosition(0, 26)
  }

  @Test
  fun `test incsearch highlights with count`() {
    configureByText(
      """
        one
        two
        ${c}one
        two
        one
        two
        one
        two
        one
        two
      """.trimIndent()
    )
    enterCommand("set hlsearch incsearch")
    typeText("3", "/", "one") // No enter
    assertSearchHighlights("one",
      """
        «one»
        two
        «one»
        two
        «one»
        two
        «one»
        two
        ‷one‴
        two
      """.trimIndent()
    )
    assertPosition(8, 0)
  }

  @Test
  fun `test incsearch highlights with large count and wrapscan`() {
    configureByText(
      """
        one
        two
        ${c}one
        two
        one
        two
        one
        two
        one
        two
      """.trimIndent()
    )
    enterCommand("set hlsearch incsearch")
    typeText("12", "/", "one") // No enter
    assertSearchHighlights("one",
      """
        «one»
        two
        «one»
        two
        «one»
        two
        ‷one‴
        two
        «one»
        two
      """.trimIndent()
    )
    assertPosition(6, 0)
  }

  @Test
  fun `test incsearch highlights with large count and nowrapscan`() {
    configureByText(
      """
        one
        two
        ${c}one
        two
        one
        two
        one
        two
        one
        two
      """.trimIndent()
    )
    enterCommand("set hlsearch incsearch nowrapscan")
    typeText("12", "/", "one") // No enter

    // No current match highlight
    assertSearchHighlights("one",
      """
        «one»
        two
        «one»
        two
        «one»
        two
        «one»
        two
        «one»
        two
      """.trimIndent()
    )

    // Back to the original location
    assertPosition(2, 0)
  }

  @Test
  fun `test incsearch highlights with count and operator count`() {
    configureByText("lorem 1 ipsum lorem 2 ipsum lorem 3 ipsum lorem 4 ipsum lorem 5 ipsum lorem 6 ipsum lorem 7 ipsum")
    enterCommand("set hlsearch incsearch")
    typeText("2d", "3/ipsum") // No enter
    assertSearchHighlights("ipsum",
      "lorem 1 «ipsum» lorem 2 «ipsum» lorem 3 «ipsum» lorem 4 «ipsum» lorem 5 «ipsum» lorem 6 ‷ipsum‴ lorem 7 «ipsum»")
  }

  @Test
  fun `test backwards incsearch with count`() {
    configureByText(
      """
        one
        two
        one
        two
        one
        two
        one
        two
        ${c}one
        two
      """.trimIndent()
    )
    enterCommand("set hlsearch incsearch")
    typeText("3", "?", "one") // No enter
    assertSearchHighlights("one",
      """
        «one»
        two
        ‷one‴
        two
        «one»
        two
        «one»
        two
        «one»
        two
      """.trimIndent()
    )
    assertPosition(2, 0)
  }

  @Test
  fun `test backwards incsearch highlights with large count and wrapscan`() {
    configureByText(
      """
        one
        two
        one
        two
        one
        two
        one
        two
        ${c}one
        two
      """.trimIndent()
    )
    enterCommand("set hlsearch incsearch")
    typeText("12", "?", "one") // No enter
    assertSearchHighlights("one",
      """
        «one»
        two
        «one»
        two
        ‷one‴
        two
        «one»
        two
        «one»
        two
      """.trimIndent()
    )
    assertPosition(4, 0)
  }

  @Test
  fun `test backwards incsearch highlights with large count and nowrapscan`() {
    configureByText(
      """
        one
        two
        one
        two
        one
        two
        one
        two
        ${c}one
        two
      """.trimIndent()
    )
    enterCommand("set hlsearch incsearch nowrapscan")
    typeText("12", "?", "one") // No enter

    // No current match highlight
    assertSearchHighlights("one",
      """
        «one»
        two
        «one»
        two
        «one»
        two
        «one»
        two
        «one»
        two
      """.trimIndent()
    )

    // Back to the original location
    assertPosition(8, 0)
  }

  @Test
  fun `test backwards incsearch highlights with count and operator count`() {
    configureByText("lorem 1 ipsum lorem 2 ipsum lorem 3 ipsum lorem 4 ipsum lorem 5 ipsum lorem 6 ipsum lorem 7 ipsu${c}m")
    enterCommand("set hlsearch incsearch")
    typeText("2d", "3?ipsum") // No enter
    assertSearchHighlights("ipsum",
      "lorem 1 «ipsum» lorem 2 ‷ipsum‴ lorem 3 «ipsum» lorem 4 «ipsum» lorem 5 «ipsum» lorem 6 «ipsum» lorem 7 «ipsum»")
  }

  @Test
  fun `test incsearch removes all highlights if no match`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText("/", "and")
    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    typeText("zz")

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
  fun `test incsearch does not hide previous search until first character is typed`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")
    typeText("/")
    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    typeText("v")

    assertSearchHighlights(
      "v",
      """I found it in a legendary land
           |all rocks and la‷v‴ender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch does not show previous search highlights when text field is deleted`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")
    typeText("/", "grass", "<BS><BS><BS><BS><BS>")

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
  fun `test cancelling incsearch shows previous search highlights`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")
    typeText("/", "grass", "<Esc>")

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
  fun `test incsearch highlights only current match with nohlsearch`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set incsearch")

    typeText("/", "and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch highlights only current match with nohlsearch (backwards)`() {
    configureByText(
      """I found it in a legendary land
           |all rocks and lave${c}nder and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set incsearch")

    typeText("?", "a")

    assertSearchHighlights(
      "a",
      """I found it in a legendary land
           |all rocks and l‷a‴vender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test incsearch highlights all matches with hlsearch enabled`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText("/", "and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test cancelling incsearch does not show previous search highlights after nohls command`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")
    enterCommand("nohlsearch")
    typeText("/", "grass", "<Esc>")

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
  fun `test incsearch highlight with force sensitive case atom`() {
    configureByText("lorem ipsum Lorem Ipsum lorem ipsum")
    enterCommand("set hlsearch incsearch")

    typeText("/", "\\Clorem")

    assertSearchHighlights("\\Clorem", "«lorem» ipsum Lorem Ipsum ‷lorem‴ ipsum")
  }

  @Test
  fun `test incsearch updates Visual selection`() {
    doTest(
      listOf("ve", "/dolor"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lorem ipsum ${c}d${se}olor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set hlsearch incsearch")
    }
    assertSearchHighlights("dolor",
      """
        |Lorem ipsum ‷dolor‴ sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin()
    )
  }

  @Test
  fun `test incsearch updates empty Visual selection`() {
    doTest(
      "v/ipsum",
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |${s}Lorem ${c}i${se}psum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set hlsearch incsearch")
    }
    assertSearchHighlights("ipsum",
      """
        |Lorem ‷ipsum‴ dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test incsearch updates Visual selection but not highlights with nohlsearch`() {
    // We don't show a highlight when nohlsearch is active and we're in Visual - the selection is enough
    doTest(
      listOf("ve", "/dolor"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lorem ipsum ${c}d${se}olor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set nohlsearch")
      enterCommand("set incsearch")
    }
    assertNoSearchHighlights()
  }

  @Test
  fun `test incsearch updates empty Visual selection but not highlights with nohlsearch`() {
    doTest(
      "v/ipsum",
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lorem ${c}i${se}psum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set nohlsearch")
      enterCommand("set incsearch")
    }
    assertNoSearchHighlights()
  }

  @Test
  fun `test incsearch updates exclusive Visual selection`() {
    doTest(
      listOf("ve", "/dolor"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lorem ipsum ${c}${se}dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set selection=exclusive")
      enterCommand("set hlsearch incsearch")
    }
    assertSearchHighlights("dolor",
      """
        |Lorem ipsum ‷dolor‴ sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin()
    )
  }

  @Test
  fun `test incsearch updates empty exclusive Visual selection`() {
    doTest(
      "v/ipsum",
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |${s}Lorem ${c}${se}ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set selection=exclusive")
      enterCommand("set hlsearch incsearch")
    }
    assertSearchHighlights("ipsum",
      """
        |Lorem ‷ipsum‴ dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test incsearch updates exclusive Visual selection but not highlights with nohlsearch`() {
    doTest(
      listOf("ve", "/dolor"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lorem ipsum ${c}${se}dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set selection=exclusive")
      enterCommand("set nohlsearch")
      enterCommand("set incsearch")
    }
    assertNoSearchHighlights()
  }

  @Test
  fun `test incsearch updates empty exclusive Visual selection but not highlights with nohlsearch`() {
    doTest(
      "v/ipsum",
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lorem ${c}${se}ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set selection=exclusive")
      enterCommand("set nohlseach")
      enterCommand("set incsearch")
    }
    assertNoSearchHighlights()
  }

  @Test
  fun `test incsearch updates selection when editing search pattern`() {
    // This will initially move selection to the "it" in "sit" on the first line, then back to the start of "ipsum".
    // Tests that the selection is correctly updated as the current target changes
    doTest(
      listOf("vl", "/it", "<BS>", "p"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |${s}Lorem ${c}i${se}psum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set hlsearch incsearch")
    }
    assertSearchHighlights("ip",
      """
        |Lorem ‷ip‴sum dolor sit amet,
        |consectetur ad«ip»iscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test incsearch updates empty selection when editing search pattern`() {
    // This will initially move selection to the "it" in "sit" on the first line, then back to the start of "ipsum".
    // Tests that the selection is correctly updated as the current target changes
    doTest(
      listOf("v", "/it", "<BS>", "p"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |${s}Lorem ${c}i${se}psum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set hlsearch incsearch")
    }
    assertSearchHighlights("ip",
      """
        |Lorem ‷ip‴sum dolor sit amet,
        |consectetur ad«ip»iscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test incsearch updates empty selection when editing search pattern with nohlsearch`() {
    // This will initially move selection to the "it" in "sit" on the first line, then back to the start of "ipsum".
    // Tests that the selection is correctly updated as the current target changes
    doTest(
      listOf("v", "/it", "<BS>", "p"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lorem ${c}i${se}psum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set nohlsearch")
      enterCommand("set incsearch")
    }
    assertNoSearchHighlights()
  }

  @Test
  fun `test incsearch updates exclusive selection when editing search pattern`() {
    // This will initially move selection to the "it" in "sit" on the first line, then back to the start of "ipsum".
    // Tests that the selection is correctly updated as the current target changes
    doTest(
      listOf("vl", "/it", "<BS>", "p"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |${s}Lorem ${c}${se}ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set selection=exclusive")
      enterCommand("set hlsearch incsearch")
    }
    assertSearchHighlights("ip",
      """
        |Lorem ‷ip‴sum dolor sit amet,
        |consectetur ad«ip»iscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test incsearch updates empty exclusive selection when editing search pattern`() {
    // This will initially move selection to the "it" in "sit" on the first line, then back to the start of "ipsum".
    // Tests that the selection is correctly updated as the current target changes
    doTest(
      listOf("vl", "/it", "<BS>", "p"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |${s}Lorem ${c}${se}ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set selection=exclusive")
      enterCommand("set hlsearch incsearch")
    }
    assertSearchHighlights("ip",
      """
        |Lorem ‷ip‴sum dolor sit amet,
        |consectetur ad«ip»iscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
  }

  @Test
  fun `test incsearch updates empty exclusive selection when editing search pattern with nohlsearch`() {
    // This will initially move selection to the "it" in "sit" on the first line, then back to the start of "ipsum".
    // Tests that the selection is correctly updated as the current target changes
    doTest(
      listOf("v", "/it", "<BS>", "p"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lorem ${c}${se}ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    ) {
      enterCommand("set selection=exclusive")
      enterCommand("set nohlsearch")
      enterCommand("set incsearch")
    }
    assertNoSearchHighlights()
  }

  @Test
  fun `test incsearch updates block selection when started in Visual mode`() {
    doTest(
      listOf("ll", "<C-V>2j", "/mauris"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |Lo${s}rem ipsum ${c}d${se}olor sit amet,
        |co${s}nsectetur ${c}a${se}dipiscing elit
        |Se${s}d in orci ${c}m${se}auris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.VISUAL(SelectionType.BLOCK_WISE))
    ) {
      enterCommand("set hlsearch incsearch")
    }
  }

  @Test
  fun `test incsearch removes Visual when searching with no Visual range`() {
    doTest(
      listOf("v", ":<C-U>%s/dolor"),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |Lorem ipsum ${c}dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.CMD_LINE(Mode.NORMAL())
    ) {
      enterCommand("set hlsearch incsearch")
    }
    assertSearchHighlights("dolor",
      """
        |Lorem ipsum ‷dolor‴ sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin()
    )
  }
}
