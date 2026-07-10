/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for the `v` forcing modifier used between an operator and a motion (`:help o_v`).
 *
 * `v` forces the operator to work characterwise and toggles the motion's inclusive/exclusive nature:
 *   - an exclusive charwise motion becomes inclusive (e.g. `w`)
 *   - an inclusive charwise motion becomes exclusive (e.g. `e`, `$`)
 *   - a linewise motion becomes exclusive charwise (e.g. `j`)
 */
class ForcedMotionVModifierTest : VimTestCase() {

  @Test
  fun `test dvw makes exclusive word motion inclusive`() {
    // `dw` deletes "hello " -> "world"; the extra `v` includes the landing char
    doTest(
      "dvw",
      "${c}hello world",
      "${c}orld",
    )
  }

  @Test
  fun `test dw is exclusive without v modifier`() {
    doTest(
      "dw",
      "${c}hello world",
      "${c}world",
    )
  }

  @Test
  fun `test dv dollar makes inclusive end-of-line exclusive`() {
    // `d$` deletes "hello"; `dv$` toggles to exclusive and leaves the last character
    doTest(
      "dv\$",
      "${c}hello",
      "${c}o",
    )
  }

  @Test
  fun `test dve makes inclusive word-end motion exclusive`() {
    // `de` deletes "hello" -> " world"; `dve` toggles to exclusive and leaves the last character of the word
    doTest(
      "dve",
      "${c}hello world",
      "${c}o world",
    )
  }

  @Test
  fun `test dvj forces linewise down motion to exclusive charwise`() {
    // `dj` deletes both lines; `dvj` deletes charwise (exclusive) to the same column on the next line
    doTest(
      "dvj",
      """
        ${c}abcde
        fghij
      """.trimIndent(),
      "${c}fghij",
    )
  }

  @Test
  fun `test cve enters insert mode with toggled charwise change`() {
    // `ce` changes "hello"; `cve` toggles to exclusive, changing "hell" and leaving "o world"
    doTest(
      "cveX",
      "${c}hello world",
      "X${c}o world",
      Mode.INSERT,
    )
  }

  @Test
  fun `test cvw honours forced motion through the cw-to-ce kludge`() {
    // Vim treats `cw` as `ce`, so `cvw` behaves like `cve`: the `v` toggles the (inclusive) word-end motion to
    // exclusive, changing "hell" and leaving "o world"
    doTest(
      "cvwX",
      "${c}hello world",
      "X${c}o world",
      Mode.INSERT,
    )
  }

  // ---------------------------------------------------------------------------------------------------------------
  // Edge cases
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `test dvf toggles inclusive find motion to exclusive`() {
    // `dfl` deletes "hel" (inclusive); `dvfl` toggles to exclusive and deletes "he"
    doTest(
      "dvfl",
      "${c}hello",
      "${c}llo",
    )
  }

  @Test
  fun `test dvt toggles inclusive till motion to exclusive`() {
    // `dtl` deletes "he" (inclusive up to just before 'l'); `dvtl` toggles to exclusive and deletes "h"
    doTest(
      "dvtl",
      "${c}hello",
      "${c}ello",
    )
  }

  @Test
  fun `test dv0 toggles exclusive start-of-line motion to inclusive`() {
    // `d0` deletes "hel" (exclusive); `dv0` toggles to inclusive and also removes the char under the caret
    doTest(
      "dv0",
      "hel${c}lo",
      "${c}o",
    )
  }

  @Test
  fun `test dvb toggles exclusive word-back motion to inclusive`() {
    // `db` deletes "wo" (exclusive); `dvb` toggles to inclusive and also removes the char under the caret
    doTest(
      "dvb",
      "hello wo${c}rld",
      "hello ${c}ld",
    )
  }

  @Test
  fun `test dv2w applies forced motion with a count`() {
    // `d2w` deletes "one two " (exclusive); `dv2w` toggles to inclusive and includes the landing char
    doTest(
      "dv2w",
      "${c}one two three",
      "${c}hree",
    )
  }

