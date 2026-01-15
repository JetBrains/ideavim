/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group

import com.intellij.idea.TestFor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
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
    assertPluginErrorMessage("E486: Pattern not found: (found)")
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
  @TestWithoutNeovim(reason = SkipNeovimReason.IDEAVIM_API_USED)
  @Test
  fun `test negative lookbehind regression`() {
    val pos = search(
      "a\\@<!b",
      "${c}ab\n",
    )
    assertEquals(-1, pos)
  }

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
    assertPluginErrorMessage("search hit BOTTOM, continuing at TOP")
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
    assertPluginErrorMessage("E385: Search hit BOTTOM without match for: one")
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
    assertPluginErrorMessage("E486: Pattern not found: banana")
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
    assertPluginErrorMessage("search hit TOP, continuing at BOTTOM")
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
    assertPluginErrorMessage("E384: Search hit TOP without match for: three")
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
    assertPluginErrorMessage("E486: Pattern not found: banana")
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
    // "in" at (0, 11) plus 10 offset from the end
    assertOffset(22)
  }

  @TestWithoutNeovim(
    SkipNeovimReason.INTELLIJ_PLATFORM_INHERITED_DIFFERENCE,
    "In the test the caret lands on the very last character. In Vim there is always a new line character, but in IntelliJ Platform there is not."
  )
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
    // "ass," at (1, 36) plus 10 offset from the end
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

  @VimBehaviorDiffers(
    description = "IdeaVim finishes one character to the right: at the 't' of 'tufted'. I'm not sure, but requires investigation if this should be fixed.",
    shouldBeFixed = true,
  )
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
    assertPluginErrorMessage("E385: Search hit BOTTOM without match for: one")
    assertPosition(2, 0)
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
    assertPluginErrorMessage("E384: Search hit TOP without match for: one")
    assertPosition(8, 0)
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
  @Test
  fun `test search word honours ignorecase`() {
    configureByText("${c}editor Editor editor")
    enterCommand("set ignorecase")
    typeText("*")
    assertOffset(7)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun `test search next word honours ignorecase`() {
    configureByText("${c}editor Editor editor")
    enterCommand("set ignorecase")
    typeText("*", "n")
    assertOffset(14)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun `test search word overrides smartcase`() {
    configureByText("${c}Editor editor Editor")
    enterCommand("set ignorecase smartcase")
    typeText("*")
    assertOffset(7)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun `test search next word overrides smartcase`() {
    configureByText("${c}Editor editor editor")
    enterCommand("set ignorecase smartcase")
    typeText("*", "n")
    assertOffset(14)
  }

  @Test
  fun `test search result position after incsearch`() {
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

  // Ensure that the offsets for the last carriage return in the file are valid, even though it's for a line that
  // doesn't exist
  @Test
  fun `test find last cr in file`() {
    val res = search("\\n", "Something\n")
    assertEquals(9, res)
  }

  @Test
  fun `test update selection with search result`() {
    doTest(
      listOf("v", "/ipsum", "<CR>"),
      """
        |${c}Lorem ipsum dolor sit amet,
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
      Mode.VISUAL(SelectionType.CHARACTER_WISE)
    )
  }

  @Test
  fun `test update selection with search result and selection option set to exclusive`() {
    doTest(
      listOf("v", "/ipsum", "<CR>"),
      """
        |${c}Lorem ipsum dolor sit amet,
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
      Mode.VISUAL(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set selection=exclusive")
    }
  }

  private fun search(pattern: String, input: String): Int {
    configureByText(input)
    val editor = fixture.editor
    val searchGroup = VimPlugin.getSearch()
    val ref = Ref.create(-1)
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteIntentReadAction<Any, Throwable> {
        // Does not move the caret!
        val n = searchGroup.processSearchCommand(editor.vim, pattern, fixture.caretOffset, 1, Direction.FORWARDS)
        ref.set(n?.first ?: -1)
      }
    }
    return ref.get()
  }
}
