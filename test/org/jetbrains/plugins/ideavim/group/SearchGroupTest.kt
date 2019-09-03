/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.group

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.helper.RunnableHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimTestFunction
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase
import java.util.*

/**
 * @author Alex Plate
 */
class SearchGroupTest : VimTestCase() {
  fun `test one letter`() {
    val pos = search("w",
      """${c}one
                  |two
               """.trimMargin())
    assertEquals(5, pos)
  }

  fun `test end of line`() {
    val pos = search("$",
      """${c}I found it in a legendary land
                  |all rocks and lavender and tufted grass,
               """.trimMargin())
    assertEquals(30, pos)
  }

  // VIM-146
  fun `test end of line with highlighting`() {
    setHighlightSearch()
    val pos = search("$",
      """${c}I found it in a legendary land
                  |all rocks and lavender and tufted grass,
               """.trimMargin())
    assertEquals(30, pos)
  }

  fun `test "and" without branches`() {
    val pos = search("\\&",
      """${c}I found it in a legendary land
                  |all rocks and lavender and tufted grass,
               """.trimMargin())
    assertEquals(1, pos)
  }

  // VIM-226
  fun `test "and" without branches with highlighting`() {
    setHighlightSearch()
    val pos = search("\\&",
      """${c}I found it in a legendary land
                  |all rocks and lavender and tufted grass,
               """.trimMargin())
    assertEquals(1, pos)
  }

  // VIM-528
  fun `test not found`() {
    val pos = search("(found)",
      """${c}I found it in a legendary land
                  |all rocks and lavender and tufted grass,
               """.trimMargin())
    assertEquals(-1, pos)
  }

  // VIM-528
  fun `test grouping`() {
    val pos = search("\\(found\\)",
      """${c}I found it in a legendary land
                  |all rocks and lavender and tufted grass,
               """.trimMargin())
    assertEquals(2, pos)
  }

  // VIM-855
  fun `test character class regression`() {
    val pos = search("[^c]b",
      "${c}bb\n")
    assertEquals(0, pos)
  }

  // VIM-855
  fun `test character class regression case insensitive`() {
    val pos = search("\\c[ABC]b",
      "${c}dd\n")
    assertEquals(-1, pos)
  }

  // VIM-856
  fun `test negative lookbehind regression`() {
    val pos = search("a\\@<!b",
      "${c}ab\n")
    assertEquals(-1, pos)
  }

  fun `test smart case search case insensitive`() {
    setIgnoreCaseAndSmartCase()
    val pos = search("tostring",
      "obj.toString();\n")
    assertEquals(4, pos)
  }

  fun `test smart case search case sensitive`() {
    setIgnoreCaseAndSmartCase()
    val pos = search("toString",
      """obj.tostring();
                 |obj.toString();""".trimMargin())
    assertEquals(20, pos)
  }

  fun `test search motion`() {
    typeTextInFile(parseKeys("/", "two", "<Enter>"),
      "${c}one two\n")
    assertOffset(4)
  }

  // |/pattern/e|
  fun `test search e motion offset`() {
    typeTextInFile(parseKeys("/", "two/e", "<Enter>"),
      "${c}one two three")
    assertOffset(6)
  }

  // |/pattern/e|
  fun `test search e-1 motion offset`() {
    typeTextInFile(parseKeys("/", "two/e-1", "<Enter>"),
      "${c}one two three")
    assertOffset(5)
  }

  // |/pattern/e|
  fun `test search e+2 motion offset`() {
    typeTextInFile(parseKeys("/", "two/e+2", "<Enter>"),
      "${c}one two three")
    assertOffset(8)
  }

  fun `test reverse search e+2 motion offset finds next match when starting on matching offset`() {
    typeTextInFile(parseKeys("?", "two?e+2", "<Enter>"),
      "one two three one two ${c}three")
    assertOffset(8)
  }

