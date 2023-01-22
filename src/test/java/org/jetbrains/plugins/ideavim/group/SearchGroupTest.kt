/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group

import com.intellij.idea.TestFor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.helper.RunnableHelper
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class SearchGroupTest : VimTestCase() {
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

  fun `test end of line`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin()
    )
    enterSearch("$")
    assertOffset(29)
  }

  // VIM-146
  fun `test end of line with highlighting`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin()
    )
    enterCommand("set hlsearch")
    enterSearch("$")
    assertOffset(29)
  }

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
  fun `test 'and' without branches with highlighting`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin()
    )
    enterCommand("set hlsearch")
    enterSearch("\\&")
    assertOffset(1)
  }

  // VIM-528
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
  fun `test grouping`() {
    configureByText(
      """
        |${c}I found it in a legendary land
        |all rocks and lavender and tufted grass,
      """.trimMargin()
    )
    enterSearch("\\(found\\)")
    assertOffset(2)
  }

  // VIM-855
  fun `test character class regression`() {
    configureByText("${c}bb\n")
    enterSearch("[^c]b")
    assertOffset(0)
  }

  // VIM-855
  fun `test character class regression case insensitive`() {
    val pos = search(
      "\\c[ABC]b",
      "${c}dd\n",
    )
    assertEquals(-1, pos)
  }

  // VIM-856
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun `test negative lookbehind regression`() {
    val pos = search(
      "a\\@<!b",
      "${c}ab\n"
    )
    assertEquals(-1, pos)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun `test smart case search case insensitive`() {
    configureByText("obj.toString();\n")
    enterCommand("set ignorecase smartcase")
    enterSearch("tostring")
    assertOffset(4)
  }

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

  fun `test search motion`() {
    configureByText("${c}one two\n")
    enterSearch("two")
    assertOffset(4)
  }

  // |/pattern/e|
  fun `test search e motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/e")
    assertOffset(6)
  }

  fun `test search e-1 motion offset`() {
    doTest(
      "/two/e-1<Enter>",
      "${c}one two three",
      "one t${c}wo three",
    )
  }

  // |/pattern/e|
  fun `test search e+2 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/e+2")
    assertOffset(8)
  }

  fun `test reverse search e+2 motion offset finds next match when starting on matching offset`() {
    configureByText("one two three one two ${c}three")
    enterSearch("two?e+2", false)
    assertOffset(8)
  }

  fun `test search e+10 motion offset at end of file`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterSearch("in/e+10")
    assertPosition(3, 38)
  }

  fun `test search e+10 motion offset wraps at end of file`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterSearch("in/e+10")
    typeText("n")
    // "in" at (0, 11) plus 10 offset from end
    assertOffset(22)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test search e+10 motion offset wraps at exactly end of file`() {
    configureByText(
      """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |${c}hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterSearch("ass./e+10")
    typeText("n")
    // "ass," at (1, 36) plus 10 offset from end
    assertPosition(2, 8)
  }

  // |/pattern/s|
  fun `test search s motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/s")
    assertOffset(4)
  }

  // |/pattern/s|
  fun `test search s-2 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/s-2")
    assertOffset(2)
  }

  fun `test search s-2 motion offset finds next match when starting on matching offset`() {
    configureByText("on${c}e two three one two three")
    enterSearch("two/s-2")
    assertOffset(16)
  }

  fun `test reverse search s-20 motion offset at beginning of file`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterSearch("it?s-20", false)
    assertOffset(0)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test reverse search s-20 motion offset wraps at beginning of file`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterSearch("it?s-20", false)
    typeText("N")
    // "it" at (2,5) minus 20 characters
    assertPosition(1, 27)
  }

  // |/pattern/s|
  fun `test search s+1 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/s+1")
    assertOffset(5)
  }

  fun `test reverse search s+2 motion offset finds next match when starting at matching offset`() {
    configureByText("one two three one tw${c}o three")
    enterSearch("two?s+2", false)
    assertOffset(6)
  }

  // |/pattern/b|
  fun `test search b motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/b")
    assertOffset(4)
  }

  // |/pattern/b|
  fun `test search b-2 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/b-2")
    assertOffset(2)
  }

  // |/pattern/b|
  fun `test search b+1 motion offset`() {
    configureByText("${c}one two three")
    enterSearch("two/b+1")
    assertOffset(5)
  }

  fun `test search above line motion offset`() {
    configureByText(
      """
        |I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
     """.trimMargin()
    )
    enterSearch("rocks/-1")
    assertOffset(0)
  }

  fun `test search below line motion offset`() {
    configureByText(
      """
        |I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin()
    )
    enterSearch("rocks/+2")
    assertOffset(113)
  }

  // |i_CTRL-K|
  fun `test search digraph`() {
    configureByText("${c}Hallo Österreich!\n")
    // enterSearch doesn't parse the special keys
    typeText("/", "<C-K>O:", "<Enter>")
    assertOffset(6)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  fun `test search word matches case`() {
    configureByText("${c}Editor editor Editor")
    typeText("*")
    assertOffset(14)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  fun `test search next word matches case`() {
    configureByText("${c}Editor editor Editor editor Editor")
    typeText("*", "n")
    assertOffset(28)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun `test search word honours ignorecase`() {
    configureByText("${c}editor Editor editor")
    enterCommand("set ignorecase")
    typeText("*")
    assertOffset(7)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun `test search next word honours ignorecase`() {
    configureByText("${c}editor Editor editor")
    enterCommand("set ignorecase")
    typeText("*", "n")
    assertOffset(14)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @TestFor(classes = [SearchWholeWordForwardAction::class])
  fun `test search word overrides smartcase`() {
    configureByText("${c}Editor editor Editor")
    enterCommand("set ignorecase smartcase")
    typeText("*")
    assertOffset(7)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun `test search next word overrides smartcase`() {
    configureByText("${c}Editor editor editor")
    enterCommand("set ignorecase smartcase")
    typeText("*", "n")
    assertOffset(14)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test incsearch moves caret to start of first match`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/", "la")
    assertPosition(1, 14)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test incsearch + hlsearch moves caret to start of first match`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")
    typeText("/", "la")
    assertPosition(1, 14)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test incsearch moves caret to start of first match (backwards)`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set incsearch")
    typeText("?", "la")
    assertPosition(0, 26)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test incsearch + hlsearch moves caret to start of first match (backwards)`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")
    typeText("?", "la")
    assertPosition(0, 26)
  }

  fun `test incsearch resets caret if no match found`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/", "lazzz")
    assertPosition(1, 0)
  }

  fun `test incsearch + hlsearch resets caret if no match found`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")
    typeText("/", "lazzz")
    assertPosition(1, 0)
  }

  fun `test incsearch resets caret if cancelled`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/", "la", "<Esc>")
    assertPosition(1, 0)
  }

  fun `test incsearch + hlsearch resets caret if cancelled`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")
    typeText("/", "la", "<Esc>")
    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test incsearch resets caret on backspace`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set incsearch")
    typeText("/", "wh")
    assertPosition(2, 0)
    typeText("<BS><BS>")
    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test incsearch + hlsearch resets caret on backspace`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")
    typeText("/", "wh")
    assertPosition(2, 0)
    typeText("<BS><BS>")
    assertPosition(1, 0)
  }

  fun `test search result position with incsearch`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set incsearch")
    enterSearch("and")
    assertPosition(1, 10)
  }

  fun `test search result position with incsearch + hlsearch`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")
    enterSearch("and")
    assertPosition(1, 10)
  }

  fun `test incsearch highlights only current match with nohlsearch`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set incsearch")

    typeText("/", "and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch highlights only current match with nohlsearch (backwards)`() {
    configureByText(
      """I found it in a legendary land
           |all rocks and lave${c}nder and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set incsearch")

    typeText("?", "a")

    assertSearchHighlights(
      "a",
      """I found it in a legendary land
           |all rocks and l‷a‴vender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch highlights all matches with hlsearch enabled`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    typeText("/", "and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch removes all highlights if no match`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    typeText("/", "and")
    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    typeText("zz")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch does not hide previous search until first character is typed`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")
    typeText("/")
    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    typeText("v")

    assertSearchHighlights(
      "v",
      """I found it in a legendary land
           |all rocks and la‷v‴ender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch does not show previous search highlights when text field is deleted`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")
    typeText("/", "grass", "<BS><BS><BS><BS><BS>")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test cancelling incsearch shows previous search highlights`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")
    typeText("/", "grass", "<Esc>")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test cancelling incsearch does not show previous search highlights after nohls command`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch highlights for substitute command`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "%s/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l‷and‴
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch only highlights for substitute command after valid argument`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch highlights for substitute command only highlights in range`() {
    configureByText(
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |${c}hard by the torrent and rush of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "2,3s/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent and rush of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch highlights for substitute command in current line with no range`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "s/and")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

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
           |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch highlights for substitute command clears highlights on backspace`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    typeText(":", "%s/and", "<BS><BS><BS>")

    assertSearchHighlights(
      "and",
      """I found it in a legendary land
             |all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test incsearch highlights for substitute command resets highlights on backspace`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")
    typeText(":", "%s/roc", "<BS><BS><BS>")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )

    // TODO: Check caret position
  }

  fun `test cancelling incsearch highlights for substitute command shows previous highlights`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch incsearch")

    enterSearch("and")
    typeText(":", "%s/ass", "<Esc>")

    assertSearchHighlights(
      "and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )

    // TODO: Check caret position
  }

  fun `test highlight search results`() {
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch")

    val pattern = "and"
    enterSearch(pattern)

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test search removes previous search highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test no highlights for unmatched search`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch")
    enterSearch("zzzz")
    assertNoSearchHighlights()
  }

  fun `test nohlsearch command removes highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch")
    enterSearch("and")
    enterCommand("nohlsearch")
    assertNoSearchHighlights()
  }

  fun `test find next after nohlsearch command shows highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test nohlsearch correctly resets incsearch highlights after deleting last occurrence`() {
    // Crazy edge case bug. With incsearch enabled, search for something with only one occurrence, delete it, call
    // :nohlsearch, undo and search next - highlights don't work anymore
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
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
        |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test nohlsearch option hides search highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch")
    enterSearch("and")
    enterCommand("set nohlsearch")
    assertNoSearchHighlights()
  }

  fun `test setting hlsearch option shows search highlights for last search`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
    )

    val pattern = "and"
    enterSearch(pattern)
    enterCommand("set hlsearch")

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test deleting text moves search highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test deleting match removes search highlight`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test deleting part of match removes search highlight`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test deleting part of match keeps highlight if pattern still matches`() {
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test inserting text moves search highlights`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test inserting text inside match removes search highlight`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test inserting text inside match keeps highlight if pattern still matches`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test inserting text shows highlight if it contains matches`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test replacing text moves search highlights`() {
    val pattern = "and"
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
    enterCommand("set hlsearch")

    enterSearch(pattern)
    typeText("b", "cw", "boulders", "<Esc>") // Replaces "rocks" with "boulders" on line 2

    assertSearchHighlights(
      pattern,
      """I found it in a legendary l«and»
           |all boulders «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test replacing text inside match removes search highlight`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin()
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
         |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test replacing text shows highlight if it contains matches`() {
    configureByText(
      """I found it in a legendary land
          |${c}all rocks and lavender and tufted grass,
          |where it was settled on some sodden sand
          |hard by the torrent of a mountain pass.""".trimMargin()
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
          |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test replacing text inside match keeps highlight if pattern still matches`() {
    configureByText(
      """I found it in a legendary land
            |${c}all rocks and lavender and tufted grass,
            |where it was settled on some sodden sand
            |hard by the torrent of a mountain pass.""".trimMargin()
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
            |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  fun `test search highlight with tabs`() {
    configureByText("\tfoo")
    enterCommand("set hlsearch")
    val pattern = "foo"
    enterSearch(pattern)
    assertSearchHighlights(pattern, "\t«foo»")
  }

  // Ensure that the offsets for the last carriage return in the file are valid, even though it's for a line that
  // doesn't exist
  fun `test find last cr in file`() {
    val res = search("\\n", "Something\n")
    assertEquals(9, res)
  }

  private fun search(pattern: String, input: String): Int {
    configureByText(input)
    val editor = myFixture.editor
    val project = myFixture.project
    val searchGroup = VimPlugin.getSearch()
    val ref = Ref.create(-1)
    RunnableHelper.runReadCommand(
      project,
      {
        // Does not move the caret!
        val n = searchGroup.processSearchCommand(editor.vim, pattern, myFixture.caretOffset, Direction.FORWARDS)
        ref.set(n)
      },
      null, null
    )
    return ref.get()
  }

  private fun assertNoSearchHighlights() {
    assertEquals(0, myFixture.editor.markupModel.allHighlighters.size)
  }

  @Suppress("DEPRECATION")
  private fun assertSearchHighlights(tooltip: String, expected: String) {
    val allHighlighters = myFixture.editor.markupModel.allHighlighters

    LOG.debug("Current text: ${myFixture.editor.document.text}")
    val actual = StringBuilder(myFixture.editor.document.text)
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
      assertEquals("Incorrect tooltip for highlighter at $offsets", tooltip, it.errorStripeTooltip)
      assertEquals(
        "Incorrect background colour for highlighter at $offsets",
        attributes.backgroundColor,
        it.textAttributes?.backgroundColor
      )
      assertEquals(
        "Incorrect foreground colour for highlighter at $offsets",
        attributes.foregroundColor,
        it.textAttributes?.foregroundColor
      )
      // TODO: Find a better way to identify the current match
      if (it.textAttributes?.effectType == EffectType.ROUNDED_BOX) {
        assertEquals(
          "Incorrect effect type for highlighter at $offsets",
          EffectType.ROUNDED_BOX,
          it.textAttributes?.effectType
        )
        assertEquals("Incorrect effect colour for highlighter at $offsets", caretColour, it.textAttributes?.effectColor)
      } else {
        assertEquals(
          "Incorrect effect type for highlighter at $offsets",
          attributes.effectType,
          it.textAttributes?.effectType
        )
        assertEquals(
          "Incorrect effect colour for highlighter at $offsets",
          attributes.effectColor,
          it.textAttributes?.effectColor
        )
      }
    }
  }
}
