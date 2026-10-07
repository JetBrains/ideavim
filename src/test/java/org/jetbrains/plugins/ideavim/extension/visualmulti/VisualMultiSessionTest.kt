/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The session starts with the first region, like `b:visual_multi` in vim-visual-multi. `n`, `q`, `Q` and
 * `<Esc>` work on the regions only inside it.
 */
class VisualMultiSessionTest : VisualMultiTestCase() {

  @Test
  fun `test n finds next occurrence in session`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("<C-n><C-n>" + "n")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test q skips region in session`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("<C-n><C-n>" + "q")

    val after = """${s}qwe$se
      |asd
      |qwe
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test Q removes region in session`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("<C-n><C-n><C-n>" + "Q")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test session starts with first region`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    // A single region is already a session, so `n` finds the next occurrence
    typeText("<C-n>" + "n")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test session continues with one region left`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    // Q removes the second region, but the session goes on with the first one
    typeText("<C-n><C-n>" + "Q" + "n")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test session ends when last region is removed`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
    """.trimMargin()
    configureByText(before)
    enterSearch("asd")

    // After removing the last region, `n` in a new Visual selection is the Vim command again
    typeText("gg" + "<C-n>" + "Q" + "v" + "n")

    val after = """qw${s}e
      |${c}a${se}sd
      |qwe
      |asd
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test session ends when leaving Visual mode with one region`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
    """.trimMargin()
    configureByText(before)
    enterSearch("asd")

    // `v` leaves Visual mode, so the next `v` starts a plain Visual selection, where `n` is the Vim command
    typeText("gg" + "<C-n>" + "v" + "v" + "n")

    val after = """qw${s}e
      |${c}a${se}sd
      |qwe
      |asd
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test session continues in Normal mode with multiple cursors`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    // After changing the regions, the cursors are still in the session, so <Esc> exits it and keeps one cursor. Outside
    // the session, <Esc> would leave Visual mode and keep both cursors
    typeText("<C-n><C-n>" + "cfoo<Esc>" + "v" + "<Esc>")

    assertMode(Mode.NORMAL())
    assertEquals(1, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test Esc exits session`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("<C-n><C-n>" + "<Esc>")

    assertState(
      """qwe
      |asd
      |qw${c}e
      """.trimMargin(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test Esc outside session exits Visual mode`() {
    val before = "${c}foo bar"
    configureByText(before)

    typeText("vl" + "<Esc>")

    assertState("f${c}oo bar")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test carets added outside visual-multi are not a session`() {
    val before = """${c}qwe
      |qwe
      |qwe
    """.trimMargin()
    configureByText(before)
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.caretModel.addCaret(VisualPosition(1, 0))
    }

    // Like with Alt+click, the IDE adds the caret, and <Esc> leaves Visual mode keeping both carets
    typeText("vl" + "<Esc>")

    assertMode(Mode.NORMAL())
    assertEquals(2, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test VM_maps changes Exit key`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Exit'] = '<C-c>'")

    typeText("<C-n><C-n>" + "<C-c>")

    assertMode(Mode.NORMAL())
    assertEquals(1, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test n outside session keeps count`() {
    val before = "${c}foo bar foo bar foo"
    configureByText(before)
    enterSearch("bar")

    typeText("0" + "v2n")

    assertState("${s}foo bar foo ${c}b${se}ar foo")
  }

  @Test
  fun `test visual block is not a session`() {
    val before = """${c}qwe
      |qwe
      |qwe
    """.trimMargin()
    configureByText(before)
    enterSearch("e")

    // Visual block uses a native caret for each line, but `n` is still the Vim command extending the block
    typeText("gg" + "<C-v>j" + "n")

    assertMode(Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertEquals(2, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test session mapping does not override user mapping`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("xnoremap q <Esc>")

    typeText("<C-n><C-n>" + "q")

    // The mapping of the user is used, rather than Skip Region
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test VM_maps changes session mapping key`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Skip Region'] = '<C-x>'")

    typeText("<C-n><C-n>" + "<C-x>")

    val after = """${s}qwe$se
      |asd
      |qwe
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test session mapping key from VM_maps keeps its Vim meaning outside session`() {
    val before = "${c}1 foo"
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Skip Region'] = '<C-x>'")

    // Without a session, <C-x> decrements the number in the Visual selection
    typeText("v" + "<C-x>")

    assertState("${c}0 foo")
  }
}
