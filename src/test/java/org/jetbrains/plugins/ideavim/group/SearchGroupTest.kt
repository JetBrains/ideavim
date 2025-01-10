/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group

import com.intellij.idea.TestFor
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.helper.RunnableHelper
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * @author Alex Plate
 */
@Suppress("SpellCheckingInspection")
class SearchGroupTest : VimTestCase() {
  @Test
  fun `test one letter`() {
    configureByText(
      """
        |${c}one
        |two
      """.trimMargin(),
    )
    enterSearch("w")
    assertOffset(5)
  }

  @Test
  fun `test end of line`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
    enterSearch("$")
    assertOffset(29)
  }

  // VIM-146
  @Test
  fun `test end of line with highlighting`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
    enterCommand("set hlsearch")
    enterSearch("$")
    assertOffset(29)
  }

  @Test
  fun `test 'and' without branches`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
    enterSearch("\\&")
    assertOffset(1)
  }

  // VIM-226
  @Test
  fun `test 'and' without branches with highlighting`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
    enterCommand("set hlsearch")
    enterSearch("\\&")
    assertOffset(1)
  }

  // VIM-528
  @Test
  fun `test not found`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
    enterSearch("(found)")
    assertOffset(0) // Caret doesn't move
    assertPluginErrorMessageContains("Pattern not found: (found)")
  }

  // VIM-528
  @Test
  fun `test grouping`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
    enterSearch("\\(found\\)")
    assertOffset(2)
  }

  // VIM-855
  @Test
  fun `test character class regression`() {
    configureByText("${c}bb\n")
    enterSearch("[^c]b")
    assertOffset(0)
  }

  // VIM-855
  @Test
  fun `test character class regression case insensitive`() {
    val pos = search(
      "\\c[ABC]b",
      "${c}dd\n",
    )
    assertEquals(-1, pos)
  }

  // VIM-856
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test negative lookbehind regression`() {
    val pos = search(
      "a\\@<!b",
      "${c}ab\n",
    )
    assertEquals(-1, pos)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test smart case search case insensitive`() {
    configureByText("obj.toString();\n")
    enterCommand("set ignorecase smartcase")
    enterSearch("tostring")
    assertOffset(4)
  }

  @Test
  fun `test smart case search case sensitive`() {
    configureByText(
      """
        |obj.tostring();
        |obj.toString();
      """.trimMargin(),
    )
    enterCommand("set ignorecase smartcase")
    enterSearch("toString")
    assertOffset(20)
  }

  @Test
  fun `test search motion`() {
    configureByText("${c}one two\n")
    enterSearch("two")
    assertOffset(4)
  }

  @Test
  fun `test search motion for text at caret moves to next occurrence`() {
    doTest(
      searchCommand("/one"),
      "${c}one one one",
      "one ${c}one one"
    )
  }

  @Test
  fun `test search with wrapscan`() {
    doTest(
      searchCommand("/one"),
      """
        one
        two
        ${c}three
      """.trimIndent(),
      """
        ${c}one
        two
        three
      """.trimIndent()
    )
    assertPluginError(false)
    assertPluginErrorMessageContains("search hit BOTTOM, continuing at TOP")
  }

  @Test
  fun `test search with nowrapscan`() {
    doTest(
      searchCommand("/one"),
      """
        one
        ${c}two
        three
      """.trimIndent(),
      """
        one
        ${c}two
        three
      """.trimIndent()
    ) {
      enterCommand("set nowrapscan")
    }
    assertPluginError(false)
    assertPluginErrorMessageContains("E385: Search hit BOTTOM without match for: one")
  }

  @Test
  fun `test search with wrapscan and no matches`() {
    doTest(
      searchCommand("/banana"),
      """
        one
        ${c}two
        three
      """.trimIndent(),
      """
        one
        ${c}two
        three
      """.trimIndent()
    )
    assertPluginError(false)
    assertPluginErrorMessageContains("E486: Pattern not found: banana")
  }

  @Test
  fun `test backwards search with wrapscan`() {
    doTest(
      searchCommand("?three"),
      """
        one
        ${c}two
        three
      """.trimIndent(),
      """
        one
        two
        ${c}three
      """.trimIndent()
    )
    assertPluginError(false)
    assertPluginErrorMessageContains("search hit TOP, continuing at BOTTOM")
  }

  @Test
  fun `test backwards search with nowrapscan`() {
    doTest(
      searchCommand("?three"),
      """
        one
        ${c}two
        three
      """.trimIndent(),
      """
        one
        ${c}two
        three
      """.trimIndent()
    ) {
      enterCommand("set nowrapscan")
    }
    assertPluginError(false)
    assertPluginErrorMessageContains("E384: Search hit TOP without match for: three")
  }

  @Test
  fun `test backwards search with wrapscan and no matches`() {
    doTest(
      searchCommand("?banana"),
      """
        one
        ${c}two
        three
      """.trimIndent(),
      """
        one
        ${c}two
        three
      """.trimIndent()
    )
    assertPluginError(false)
    assertPluginErrorMessageContains("E486: Pattern not found: banana")
  }

  @Test
  fun `test search for last pattern`() {
    doTest(
      listOf(searchCommand("/one"), searchCommand("//")),
      "${c}one one one",
      "one one ${c}one"
    )
  }

  // |/pattern/e|
  @Test
  fun `test search e motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/e")
    assertOffset(6)
  }

  @Test
  fun `test search e motion offset for text at caret`() {
    doTest(
      searchCommand("/one/e"),
      "${c}one one one",
      "on${c}e one one"
    )
  }

  @Test
  fun `test search for last pattern with e motion offset`() {
    doTest(
      listOf(searchCommand("/one"), searchCommand("//e")),
      "${c}one one one",
      "one on${c}e one"
    )
  }

  @Test
  fun `test search for last pattern with e motion offset 2`() {
    doTest(
      listOf(searchCommand("/one/e"), searchCommand("//e")),
      "${c}one one one",
      "one on${c}e one"
    )
  }

  @Test
  fun `test search e-1 motion offset`() {
    doTest(
      "/two/e-1<Enter>",
      "${c}one two three",
      "one t${c}wo three",
    )
  }

  // |/pattern/e|
  @Test
  fun `test search e+2 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/e+2")
    assertOffset(8)
  }

  @Test
  fun `test reverse search e+2 motion offset finds next match when starting on matching offset`() {
    configureByText("one two three one two ${c}three")
    enterSearch("two?e+2", false)
    assertOffset(8)
  }

  @Test
  fun `test search e+10 motion offset at end of file`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterSearch("in/e+10")
    assertPosition(3, 38)
  }

  @Test
  fun `test search e+10 motion offset wraps at end of file`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterSearch("in/e+10")
    typeText("n")
    // "in" at (0, 11) plus 10 offset from end
    assertOffset(22)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test search e+10 motion offset wraps at exactly end of file`() {
    configureByText(
      """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |${c}hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterSearch("ass./e+10")
    typeText("n")
    // "ass," at (1, 36) plus 10 offset from end
    assertPosition(2, 8)
  }

  // |/pattern/s|
  @Test
  fun `test search s motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/s")
    assertOffset(4)
  }

  // |/pattern/s|
  @Test
  fun `test search s-2 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/s-2")
    assertOffset(2)
  }

  @Test
  fun `test search s-2 motion offset finds next match when starting on matching offset`() {
    configureByText("on${c}e two three one two three")
    enterSearch("two/s-2")
    assertOffset(16)
  }

  @Test
  fun `test reverse search s-20 motion offset at beginning of file`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterSearch("it?s-20", false)
    assertOffset(0)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test reverse search s-20 motion offset wraps at beginning of file`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterSearch("it?s-20", false)
    typeText("N")
    // "it" at (2,5) minus 20 characters
    assertPosition(1, 27)
  }

  // |/pattern/s|
  @Test
  fun `test search s+1 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/s+1")
    assertOffset(5)
  }

  @Test
  fun `test reverse search s+2 motion offset finds next match when starting at matching offset`() {
    configureByText("one two three one tw${c}o three")
    enterSearch("two?s+2", false)
    assertOffset(6)
  }

  // |/pattern/b|
  @Test
  fun `test search b motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/b")
    assertOffset(4)
  }

  // |/pattern/b|
  @Test
  fun `test search b-2 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/b-2")
    assertOffset(2)
  }

  // |/pattern/b|
  @Test
  fun `test search b+1 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/b+1")
    assertOffset(5)
  }

  @Test
  fun `test search above line motion offset`() {
    configureByText(
      """
        |I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterSearch("rocks/-1")
    assertOffset(0)
  }

  @Test
  fun `test search below line motion offset`() {
    configureByText(
      """
        |I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterSearch("rocks/+2")
    assertOffset(113)
  }

  @Test
  fun `test search with count`() {
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
    typeText("3", "/one<CR>")
    assertPosition(8, 0)
  }

  @Test
  fun `test search with 0 count`() {
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
      """.trimIndent()
    )
    typeText("0", "/", searchCommand("one"))  // Same as 1
    assertPosition(4, 0)
  }

  @Test
  fun `test search with large count and wrapscan`() {
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
      """.trimIndent()
    )
    assertTrue(options().wrapscan)
    typeText("10", "/", searchCommand("one"))
    assertPosition(6, 0)
  }

  @Test
  fun `test search with large count and nowrapscan`() {
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
      """.trimIndent()
    )
    enterCommand("set nowrapscan")
    typeText("10", "/", searchCommand("one"))
    assertPluginError(false)
    assertPluginErrorMessageContains("E385: Search hit BOTTOM without match for: one")
    assertPosition(2, 0)
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
    assertSearchHighlights(
      "one",
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
    assertSearchHighlights(
      "one",
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
    assertSearchHighlights(
      "one",
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

    // Back to original location
    assertPosition(2, 0)
  }

  @Test
  fun `test incsearch highlights with count and operator count`() {
    configureByText("lorem 1 ipsum lorem 2 ipsum lorem 3 ipsum lorem 4 ipsum lorem 5 ipsum lorem 6 ipsum lorem 7 ipsum")
    enterCommand("set hlsearch incsearch")
    typeText("2d", "3/ipsum") // No enter
    assertSearchHighlights(
      "ipsum",
      "lorem 1 «ipsum» lorem 2 «ipsum» lorem 3 «ipsum» lorem 4 «ipsum» lorem 5 «ipsum» lorem 6 ‷ipsum‴ lorem 7 «ipsum»"
    )
  }

  @Test
  fun `test backwards search with count`() {
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
    typeText("3", "?one<CR>")
    assertPosition(2, 0)
  }

  @Test
  fun `test backwards search with large count and wrapscan`() {
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
    enterCommand("set wrapscan")
    typeText("12", "?one<CR>")
    assertPosition(4, 0)
  }

  @Test
  fun `test backwards search with large count and nowrapscan`() {
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
    enterCommand("set nowrapscan")
    typeText("12", "?one<CR>")
    assertPluginError(false)
    assertPluginErrorMessageContains("E384: Search hit TOP without match for: one")
    assertPosition(8, 0)
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
    assertSearchHighlights(
      "one",
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
    assertSearchHighlights(
      "one",
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
    assertSearchHighlights(
      "one",
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

    // Back to original location
    assertPosition(8, 0)
  }

  @Test
  fun `test backwards incsearch highlights with count and operator count`() {
    configureByText("lorem 1 ipsum lorem 2 ipsum lorem 3 ipsum lorem 4 ipsum lorem 5 ipsum lorem 6 ipsum lorem 7 ipsu${c}m")
    enterCommand("set hlsearch incsearch")
    typeText("2d", "3?ipsum") // No enter
    assertSearchHighlights(
      "ipsum",
      "lorem 1 «ipsum» lorem 2 ‷ipsum‴ lorem 3 «ipsum» lorem 4 «ipsum» lorem 5 «ipsum» lorem 6 «ipsum» lorem 7 «ipsum»"
    )
  }

  // |i_CTRL-K|
  @Test
  fun `test search digraph`() {
    configureByText("${c}Hallo Österreich!\n")
    // enterSearch doesn't parse the special keys
    typeText("/", "<C-K>O:", "<Enter>")
    assertOffset(6)
  }

  @Test
  fun `test search beginning of file atom`() {
    configureByText(
      """
      one
      ${c}two
      one
      two
    """.trimIndent()
    )
    enterSearch("""\%^one""")
    assertPosition(0, 0)
  }

  @Test
  fun `test search beginning of file atom with old regex engine`() {
    configureByText(
      """
      one
      ${c}two
      one
      two
    """.trimIndent()
    )
    enterCommand("set nousenewregex")
    enterSearch("""\%^""")
    assertPosition(0, 0)
  }

  @Test
  fun `test search end of file atom`() {
    configureByText(
      """
      one
      two
      one
      two
    """.trimIndent()
    )
    enterSearch("""two\%$""")
    assertPosition(3, 0)
  }

  @Test
  fun `test search end of file atom with old regex engine`() {
    configureByText(
      """
      one
      two
      one
      two
    """.trimIndent()
    )
    enterCommand("set nousenewregex")
    enterSearch("""two\%$""")
    assertPosition(3, 0)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun `test search word matches case`() {
    configureByText("${c}Editor editor Editor")
    typeText("*")
    assertOffset(14)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun `test search next word matches case`() {
    configureByText("${c}Editor editor Editor editor Editor")
    typeText("*", "n")
    assertOffset(28)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test search word honours ignorecase`() {
    configureByText("${c}editor Editor editor")
    enterCommand("set ignorecase")
    typeText("*")
    assertOffset(7)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test search next word honours ignorecase`() {
    configureByText("${c}editor Editor editor")
    enterCommand("set ignorecase")
    typeText("*", "n")
    assertOffset(14)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun `test search word overrides smartcase`() {
    configureByText("${c}Editor editor Editor")
    enterCommand("set ignorecase smartcase")
    typeText("*")
    assertOffset(7)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test search next word overrides smartcase`() {
    configureByText("${c}Editor editor editor")
    enterCommand("set ignorecase smartcase")
    typeText("*", "n")
    assertOffset(14)
  }

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

  @Test
  fun `test incsearch resets caret if no match found`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set incsearch")
    typeText("/", "lazzz")
    assertPosition(1, 0)
  }

  @Test
  fun `test incsearch + hlsearch resets caret if no match found`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")
    typeText("/", "lazzz")
    assertPosition(1, 0)
  }

  @Test
  fun `test incsearch resets caret if cancelled`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set incsearch")
    typeText("/", "la", "<Esc>")
    assertPosition(1, 0)
  }

  @Test
  fun `test incsearch + hlsearch resets caret if cancelled`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")
    typeText("/", "la", "<Esc>")
    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test incsearch resets caret on backspace`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set incsearch")
    typeText("/", "wh")
    assertPosition(2, 0)
    typeText("<BS><BS>")
    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test incsearch + hlsearch resets caret on backspace`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")
    typeText("/", "wh")
    assertPosition(2, 0)
    typeText("<BS><BS>")
    assertPosition(1, 0)
  }

  @Test
  fun `test search result position with incsearch`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set incsearch")
    enterSearch("and")
    assertPosition(1, 10)
  }

  @Test
  fun `test search result position with incsearch + hlsearch`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")
    enterSearch("and")
    assertPosition(1, 10)
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

    // E.g. don't remove highlights when trying to type :set
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

    enterSearch("and")  // Moves the care to "and" on second line: (1, 10)
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

  // global
  @Test
  fun `test incsearch highlights for global command with range`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "%g/and")

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
  fun `test incsearch highlights for global command in whole file with default range`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "g/and")

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
  fun `test incsearch highlights for global-bang command`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "%g!/and")

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
  fun `test incsearch only highlights for global command after valid argument`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    // E.g. don't remove highlights when trying to type :goto
    enterSearch("and")
    typeText(":g")

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
  fun `test incsearch highlights for global command only highlights in range`() {
    configureByText(
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |${c}hard by the torrent and rush of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "2,3g/and")

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
  fun `test incsearch for global command starts at beginning of range not caret position`() {
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

    typeText(":", "2,8g/and")

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
  fun `test incsearch highlights for global command clears highlights on backspace`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "g/and", "<BS><BS><BS>")

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
  fun `test incsearch highlights for global command resets highlights on backspace`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")  // Moves the caret to "and" on the second line: (1, 10)
    typeText(":", "g/roc", "<BS><BS><BS>")

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
  fun `test cancelling incsearch highlights for global command shows previous highlights`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")  // Moves the caret to "and" on the second line (1, 10)
    typeText(":", "g/ass", "<Esc>")

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

  // vglobal
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

    // E.g. don't remove highlights when trying to type :vmap
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


  @Test
  fun `test incsearch updates selection when started in Visual mode`() {
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
      enterCommand("set hlsearch incsearch")
    }
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
  fun `test cancelling search restores Visual (characterwise)`() {
    doTest(
      listOf("v", "e", "/amet", "<Esc>"),
      """
        |${c}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lore${c}m${se} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE)
    )
  }

  @Test
  fun `test cancelling search restores Visual (linewise)`() {
    doTest(
      listOf("V", "2j", "/mauris", "<Esc>"),
      """
        |${c}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |${s}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |${c}Sed in orci mauris.
        |${se}Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE)
    )
  }

  @Test
  fun `test cancelling search restores Visual (blockwise)`() {
    doTest(
      listOf("<C-V>", "2j", "/mauris", "<Esc>"),
      """
        |Lo${c}rem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      """
        |Lo${s}${c}r${se}em ipsum dolor sit amet,
        |co${s}${c}n${se}sectetur adipiscing elit
        |Se${s}${c}d${se} in orci mauris.
        |Cras id tellus in ex imperdiet egestas. 
      """.trimMargin(),
      Mode.VISUAL(SelectionType.BLOCK_WISE)
    )
  }

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

  // Ensure that the offsets for the last carriage return in the file are valid, even though it's for a line that
  // doesn't exist
  @Test
  fun `test find last cr in file`() {
    val res = search("\\n", "Something\n")
    assertEquals(9, res)
  }

  // VIM-2510
  @Test
  fun `test backslash-i does not hang while trying to highlight all instances with old regex engine`() {
    // \i - identifier character class.
    // The old regex engine would treat '\0' as a valid identifier, which is unfortunate, because that marks the end of
    // each search input line. There is also no validation on the column, so it would continuously match the end of line
    // marker and increment the column even though there was no column.
    // We should also be using 'isident' instead of Character.isJavaIdentifierPart, but let's not worry about the old
    // engine now.
    doTest(
      searchCommand("/\\i"),
      """lorem ipsum""",
      """l${c}orem ipsum"""
    ) {
      enterCommand("set hlsearch nousenewregex")
    }
  }

  private fun search(pattern: String, input: String): Int {
    configureByText(input)
    val editor = fixture.editor
    val project = fixture.project
    val searchGroup = VimPlugin.getSearch()
    val ref = Ref.create(-1)
    RunnableHelper.runReadCommand(
      project,
      {
        // Does not move the caret!
        val n = searchGroup.processSearchCommand(editor.vim, pattern, fixture.caretOffset, 1, Direction.FORWARDS)
        ref.set(n?.first ?: -1)
      },
      null,
      null,
    )
    return ref.get()
  }

  private fun assertNoSearchHighlights() {
    assertEquals(0, fixture.editor.markupModel.allHighlighters.size)
  }

  @Suppress("DEPRECATION")
  private fun assertSearchHighlights(tooltip: String, expected: String) {
    val allHighlighters = fixture.editor.markupModel.allHighlighters

    thisLogger().debug("Current text: ${fixture.editor.document.text}")
    val actual = StringBuilder(fixture.editor.document.text)
    val inserts = mutableMapOf<Int, String>()

    // Digraphs:
    // <C-K>3" → ‷ + <C-K>3' → ‴ (current match)
    // <C-K><< → « + <C-K>>> → » (normal match)
    allHighlighters.forEach {
      // TODO: This is not the nicest way to check for current match. Add something to the highlight's user data?
      if (it.textAttributes?.effectType == EffectType.ROUNDED_BOX) {
        inserts.compute(it.startOffset) { _, v -> if (v == null) "‷" else "$v‷" }
        inserts.compute(it.endOffset) { _, v -> if (v == null) "‴" else "$v‴" }
      } else {
        inserts.compute(it.startOffset) { _, v -> if (v == null) "«" else "$v«" }
        inserts.compute(it.endOffset) { _, v -> if (v == null) "»" else "$v»" }
      }
    }

    var offset = 0
    inserts.toSortedMap().forEach { (k, v) ->
      actual.insert(k + offset, v)
      offset += v.length
    }

    assertEquals(expected, actual.toString())

    // Assert all highlighters have the correct tooltip and text attributes
    val editorColorsScheme = EditorColorsManager.getInstance().globalScheme
    val attributes = editorColorsScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)
    val caretColour = editorColorsScheme.getColor(EditorColors.CARET_COLOR)
    allHighlighters.forEach {
      val offsets = "(${it.startOffset}, ${it.endOffset})"
      assertEquals(tooltip, it.errorStripeTooltip, "Incorrect tooltip for highlighter at $offsets")
      assertEquals(
        attributes.backgroundColor,
        it.textAttributes?.backgroundColor,
        "Incorrect background colour for highlighter at $offsets",
      )
      assertEquals(
        attributes.foregroundColor,
        it.textAttributes?.foregroundColor,
        "Incorrect foreground colour for highlighter at $offsets",
      )
      // TODO: Find a better way to identify the current match
      if (it.textAttributes?.effectType == EffectType.ROUNDED_BOX) {
        assertEquals(
          EffectType.ROUNDED_BOX,
          it.textAttributes?.effectType,
          "Incorrect effect type for highlighter at $offsets",
        )
        assertEquals(caretColour, it.textAttributes?.effectColor, "Incorrect effect colour for highlighter at $offsets")
      } else {
        assertEquals(
          attributes.effectType,
          it.textAttributes?.effectType,
          "Incorrect effect type for highlighter at $offsets",
        )
        assertEquals(
          attributes.effectColor,
          it.textAttributes?.effectColor,
          "Incorrect effect colour for highlighter at $offsets",
        )
      }
    }
  }
}
