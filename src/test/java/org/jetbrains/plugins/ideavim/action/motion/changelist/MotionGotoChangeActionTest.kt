/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.changelist

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for VIM-519: cycling between recent edits with `g;` and `g,`.
 *
 * Reference: Neovim 0.11 — `nv_pcmark` / `get_changelist` (`src/nvim/normal.c`,
 * `src/nvim/mark.c`) and `changed_common` (`src/nvim/change.c`).
 *
 * Semantics in one breath:
 *  - Each undoable change appends an entry; the per-window index sits AT the
 *    position past the newest entry (so the first `g;` lands on the newest one).
 *  - `g;` walks backwards (`count` older), `g,` walks forwards (`count` newer).
 *  - Same-line edits within `'textwidth'` columns of the prior entry merge into
 *    a single entry at the latest position (default 79 when `'textwidth'` is 0).
 *  - Errors:
 *      E662 "At start of changelist"  — `g;` at the oldest entry
 *      E663 "At end of changelist"    — `g,` at the newest entry
 *      E664 "Changelist is empty"     — either command with no changes recorded
 *  - `g,` from the fresh "past end" position silently clamps to the newest
 *    entry; the error fires on the *next* `g,`.
 */
class MotionGotoChangeActionTest : VimTestCase() {

  @Test
  fun `test g_semicolon returns to last change after moving away`() {
    val before = """
      aaa
      ${c}bbb
      ccc
      ddd
      eee
    """.trimIndent()

    // Edit on line 2, jump to bottom, `g;` should bring us back.
    val keys = listOf("rA", "G\$", "g;")

    val after = """
      aaa
      ${c}Abb
      ccc
      ddd
      eee
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g_semicolon walks backwards through multiple changes`() {
    val before = """
      ${c}aaa
      bbb
      ccc
      ddd
    """.trimIndent()

    // Three changes on lines 1-3; G$ moves "past end"; first `g;` lands on the
    // newest (line 3, already where the cursor is); second `g;` lands on B.
    val keys = listOf("rA", "jrB", "jrC", "G\$", "g;", "g;")

    val after = """
      Aaa
      ${c}Bbb
      Ccc
      ddd
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g_semicolon then g_comma round trips`() {
    val before = """
      ${c}aaa
      bbb
      ccc
      ddd
    """.trimIndent()

    // Need at least 3 entries for a real round trip: the index sits past the
    // newest, so `g;` first hops to the newest, `g;` again to the middle, then
    // `g,` advances back to the newest entry.
    val keys = listOf("rA", "jrB", "jrC", "G\$", "g;", "g;", "g,")

    val after = """
      Aaa
      Bbb
      ${c}Ccc
      ddd
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g_semicolon with count walks back N entries from past-end`() {
    val before = """
      ${c}aaa
      bbb
      ccc
      ddd
    """.trimIndent()

    // 4 changes; index = 4 (past end). `3g;` → index 1 → entry B.
    // (Not the oldest -- "3 older" from past-end is the third-newest.)
    val keys = listOf("rA", "jrB", "jrC", "jrD", "3g;")

    val after = """
      Aaa
      ${c}Bbb
      Ccc
      Ddd
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g_semicolon with large count clamps to oldest change`() {
    val before = """
      ${c}aaa
      bbb
      ccc
      ddd
    """.trimIndent()

    val keys = listOf("rA", "jrB", "jrC", "jrD", "999g;")

    val after = """
      ${c}Aaa
      Bbb
      Ccc
      Ddd
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g_comma with count walks forward N entries`() {
    val before = """
      ${c}aaa
      bbb
      ccc
      ddd
    """.trimIndent()

    // Walk all the way to oldest first, then 2 newer → entry C.
    val keys = listOf("rA", "jrB", "jrC", "jrD", "999g;", "2g,")

    val after = """
      Aaa
      Bbb
      ${c}Ccc
      Ddd
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g_semicolon on buffer with no changes reports empty changelist`() {
    val before = """
      ${c}aaa
      bbb
    """.trimIndent()

    configureByText(before)
    typeText("g;")
    assertPluginError(true)
    assertStatusLineMessageContains("E664")
  }

  @Test
  fun `test g_semicolon past oldest entry reports start of changelist`() {
    val before = """
      ${c}aaa
      bbb
      ccc
    """.trimIndent()

    configureByText(before)
    // One change, then walk to it (oldest == newest), then try to go older.
    typeText("rA")
    typeText("G")
    typeText("g;")          // lands on the only entry; idx = 0
    typeText("g;")          // already at oldest → E662
    assertPluginError(true)
    assertStatusLineMessageContains("E662")
  }

  @Test
  fun `test g_comma past newest entry reports end of changelist`() {
    val before = """
      ${c}aaa
      bbb
    """.trimIndent()

    configureByText(before)
    // First `g,` after a single change silently clamps idx to the newest;
    // the second `g,` is the one that actually errors.
    typeText("rA")
    typeText("g,")
    typeText("g,")
    assertPluginError(true)
    assertStatusLineMessageContains("E663")
  }

  @Test
  fun `test nearby same line edits collapse into one change list entry`() {
    val before = "${c}abcdef"

    // Two single-character edits on the same line, well within 'textwidth'
    // (default 79). The two changes coalesce into a single entry sitting at
    // the *latest* position (column 1), so a second `g;` errors instead of
    // taking us back to column 0.
    configureByText(before)
    typeText("rA")
    typeText("lrB")
    typeText("G\$")
    typeText("g;")          // lands on the merged entry (col 1, on the 'B')
    assertState("A${c}Bcdef")
    typeText("g;")          // only one entry → E662
    assertPluginError(true)
    assertStatusLineMessageContains("E662")
  }

  @Test
  fun `test insert mode change is recorded at insert position`() {
    val before = """
      aaa
      ${c}bbb
      ccc
    """.trimIndent()

    val keys = listOf("iX", "<Esc>", "gg", "g;")

    val after = """
      aaa
      ${c}Xbbb
      ccc
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test append at end of line is recorded`() {
    val before = """
      aaa
      ${c}bbb
      ccc
    """.trimIndent()

    val keys = listOf("AZ", "<Esc>", "gg", "g;")

    // Cursor lands on the inserted Z (the recorded position).
    val after = """
      aaa
      bbb${c}Z
      ccc
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test line delete is recorded in change list`() {
    val before = """
      aaa
      ${c}bbb
      ccc
    """.trimIndent()

    val keys = listOf("dd", "gg", "g;")

    // After `dd` deletes line 2, the change is remembered at that line; `g;`
    // returns the cursor to the deletion site (which now holds "ccc").
    val after = """
      aaa
      ${c}ccc
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test new change after walking back does not truncate forward history`() {
    val before = """
      ${c}aaa
      bbb
      ccc
    """.trimIndent()

    // Make 3 changes, walk back to B, edit there. Unlike the jump list,
    // Vim's change list does NOT prune newer entries when a change is made
    // mid-list -- it just appends. So C must still be reachable via `g;`.
    //
    //   list before rX: [A@(1,1), B@(2,1), C@(3,1)]    idx=3 (past end)
    //   after G$ g;g; : idx=1, cursor on B
    //   after rX      : [A, B, C, X@(2,1)] idx=4 (past end)  -- C survives
    //   after 2g;     : idx=2, cursor on C (at line 3)
    val keys = listOf("rA", "jrB", "jrC", "G\$", "g;", "g;", "rX", "2g;")

    val after = """
      Aaa
      Xbb
      ${c}Ccc
    """.trimIndent()

    doTest(keys, before, after, Mode.NORMAL())
  }
}