  fun `test search e+10 motion offset at end of file`() {
    typeTextInFile(parseKeys("/", "in/e+10", "<Enter>"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())
    assertPosition(3, 38)
  }

  fun `test search e+10 motion offset wraps at end of file`() {
    typeTextInFile(parseKeys("/", "in/e+10", "<Enter>", "n"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())
    // "in" at (0, 11) plus 10 offset from end
    assertOffset(22)
  }

  fun `test search e+10 motion offset wraps at exactly end of file`() {
    typeTextInFile(parseKeys("/", "ass./e+10", "<Enter>", "n"),
      """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |${c}hard by the torrent of a mountain pass.""".trimMargin())
    // "ass," at (1, 36) plus 10 offset from end
    assertPosition(2, 8)
  }

  // |/pattern/s|
  fun `test search s motion offset`() {
    typeTextInFile(parseKeys("/", "two/s", "<Enter>"),
      "${c}one two three")
    assertOffset(4)
  }

  // |/pattern/s|
  fun `test search s-2 motion offset`() {
    typeTextInFile(parseKeys("/", "two/s-2", "<Enter>"),
      "${c}one two three")
    assertOffset(2)
  }

  fun `test search s-2 motion offset finds next match when starting on matching offset`() {
    typeTextInFile(parseKeys("/", "two/s-2", "<Enter>"),
      "on${c}e two three one two three")
    assertOffset(16)
  }

  fun `test reverse search s-20 motion offset at beginning of file`() {
    typeTextInFile(parseKeys("?", "it?s-20", "<Enter>"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())
    assertOffset(0)
  }

  fun `test reverse search s-20 motion offset wraps at beginning of file`() {
    typeTextInFile(parseKeys("?", "it?s-20", "<Enter>", "N"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())
    // "it" at (2,5) minus 20 characters
    assertPosition(1, 27)
  }

  // |/pattern/s|
  fun `test search s+1 motion offset`() {
    typeTextInFile(parseKeys("/", "two/s+1", "<Enter>"),
      "${c}one two three")
    assertOffset(5)
  }

  fun `test reverse search s+2 motion offset finds next match when starting at matching offset`() {
    typeTextInFile(parseKeys("?", "two?s+2", "<Enter>"),
      "one two three one tw${c}o three")
    assertOffset(6)
  }

  // |/pattern/b|
  fun `test search b motion offset`() {
    typeTextInFile(parseKeys("/", "two/b", "<Enter>"),
      "${c}one two three")
    assertOffset(4)
  }

  // |/pattern/b|
  fun `test search b-2 motion offset`() {
    typeTextInFile(parseKeys("/", "two/b-2", "<Enter>"),
      "${c}one two three")
    assertOffset(2)
  }

  // |/pattern/b|
  fun `test search b+1 motion offset`() {
    typeTextInFile(parseKeys("/", "two/b+1", "<Enter>"),
      "${c}one two three")
    assertOffset(5)
  }

  fun `test search above line motion offset`() {
    typeTextInFile(parseKeys("/", "rocks/-1", "<Enter>"),
      """I found it in a legendary land
                 |${c}all rocks and lavender and tufted grass,
                 |where it was settled on some sodden sand
                 |hard by the torrent of a mountain pass.""".trimMargin())
    assertOffset(0)
  }

  fun `test search below line motion offset`() {
    typeTextInFile(parseKeys("/", "rocks/+2", "<Enter>"),
      """I found it in a legendary land
                 |${c}all rocks and lavender and tufted grass,
                 |where it was settled on some sodden sand
                 |hard by the torrent of a mountain pass.""".trimMargin())
    assertOffset(113)
  }

  // |i_CTRL-K|
  fun `test search digraph`() {
    typeTextInFile(parseKeys("/", "<C-K>O:", "<Enter>"),
      "${c}Hallo Österreich!\n")
    assertOffset(6)
  }

  @VimTestFunction("com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction")
  fun `test search word matches case`() {
    typeTextInFile(parseKeys("*"),
      "${c}Editor editor Editor")
    assertOffset(14)
  }

  @VimTestFunction("com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction")
  fun `test search next word matches case`() {
    typeTextInFile(parseKeys("*", "n"),
      "${c}Editor editor Editor editor Editor")
    assertOffset(28)
  }

  @VimTestFunction("com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction")
  fun `test search word honours ignorecase`() {
    setIgnoreCase()
    typeTextInFile(parseKeys("*"),
      "${c}editor Editor editor")
    assertOffset(7)
  }

  @VimTestFunction("com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction")
  fun `test search next word honours ignorecase`() {
    setIgnoreCase()
    typeTextInFile(parseKeys("*", "n"),
      "${c}editor Editor editor")
    assertOffset(14)
  }

  @VimTestFunction("com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction")
  fun `test search word overrides smartcase`() {
    setIgnoreCaseAndSmartCase()
    typeTextInFile(parseKeys("*"),
      "${c}Editor editor Editor")
    assertOffset(7)
  }

  @VimTestFunction("com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction")
  fun `test search next word overrides smartcase`() {
    setIgnoreCaseAndSmartCase()
    typeTextInFile(parseKeys("*", "n"),
      "${c}Editor editor editor")
    assertOffset(14)
  }

  fun `test incsearch moves caret to start of first match`() {
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "la"))
    assertPosition(1, 14)
  }

  fun `test incsearch + hlsearch moves caret to start of first match`() {
    setHighlightSearch()
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "la"))
    assertPosition(1, 14)
  }

