/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.targets

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Port of the intent of targets.vim `s:testQuotes` (test7): quotes are paired by counting quote
 * characters from the start of the line, so the cursor's position relative to that count decides
 * which quote pair a command operates on. The upstream test sweeps 18 "rep" codes (e.g. `010`,
 * `102`) — `[parity of quotes left][quote under cursor][quotes right]` — across a multiline grid.
 *
 * Here we exercise the same classification with single-line fixtures (the multiline grid is the
 * upstream harness's way of generating the cases; the user-visible rule is the parity logic below).
 * Fixture: `x ' a ' b ' c ' d` pairs as `' a '` and `' c '`, with `b` sitting *between* the pairs.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsQuoteSeekTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // 0 quotes to the left (even), none under cursor: seek right into the first pair.
  @Test
  fun `even quotes left seeks right to first pair`() {
    doTest("ci'", "${c}x ' a ' b ' c ' d", "x '$c' b ' c ' d", Mode.INSERT)
  }

  // 1 quote to the left (odd): cursor is inside the first pair.
  @Test
  fun `odd quotes left means inside the current pair`() {
    doTest("ci'", "x ' ${c}a ' b ' c ' d", "x '$c' b ' c ' d", Mode.INSERT)
  }

  // 2 quotes to the left (even), cursor between the two pairs: skip the pseudo-quote and seek right.
  @Test
  fun `between pairs seeks right skipping the false quote`() {
    doTest("ci'", "x ' a ' ${c}b ' c ' d", "x ' a ' b '$c' d", Mode.INSERT)
  }

  // 3 quotes to the left (odd): cursor inside the second pair.
  @Test
  fun `odd quotes left inside the second pair`() {
    doTest("ci'", "x ' a ' b ' ${c}c ' d", "x ' a ' b '$c' d", Mode.INSERT)
  }

  // `n` works relative to the count of quotes: next pair after the current one.
  @Test
  fun `next quote from inside the first pair`() {
    doTest("cin'", "x ' ${c}a ' b ' c ' d", "x ' a ' b '$c' d", Mode.INSERT)
  }

  // `l` selects the previous pair.
  @Test
  fun `last quote from inside the second pair`() {
    doTest("cil'", "x ' a ' b ' ${c}c ' d", "x '$c' b ' c ' d", Mode.INSERT)
  }
}
