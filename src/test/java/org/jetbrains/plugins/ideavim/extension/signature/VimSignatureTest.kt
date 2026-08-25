/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.signature

import com.intellij.openapi.editor.Editor
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimSplitWindowTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Gutter signs for local marks - a port of kshenoy/vim-signature (VIM-1347).
 *
 * Global marks `A`-`Z` are already visible: with `ideamarks` on they become IDE bookmarks, which the platform draws in
 * the gutter itself. Local marks `a`-`z` have no visual at all, and that is what this port adds.
 */
class VimSignatureTest : VimSplitWindowTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("signature")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test no signs when no mark is set`() {
    configureByText(text)

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test setting a local mark signs its line`() {
    configureByText(text)
    typeText("ma")

    assertSigns(0 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test sign is placed on the line of the mark, not on the line of the caret`() {
    configureByText(text)
    typeText("jjma", "gg")

    assertSigns(2 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test every local mark gets its own sign`() {
    configureByText(text)
    typeText("ma", "jmb", "jjmc")

    assertSigns(0 to "a", 1 to "b", 3 to "c")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test marks on the same line have separate signs`() {
    configureByText(text)
    typeText("jma", "\$mb")

    assertSigns(1 to "a", 1 to "b")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test resetting a mark moves its sign`() {
    configureByText(text)
    typeText("ma", "jjjma")

    assertSigns(3 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test the last mark on a line keeps the sign when a sibling moves away`() {
    configureByText(text)
    typeText("ma", "mb", "jjmb")

    assertSigns(0 to "a", 2 to "b")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test moving a mark updates its sign`() {
    configureByText(text)
    typeText("ma", "j", "ma")

    assertSigns(1 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test delmarks removes the sign`() {
    configureByText(text)
    typeText("ma", "jmb")
    enterCommand("delmarks a")

    assertSigns(1 to "b")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test delmarks with bang removes every sign`() {
    configureByText(text)
    typeText("ma", "jmb", "jmc")
    enterCommand("delmarks!")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test sign follows the mark when a line is inserted above`() {
    configureByText(text)
    typeText("jjma", "gg", "Onew line<Esc>")

    assertSigns(3 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test sign follows the mark when a line above is deleted`() {
    configureByText(text)
    typeText("jjma", "gg", "dd")

    assertSigns(1 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test sign is dropped when the marked line is deleted`() {
    configureByText(text)
    typeText("jjma", "dd")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test global marks are not signed by the extension`() {
    configureByText(text)
    typeText("jmA")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test global mark next to a local one leaves only the local sign`() {
    configureByText(text)
    typeText("jma", "jmB")

    assertSigns(1 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test the last change mark is not signed`() {
    configureByText(text)
    typeText("jjx")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test the last jump mark is not signed`() {
    configureByText(text)
    typeText("G")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test change marks are not signed`() {
    configureByText(text)
    typeText("yy")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test disabling the extension removes every sign`() {
    configureByText(text)
    typeText("ma", "jmb")
    enterCommand("set nosignature")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test no sign is placed while the extension is disabled`() {
    configureByText(text)
    enterCommand("set nosignature")
    typeText("ma")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test enabling the extension signs the marks that already exist`() {
    configureByText(text)
    enterCommand("set nosignature")
    typeText("ma", "jjmb")
    enterCommand("set signature")

    assertSigns(0 to "a", 2 to "b")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test mark sign appears in split window of the same file`() {
    configureByText(text)
    val mainEditor = fixture.editor

    typeText("ma")
    assertSigns(0 to "a")

    val splitEditor = openSplitWindow(mainEditor)

    assertHasSigns(splitEditor, 0 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test mark set in split is also visible in the original window`() {
    configureByText(text)
    val mainEditor = fixture.editor
    val splitEditor = openSplitWindow(mainEditor)

    selectWindow(splitEditor)
    typeText("jma")

    assertHasSigns(mainEditor, 1 to "a")
    assertHasSigns(splitEditor, 1 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test moving a mark updates the sign in both windows`() {
    configureByText(text)
    val mainEditor = fixture.editor
    typeText("jjma")
    val splitEditor = openSplitWindow(mainEditor)

    selectWindow(mainEditor)
    typeText("jma")

    assertHasSigns(mainEditor, 3 to "a")
    assertHasSigns(splitEditor, 3 to "a")
    assertNoSignOnLine(mainEditor, line = 2)
    assertNoSignOnLine(splitEditor, line = 2)
  }


  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m comma places mark a when no marks exist`() {
    configureByText(text)
    typeText("m,")

    assertSigns(0 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m comma places mark b when a is already used`() {
    configureByText(text)
    typeText("ma", "jm,")

    assertSigns(0 to "a", 1 to "b")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m comma picks the first free mark alphabetically`() {
    configureByText(text)
    typeText("ma", "jmc", "jjm,")

    assertSigns(0 to "a", 1 to "c", 3 to "b")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m comma advances through the alphabet with each use`() {
    configureByText(text)
    typeText("m,", "jm,", "jm,")

    assertSigns(0 to "a", 1 to "b", 2 to "c")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m comma skips marks used in the same buffer`() {
    configureByText(text)
    typeText("mb")
    typeText("jm,")

    assertSigns(0 to "b", 1 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m comma on the same line as an existing mark places a new mark`() {
    configureByText(text)
    typeText("ma", "m,")

    assertSigns(0 to "a", 0 to "b")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m comma when all marks are used shows an error`() {
    configureByText(text)
    for (mark in 'a'..'z') {
      typeText("m$mark")
    }
    typeText("m,")

    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m minus removes a single mark from the current line`() {
    configureByText(text)
    typeText("ma", "m-")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m minus removes all marks from the current line`() {
    configureByText(text)
    typeText("ma", "\$mb", "m-")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m minus does not affect marks on other lines`() {
    configureByText(text)
    typeText("ma", "jmb", "m-")

    assertSigns(0 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m minus on a line with no marks does nothing`() {
    configureByText(text)
    typeText("ma", "jm-")

    assertSigns(0 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m minus on an empty line does nothing`() {
    configureByText(text)
    typeText("ma", "jjm-")

    assertSigns(0 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m minus removes mark from current line but keeps marks on other lines intact`() {
    configureByText(text)
    typeText("ma", "jmb", "jmc", "km-")

    assertSigns(0 to "a", 2 to "c")
  }

  // ----- m<Space> (delete all marks from current buffer) -----

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m space removes all marks from the buffer`() {
    configureByText(text)
    typeText("ma", "jmb", "jmc", "m<Space>")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m space on a buffer with no marks does nothing`() {
    configureByText(text)
    typeText("m<Space>")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m space removes marks from all lines in the buffer`() {
    configureByText(text)
    typeText("ma", "jmb", "jjmc", "jmd")
    typeText("m<Space>")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m space does not affect marks in a split window of the same file`() {
    configureByText(text)
    val mainEditor = fixture.editor
    typeText("jma")

    val splitEditor = openSplitWindow(mainEditor)
    selectWindow(mainEditor)
    typeText("m<Space>")

    assertNoSigns()
    assertNoSignOnLine(splitEditor, line = 1)
  }

  // ----- ]' / [' (jump to start of next/prev line containing a mark) -----

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to next marked line moves to start of the line`() {
    configureByText(text)
    typeText("jjwma", "gg")
    typeText("]'")

    assertPosition(2, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to next marked line always lands on column zero regardless of mark column`() {
    configureByText(text)
    typeText("jj\$ma", "gg")
    typeText("]'")

    assertPosition(2, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to next marked line traverses lines in order`() {
    configureByText(text)
    typeText("jma", "jjmb", "gg")
    typeText("]'")
    assertPosition(1, 0)
    typeText("]'")
    assertPosition(3, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to next marked line treats multiple marks on same line as a single stop`() {
    configureByText(text)
    typeText("jma", "\$mb", "jjmc", "gg")
    typeText("]'")
    assertPosition(1, 0)
    typeText("]'")
    assertPosition(3, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to next marked line wraps around to the first marked line`() {
    configureByText(text)
    typeText("jma", "jjmb", "jmb")
    assertPosition(4, 0)
    typeText("]'")

    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to next marked line does nothing when no marks exist`() {
    configureByText(text)
    typeText("]'")

    assertPosition(0, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to next marked line navigates to global marks in the current file`() {
    configureByText(text)
    typeText("jjwmA", "gg")
    typeText("]'")

    assertPosition(2, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to next marked line navigates through mixed local and global marks in line order`() {
    configureByText(text)
    typeText("jma", "jjmA", "gg")
    typeText("]'")
    assertPosition(1, 0)
    typeText("]'")
    assertPosition(3, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to prev marked line moves to start of the line`() {
    configureByText(text)
    typeText("jma", "jjmb", "jjgg")
    typeText("['")

    assertPosition(3, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to prev marked line always lands on column zero regardless of mark column`() {
    configureByText(text)
    typeText("jj\$ma", "jjgg")
    typeText("['")

    assertPosition(2, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to prev marked line traverses lines in reverse order`() {
    configureByText(text)
    typeText("jma", "jjmb", "jjgg")
    typeText("['")
    assertPosition(3, 0)
    typeText("['")
    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to prev marked line treats multiple marks on same line as a single stop`() {
    configureByText(text)
    typeText("jma", "\$mb", "jjmc", "jjgg")
    typeText("['")
    assertPosition(3, 0)
    typeText("['")
    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to prev marked line wraps around to the last marked line`() {
    configureByText(text)
    typeText("jma", "jjmb", "gg")
    typeText("['")

    assertPosition(3, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to prev marked line does nothing when no marks exist`() {
    configureByText(text)
    typeText("G", "['")

    assertPosition(4, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to prev marked line navigates to global marks in the current file`() {
    configureByText(text)
    typeText("jjwmA", "G")
    typeText("['")

    assertPosition(2, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to prev marked line navigates through mixed local and global marks in reverse line order`() {
    configureByText(text)
    typeText("jma", "jjmA", "G")
    typeText("['")
    assertPosition(3, 0)
    typeText("['")
    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test deleting one marked line leaves the signs of the other marks in place`() {
    configureByText(text)
    typeText("ma", "jmb", "jjmc")
    assertSigns(0 to "a", 1 to "b", 3 to "c")

    typeText("gg", "j", "dd")  // removes mark b with its line, and pulls mark c up one line

    assertSigns(0 to "a", 2 to "c")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test m comma still picks the first free mark while the file is open in a split`() {
    configureByText(text)
    val mainEditor = fixture.editor
    typeText("ma")
    openSplitWindow(mainEditor)
    selectWindow(mainEditor)

    typeText("jm,")

    assertSigns(0 to "a", 1 to "b")
  }

  // ----- <Plug> mappings -----

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test a key mapped to the plug name places the next mark`() {
    configureByText(text)
    enterCommand("nmap X <Plug>(SignaturePlaceNextMark)")

    typeText("X")

    assertSigns(0 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test a key mapped to the plug name purges the marks of the line`() {
    configureByText(text)
    enterCommand("nmap X <Plug>(SignaturePurgeMarksAtLine)")
    typeText("ma", "jmb")

    typeText("X")

    assertSigns(0 to "a")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test a key mapped to the plug name purges the marks of the buffer`() {
    configureByText(text)
    enterCommand("nmap X <Plug>(SignaturePurgeMarks)")
    typeText("ma", "jmb")

    typeText("X")

    assertNoSigns()
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test a key mapped to the plug name jumps to the next marked line`() {
    configureByText(text)
    enterCommand("nmap X <Plug>(SignatureGotoNextLineByPos)")
    typeText("jjma", "gg")

    typeText("X")

    assertPosition(2, 0)
  }

  // ----- ]` / [` are IdeaVim’s own motions and the extension must leave them alone -----

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test backtick bracket keeps honouring a count`() {
    configureByText(text)
    typeText("jma", "jmb", "jmc", "gg")
    typeText("2]`")

    assertPosition(2, 0)  // the second mark forward, not the first
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test backtick bracket does not wrap past the last mark`() {
    configureByText(text)
    typeText("jma", "jmb", "jmc", "G")
    typeText("]`")

    assertPosition(4, 0)  // Vim beeps here, it does not wrap round to the first mark
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test backtick bracket is still a motion an operator can use`() {
    configureByText(text)
    typeText("jjma", "gg")
    typeText("d]`")

    assertState(
      """
        ${c}Sed in orci mauris.
        Cras id tellus in ex imperdiet egestas.
        Nulla porta tristique.
      """.trimIndent()
    )
  }

  // ----- counts -----

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test count on jump to next marked line`() {
    configureByText(text)
    typeText("jma", "jmb", "jmc", "gg")
    typeText("3]'")

    assertPosition(3, 0)  // mark c
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test count on jump to prev marked line`() {
    configureByText(text)
    typeText("jma", "jmb", "jmc", "G")
    typeText("2['")

    assertPosition(2, 0)  // mark b
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test count larger than the number of marks wraps around`() {
    configureByText(text)
    typeText("jma", "jmb", "gg")
    typeText("3]'")

    assertPosition(1, 0)  // a, b, then round to a again
  }

  // ----- jump list -----

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to a marked line can be undone with ctrl-o`() {
    configureByText(text)
    typeText("jjjjma", "gg")
    typeText("]'")
    assertPosition(4, 0)

    typeText("<C-O>")

    assertPosition(0, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test a jump that does not move the caret adds nothing to the jump list`() {
    configureByText(text)
    typeText("jjma")  // the only mark is on the caret's own line
    typeText("jj")
    assertPosition(4, 0)
    typeText("gg")

    typeText("]'")  // wraps to the only marked line
    assertPosition(2, 0)
    typeText("]'")  // already there, so this must not push a new entry
    typeText("<C-O>")

    assertPosition(0, 0)
  }

  // ----- line jumps land on the first non-blank, as Vim’s mark jumps do -----

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to next marked line lands on the first non-blank`() {
    configureByText(indentedText)
    typeText("jjma", "gg")
    typeText("]'")

    assertPosition(2, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test jump to prev marked line lands on the first non-blank`() {
    configureByText(indentedText)
    typeText("jjma", "G")
    typeText("['")

    assertPosition(2, 4)
  }

  // ----- helpers -----

  private val indentedText = """
    ${c}Lorem ipsum dolor sit amet,
    consectetur adipiscing elit
        Sed in orci mauris.
    Cras id tellus in ex imperdiet egestas.
    Nulla porta tristique.
  """.trimIndent()

  private val text = """
    ${c}Lorem ipsum dolor sit amet,
    consectetur adipiscing elit
    Sed in orci mauris.
    Cras id tellus in ex imperdiet egestas.
    Nulla porta tristique.
  """.trimIndent()

  /** The gutter signs of the current editor as `line number (zero based) to tooltip`, ordered top down. */
  private fun signs(): List<Pair<Int, String>> = signsIn(fixture.editor)

  private fun signsIn(editor: Editor): List<Pair<Int, String>> {
    return editor.markupModel.allHighlighters
      .filter { it.gutterIconRenderer != null }
      .map { editor.document.getLineNumber(it.startOffset) to (it.gutterIconRenderer?.tooltipText ?: "") }
      .sortedBy { it.first }
  }

  /** Asserts that exactly the given lines are signed, and that each line is signed for exactly the marks expected. */
  private fun assertSigns(vararg expected: Pair<Int, String>) {
    val actual = signs()
    assertEquals(
      expected.map { it.first },
      actual.map { it.first },
      "Signed lines do not match. Signs: $actual",
    )
    // Compared per line rather than pairwise: two marks on one line come back in an arbitrary order.
    val expectedByLine = expected.groupBy({ it.first }, { it.second }).mapValues { it.value.joinToString("").toSet() }
    val actualByLine = actual.groupBy({ it.first }, { it.second }).mapValues { it.value.joinToString("").toSet() }
    assertEquals(expectedByLine, actualByLine, "Marks named by the signs do not match. Signs: $actual")
  }

  private fun assertHasSigns(editor: Editor, vararg expected: Pair<Int, String>) {
    val actual = signsIn(editor)
    for ((line, marks) in expected) {
      val tooltip = actual.find { it.first == line }?.second
      for (mark in marks) {
        assertTrue(
          tooltip != null && tooltip.contains(mark),
          "Expected sign for mark '$mark' on line $line in ${editorLabel(editor)}, but signs were: $actual",
        )
      }
    }
  }

  private fun assertNoSignOnLine(editor: Editor, line: Int) {
    val onLine = signsIn(editor).filter { it.first == line }
    assertTrue(onLine.isEmpty(), "Expected no sign on line $line in ${editorLabel(editor)}, but found: $onLine")
  }

  private fun assertNoSigns() {
    assertTrue(signs().isEmpty(), "Expected no gutter signs, but found ${signs()}")
  }

  private fun editorLabel(editor: Editor): String =
    if (editor === fixture.editor) "main editor" else "split editor"
}