  fun `test incsearch moves caret to start of first match (backwards)`() {
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("?", "la"))
    assertPosition(0, 26)
  }

  fun `test incsearch + hlsearch moves caret to start of first match (backwards)`() {
    setHighlightSearch()
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("?", "la"))
    assertPosition(0, 26)
  }

  fun `test incsearch resets caret if no match found`() {
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "lazzz"))
    assertPosition(1, 0)
  }

  fun `test incsearch + hlsearch resets caret if no match found`() {
    setHighlightSearch()
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "lazzz"))
    assertPosition(1, 0)
  }

  fun `test incsearch resets caret if cancelled`() {
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "la"))
    typeText(parseKeys("<Esc>"))
    assertPosition(1, 0)
  }

  fun `test incsearch + hlsearch resets caret if cancelled`() {
    setHighlightSearch()
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "la"))
    typeText(parseKeys("<Esc>"))
    assertPosition(1, 0)
  }

  fun `test incsearch resets caret on backspace`() {
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "wh"))
    assertPosition(2, 0)
    typeText(parseKeys("<BS><BS>"))
    assertPosition(1, 0)
  }

  fun `test incsearch + hlsearch resets caret on backspace`() {
    setHighlightSearch()
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "wh"))
    assertPosition(2, 0)
    typeText(parseKeys("<BS><BS>"))
    assertPosition(1, 0)
  }

  fun `test search result position with incsearch`() {
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "and", "<CR>"))
    assertPosition(1, 10)
  }

  fun `test search result position with incsearch + hlsearch`() {
    setHighlightSearch()
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("/", "and", "<CR>"))
    assertPosition(1, 10)
  }

  fun `test incsearch highlights only current match with nohlsearch`() {
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys("/", "and"))

    assertSearchHighlights("and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch highlights only current match with nohlsearch (backwards)`() {
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
           |all rocks and lave${c}nder and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys("?", "a"))

    assertSearchHighlights("a",
      """I found it in a legendary land
           |all rocks and l‷a‴vender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch highlights all matches with hlsearch enabled`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys("/", "and"))

    assertSearchHighlights("and",
      """I found it in a legendary l«and»
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch removes all highlights if no match`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys("/", "and"))
    assertSearchHighlights("and",
      """I found it in a legendary l«and»
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("zz"))

    assertSearchHighlights("and",
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch does not hide previous search until first character is typed`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    enterSearch("and")
    typeText(parseKeys("/"))
    assertSearchHighlights("and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
    typeText(parseKeys("v"))

    assertSearchHighlights("v",
      """I found it in a legendary land
           |all rocks and la‷v‴ender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch does not show previous search highlights when text field is deleted`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    enterSearch("and")
    typeText(parseKeys("/", "grass", "<BS><BS><BS><BS><BS>"))

    assertSearchHighlights("and",
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test cancelling incsearch shows previous search highlights`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    enterSearch("and")
    typeText(parseKeys("/", "grass", "<Esc>"))

    assertSearchHighlights("and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test cancelling incsearch does not show previous search highlights after nohls command`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    enterSearch("and")
    enterCommand("nohlsearch")
    typeText(parseKeys("/", "grass", "<Esc>"))

    assertSearchHighlights("and",
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch highlights for substitute command`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys(":", "%s/and"))

    assertSearchHighlights("and",
      """I found it in a legendary l‷and‴
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch only highlights for substitute command after valid argument`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    // E.g. don't remove highlights when trying to type :set
    enterSearch("and")
    typeText(parseKeys(":s"))

    assertSearchHighlights("and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch highlights for substitute command only highlights in range`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |${c}hard by the torrent and rush of a mountain pass.""".trimMargin())

    typeText(parseKeys(":", "2,3s/and"))

    assertSearchHighlights("and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent and rush of a mountain pass.""".trimMargin())
  }

  fun `test incsearch highlights for substitute command in current line with no range`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys(":", "s/and"))

    assertSearchHighlights("and",
      """I found it in a legendary land
           |all rocks ‷and‴ lavender «and» tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch for substitute command starts at beginning of range not caret position`() {
    setIncrementalSearch()
    setHighlightSearch()
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
           |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys(":", "2,8s/and"))

    assertSearchHighlights("and",
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
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch highlights for substitute command clears highlights on backspace`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys(":", "%s/and", "<BS><BS><BS>"))

    assertSearchHighlights("and",
      """I found it in a legendary land
             |all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test incsearch highlights for substitute command resets highlights on backspace`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    enterSearch("and")
    typeText(parseKeys(":", "%s/roc", "<BS><BS><BS>"))

    assertSearchHighlights("and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())

    // TODO: Check caret position
  }

  fun `test cancelling incsearch highlights for substitute command shows previous highlights`() {
    setIncrementalSearch()
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    enterSearch("and")
    typeText(parseKeys(":", "%s/ass", "<Esc>"))

    assertSearchHighlights("and",
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())

    // TODO: Check caret position
  }

  fun `test highlight search results`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test search removes previous search highlights`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch("mountain")
    enterSearch(pattern)

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test no highlights for unmatched search`() {
    setHighlightSearch()
    typeTextInFile(parseKeys("/", "zzzz", "<Enter>"),
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())
    assertNoSearchHighlights()
  }

  fun `test nohlsearch command removes highlights`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())
    enterSearch("and")
    enterCommand("nohlsearch")
    assertNoSearchHighlights()
  }

  fun `test find next after nohlsearch command shows highlights`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    enterCommand("nohlsearch")
    typeText(parseKeys("n"))

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test nohlsearch correctly resets incsearch highlights after deleting last occurrence`() {
    // Crazy edge case bug. With incsearch enabled, search for something with only one occurrence, delete it, call
    // :nohlsearch, undo and search next - highlights don't work any more
    setHighlightSearch()
    setIncrementalSearch()
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "lavender"
    enterSearch(pattern)
    typeText(parseKeys("dd"))
    enterCommand("nohlsearch")
    typeText(parseKeys("u"))
    typeText(parseKeys("n"))

    assertSearchHighlights(pattern,
      """I found it in a legendary land
        |all rocks and «lavender» and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test nohlsearch option hides search highlights`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())
    enterSearch("and")
    clearHighlightSearch()
    assertNoSearchHighlights()
  }

  fun `test setting hlsearch option shows search highlights for last search`() {
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    setHighlightSearch()

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test deleting text moves search highlights`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    typeText(parseKeys("b", "dw"))  // deletes "rocks "

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test deleting match removes search highlight`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    typeText(parseKeys("dw")) // deletes first "and " on line 2

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all rocks lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test deleting part of match removes search highlight`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    typeText(parseKeys("xx")) // deletes "an" from first "and" on line 2

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all rocks d lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test deleting part of match keeps highlight if pattern still matches`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = """\<s\w*d\>"""  // Should match "settled" and "sand"
    enterSearch(pattern)
    typeText(parseKeys("l", "xxx")) // Change "settled" to "sled"

    assertSearchHighlights(pattern,
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was «sled» on some sodden «sand»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test inserting text moves search highlights`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    typeText(parseKeys("h", "i", ", trees"))  // inserts ", trees" before first "and" on line 2

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all rocks, trees «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test inserting text inside match removes search highlight`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    typeText(parseKeys("l", "i", "FOO"))  // inserts "FOO" inside first "and" - "aFOOnd"

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all rocks aFOOnd lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test inserting text inside match keeps highlight if pattern still matches`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = """\<s\w*d\>"""  // Should match "settled" and "sand"
    enterSearch(pattern)
    typeText(parseKeys("l", "i", "FOO", "<Esc>")) // Change "settled" to "sFOOettled"

    assertSearchHighlights(pattern,
      """I found it in a legendary land
           |all rocks and lavender and tufted grass,
           |where it was «sFOOettled» on some sodden «sand»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test inserting text shows highlight if it contains matches`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    typeText(parseKeys("o", "and then I saw a cat and a dog", "<Esc>"))

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all rocks «and» lavender «and» tufted grass,
           |«and» then I saw a cat «and» a dog
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test replacing text moves search highlights`() {
    val pattern = "and"
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())

    enterSearch(pattern)
    typeText(parseKeys("b", "cw", "boulders", "<Esc>"))  // Replaces "rocks" with "boulders" on line 2

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
           |all boulders «and» lavender «and» tufted grass,
           |where it was settled on some sodden s«and»
           |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test replacing text inside match removes search highlight`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
         |${c}all rocks and lavender and tufted grass,
         |where it was settled on some sodden sand
         |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    typeText(parseKeys("l", "cw", "lso", "<Esc>")) // replaces "nd" in first "and" with "lso" on line 2

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
         |all rocks also lavender «and» tufted grass,
         |where it was settled on some sodden s«and»
         |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test replacing text shows highlight if it contains matches`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
          |${c}all rocks and lavender and tufted grass,
          |where it was settled on some sodden sand
          |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = "and"
    enterSearch(pattern)
    typeText(parseKeys("w", "cw", "trees and boulders", "<Esc>"))

    assertSearchHighlights(pattern,
      """I found it in a legendary l«and»
          |all rocks «and» trees «and» boulders «and» tufted grass,
          |where it was settled on some sodden s«and»
          |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test replacing text inside match keeps highlight if pattern still matches`() {
    setHighlightSearch()
    configureByText(
      """I found it in a legendary land
            |${c}all rocks and lavender and tufted grass,
            |where it was settled on some sodden sand
            |hard by the torrent of a mountain pass.""".trimMargin())

    val pattern = """\<s\w*d\>"""  // Should match "settled" and "sand"
    enterSearch(pattern)
    typeText(parseKeys("l", "ctl", "huff", "<Esc>")) // Change "settled" to "shuffled"

    assertSearchHighlights(pattern,
      """I found it in a legendary land
            |all rocks and lavender and tufted grass,
            |where it was «shuffled» on some sodden «sand»
            |hard by the torrent of a mountain pass.""".trimMargin())
  }

  fun `test search highlight with tabs`() {
    setHighlightSearch()
    configureByText("\tfoo")
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

  private fun setIgnoreCase() = OptionsManager.ignorecase.set()

  private fun setIgnoreCaseAndSmartCase() {
    OptionsManager.ignorecase.set()
    OptionsManager.smartcase.set()
  }

  private fun setHighlightSearch() = OptionsManager.hlsearch.set()
  private fun clearHighlightSearch() = OptionsManager.hlsearch.reset()
  private fun setIncrementalSearch() = OptionsManager.incsearch.set()

  private fun search(pattern: String, input: String): Int {
    myFixture.configureByText("a.java", input)
    val editor = myFixture.editor
    val project = myFixture.project
    val searchGroup = VimPlugin.getSearch()
    val ref = Ref.create(-1)
    RunnableHelper.runReadCommand(project, {
      val n = searchGroup.search(editor, pattern, 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
      ref.set(n)
    }, null, null)
    return ref.get()
  }

  private fun assertNoSearchHighlights() {
    assertEquals(0, myFixture.editor.markupModel.allHighlighters.size)
  }

  private fun assertSearchHighlights(tooltip: String, expected: String) {
    val allHighlighters = myFixture.editor.markupModel.allHighlighters

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
      assertEquals("Incorrect background colour for highlighter at $offsets", attributes.backgroundColor, it.textAttributes?.backgroundColor)
      assertEquals("Incorrect foreground colour for highlighter at $offsets", attributes.foregroundColor, it.textAttributes?.foregroundColor)
      // TODO: Find a better way to identify the current match
      if (it.textAttributes?.effectType == EffectType.ROUNDED_BOX) {
        assertEquals("Incorrect effect type for highlighter at $offsets", EffectType.ROUNDED_BOX, it.textAttributes?.effectType)
        assertEquals("Incorrect effect colour for highlighter at $offsets", caretColour, it.textAttributes?.effectColor)
      } else {
        assertEquals("Incorrect effect type for highlighter at $offsets", attributes.effectType, it.textAttributes?.effectType)
        assertEquals("Incorrect effect colour for highlighter at $offsets", attributes.effectColor, it.textAttributes?.effectColor)
      }
    }
  }
}
