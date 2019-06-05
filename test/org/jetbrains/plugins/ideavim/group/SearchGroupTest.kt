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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.group

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.group.SearchGroup
import com.maddyhome.idea.vim.helper.RunnableHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.Options
import junit.framework.TestCase
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

    // |/pattern/s|
    fun `test search s+1 motion offset`() {
        typeTextInFile(parseKeys("/", "two/s+1", "<Enter>"),
                "${c}one two three")
        assertOffset(5)
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

    fun `test search word matches case`() {
        typeTextInFile(parseKeys("*"),
            "${c}Editor editor Editor")
        assertOffset(14)
    }

    fun `test search next word matches case`() {
        typeTextInFile(parseKeys("*", "n"),
            "${c}Editor editor Editor editor Editor")
        assertOffset(28)
    }

    fun `test search word honours ignorecase`() {
        setIgnoreCase()
        typeTextInFile(parseKeys("*"),
            "${c}editor Editor editor")
        assertOffset(7)
    }

    fun `test search next word honours ignorecase`() {
        setIgnoreCase()
        typeTextInFile(parseKeys("*", "n"),
            "${c}editor Editor editor")
        assertOffset(14)
    }

    fun `test search word overrides smartcase`() {
        setIgnoreCaseAndSmartCase()
        typeTextInFile(parseKeys("*"),
            "${c}Editor editor Editor")
        assertOffset(7)
    }

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

    fun `test incsearch resets caret if no match found`() {
      setIncrementalSearch()
      configureByText(
        """I found it in a legendary land
             |${c}all rocks and lavender and tufted grass,
             |where it was settled on some sodden sand
             |hard by the torrent of a mountain pass.""".trimMargin())
      typeText(parseKeys("/", "lazzz"))
      assertOffset(31)
    }

    fun `test incsearch resets caret if cancelled`() {
      setIncrementalSearch()
      configureByText(
        """I found it in a legendary land
           |${c}all rocks and lavender and tufted grass,
           |where it was settled on some sodden sand
           |hard by the torrent of a mountain pass.""".trimMargin())
      typeText(parseKeys("/", "la"))
      assertOffset(45)
      typeText(parseKeys("<Esc>"))
      assertOffset(31)
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

    fun `test search removes last search highlights`() {

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

    // Ensure that the offsets for the last carriage return in the file are valid, even though it's for a line that
    // doesn't exist
    fun `test find last cr in file`() {
        myFixture.configureByText("a.txt", "Something\n")
        val textRange = SearchGroup.findNext(myFixture.editor, "\\n", 0, false, true)
        assertNotNull(textRange)
        TestCase.assertEquals(9, textRange?.startOffset)
        TestCase.assertEquals(10, textRange?.endOffset)
    }

    private fun setIgnoreCase() {
      setOption(Options.IGNORE_CASE)
    }

    private fun setIgnoreCaseAndSmartCase() {
      setOption(Options.IGNORE_CASE)
      setOption(Options.SMART_CASE)
    }

    private fun setHighlightSearch() = setOption(Options.HIGHLIGHT_SEARCH)
    private fun clearHighlightSearch() = resetOption(Options.HIGHLIGHT_SEARCH)

    private fun setIncrementalSearch() {
      setOption(Options.INCREMENTAL_SEARCH)
    }

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
      TestCase.assertEquals(0, myFixture.editor.markupModel.allHighlighters.size)
    }

    private fun assertSearchHighlights(tooltip: String, expected: String) {
      val allHighlighters = myFixture.editor.markupModel.allHighlighters

      val actual = StringBuilder(myFixture.editor.document.text)
      val inserts = mutableMapOf<Int, String>()

      allHighlighters.forEach {
        inserts.compute(it.startOffset) { _, v -> if (v == null) "«" else "$v«" }
        inserts.compute(it.endOffset) { _, v -> if (v == null) "»" else "$v»" }
      }

      var offset = 0
      inserts.toSortedMap().forEach { (k, v) ->
        actual.insert(k + offset, v)
        offset += v.length
      }

      assertEquals(expected, actual.toString())

      // Assert all highlighters have the correct tooltip and text attributes
      val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)
      allHighlighters.forEach {
        val offsets = "(${it.startOffset}, ${it.endOffset})"
        assertEquals("Incorrect tooltip for highlighter at $offsets", tooltip, it.errorStripeTooltip)
        assertEquals("Incorrect background colour for highlighter at $offsets", attributes.backgroundColor, it.textAttributes?.backgroundColor)
        assertEquals("Incorrect foreground colour for highlighter at $offsets", attributes.foregroundColor, it.textAttributes?.foregroundColor)
        assertEquals("Incorrect effect type for highlighter at $offsets", attributes.effectType, it.textAttributes?.effectType)
        assertEquals("Incorrect effect colour for highlighter at $offsets", attributes.effectColor, it.textAttributes?.effectColor)
      }
    }
}