  @Test
  fun `test forced motion is repeated with dot`() {
    // `dvw` deletes "aa b" (w toggled to inclusive); `.` repeats the forced delete, removing "b c"
    doTest(
      "dvw.",
      "${c}aa bb cc dd",
      "${c}c dd",
    )
  }

  @Test
  fun `test yvw yanks the forced inclusive range`() {
    // `yw` yanks "hello " (exclusive); `yvw` toggles to inclusive and yanks "hello w", pasted after the caret
    doTest(
      "yvwp",
      "${c}hello world",
      "hhello ${c}wello world",
    )
  }

  @Test
  fun `test dviw forcing an already charwise text object is a no-op`() {
    // Forcing characterwise on an already-characterwise text object leaves it unchanged: `dviw` == `diw`
    doTest(
      "dviw",
      "${c}hello world",
      "${c} world",
    )
  }

  // ---------------------------------------------------------------------------------------------------------------
  // `V` — force linewise
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `test dVl forces charwise right motion to linewise`() {
    // `V` forces the characterwise `l` motion to linewise, deleting the whole line
    doTest(
      "dVl",
      "${c}hello\nworld",
      "${c}world",
    )
  }

  @Test
  fun `test dVe forces inclusive word-end motion to linewise`() {
    // `de` would delete just "hello"; `dVe` deletes the whole line
    doTest(
      "dVe",
      "${c}hello\nworld",
      "${c}world",
    )
  }

  @Test
  fun `test dV dollar forces end-of-line motion to linewise`() {
    doTest(
      "dV\$",
      "${c}hello\nworld",
      "${c}world",
    )
  }

  @Test
  fun `test dVj on already linewise motion stays linewise`() {
    // `j` is already linewise, so forcing linewise is a no-op: both lines are deleted, like `dj`
    doTest(
      "dVj",
      "${c}abc\ndef\nghi",
      "${c}ghi",
    )
  }

  @Test
  fun `test dVk forces upward motion to linewise`() {
    doTest(
      "dVk",
      "abc\n${c}def\nghi",
      "${c}ghi",
    )
  }

  @Test
  fun `test dViw forces charwise text object to linewise`() {
    // `diw` would delete just the inner word; `dViw` deletes the whole line
    doTest(
      "dViw",
      "${c}hello\nworld",
      "${c}world",
    )
  }

  @Test
  fun `test dV2j applies forced linewise motion with a count`() {
    doTest(
      "dV2j",
      "${c}aaa\nbbb\nccc\nddd",
      "${c}ddd",
    )
  }

  @Test
  fun `test cVl changes the whole line linewise`() {
    doTest(
      "cVlX",
      "${c}hello\nworld",
      "X${c}\nworld",
      Mode.INSERT,
    )
  }

  @Test
  fun `test yVl yanks the whole line linewise and pastes below`() {
    doTest(
      "yVlp",
      "${c}hello\nworld",
      "hello\n${c}hello\nworld",
    )
  }

  // ---------------------------------------------------------------------------------------------------------------
  // `CTRL-V` — force blockwise
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `test d CTRL-V l deletes a single-row block`() {
    // On one line the block is one row high: `l` forces blockwise over columns 0..1, deleting "he"
    doTest(
      "d<C-V>l",
      "${c}hello\nworld",
      "${c}llo\nworld",
    )
  }

  @Test
  fun `test d CTRL-V j deletes a vertical block`() {
    // Block from (0,0) to (1,0) - deletes the first column of both lines
    doTest(
      "d<C-V>j",
      "${c}abcde\nfghij",
      "${c}bcde\nghij",
    )
  }

  @Test
  fun `test c CTRL-V j changes a block on every line`() {
    // Blockwise change deletes the block, then repeats the inserted text on every line of the block on exit
    doTest(
      "c<C-V>jX<Esc>",
      "${c}abcde\nfghij",
      "${c}Xbcde\nXghij",
      Mode.NORMAL(),
    )
  }
}
