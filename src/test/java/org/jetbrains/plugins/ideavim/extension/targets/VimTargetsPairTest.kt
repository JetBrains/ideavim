/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.targets

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Port of targets.vim `s:testBasic` (test1) for the **pair** source.
 *
 * Trigger characters: `( ) { } B [ ] < >`. Modifiers `i a I A`, with the `n`/`l` next/last
 * qualifiers and counts. The canonical nested and the exact expected results are transcribed
 * from targets.vim's golden file `test/test1.ok` (first nested line). The `_` marker in the
 * golden file marks where the operated region collapses to; for the change operator that is the
 * caret in insert mode.
 *
 * See also the README "Pair Text Objects" section and `cheatsheet.md` pair charts.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsPairTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // Cursor sits on `x`, inside the innermost pair of `( ( x ) )`.
  private val nested = "a ( b ) ( c ) ( ( ${c}x ) ) ( e ) ( f ) g"

  @ParameterizedTest(name = "change pair: {0}")
  @MethodSource("pairChangeCases")
  fun `change pair`(keys: String, after: String) {
    doTest(keys, nested, after, Mode.INSERT)
  }

  // ---- the four modifiers, delete operator (caret lands on the trailing delimiter) ----

  @Test
  fun `delete inside pair`() {
    doTest("di(", nested, "a ( b ) ( c ) ( (${c}) ) ( e ) ( f ) g", Mode.NORMAL())
  }

  @Test
  fun `delete Inside pair excludes whitespace`() {
    doTest("dI(", nested, "a ( b ) ( c ) ( ( ${c} ) ) ( e ) ( f ) g", Mode.NORMAL())
  }

  @Test
  fun `delete a pair`() {
    doTest("da(", nested, "a ( b ) ( c ) ( ${c} ) ( e ) ( f ) g", Mode.NORMAL())
  }

  @Test
  fun `delete Around pair eats trailing whitespace`() {
    doTest("dA(", nested, "a ( b ) ( c ) ( ${c}) ( e ) ( f ) g", Mode.NORMAL())
  }

  // ---- the four modifiers, visual operator (selection region) ----

  @Test
  fun `visual inside pair`() {
    doTest(
      "vi(",
      nested,
      "a ( b ) ( c ) ( (${s} x ${se}) ) ( e ) ( f ) g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual Inside pair`() {
    doTest(
      "vI(",
      nested,
      "a ( b ) ( c ) ( ( ${s}x${se} ) ) ( e ) ( f ) g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual a pair`() {
    doTest(
      "va(",
      nested,
      "a ( b ) ( c ) ( ${s}( x )${se} ) ( e ) ( f ) g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual Around pair`() {
    doTest(
      "vA(",
      nested,
      "a ( b ) ( c ) ( ${s}( x ) ${se}) ( e ) ( f ) g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ---- the trigger aliases all behave identically (`)` and `b` == `(`) ----

  @Test
  fun `closing paren trigger is an alias`() {
    doTest("ci)", nested, "a ( b ) ( c ) ( (${c}) ) ( e ) ( f ) g", Mode.INSERT)
  }

  @Test
  fun `b trigger is any-block but here resolves to the surrounding paren`() {
    doTest("cib", nested, "a ( b ) ( c ) ( (${c}) ) ( e ) ( f ) g", Mode.INSERT)
  }

  // ---- curly / square / angle triggers ----

  @Test
  fun `inside curly braces`() {
    doTest(
      "ci{",
      "a { b { ${c}cccccccc } d } e",
      "a { b {${c}} d } e",
      Mode.INSERT,
    )
  }

  @Test
  fun `B trigger works on curly braces`() {
    doTest(
      "ciB",
      "a { b { ${c}cccccccc } d } e",
      "a { b {${c}} d } e",
      Mode.INSERT,
    )
  }

  @Test
  fun `inside square brackets`() {
    doTest(
      "ci[",
      "a [ b [ ${c}cccccccc ] d ] e",
      "a [ b [${c}] d ] e",
      Mode.INSERT,
    )
  }

  @Test
  fun `inside angle brackets`() {
    doTest(
      "ci<",
      "a < b < ${c}cccccccc > d > e",
      "a < b <${c}> d > e",
      Mode.INSERT,
    )
  }

  // ---- seek: when not inside a pair, the plain command seeks on the current line ----

  @Test
  fun `seek forward to next pair on line`() {
    doTest(
      "ci(",
      "a${c} ( bbbbbbbb ) c",
      "a (${c}) c",
      Mode.INSERT,
    )
  }

  @Test
  fun `seek backward to pair on line`() {
    doTest(
      "ci(",
      "a ( bbbbbbbb ) ${c}c",
      "a (${c}) c",
      Mode.INSERT,
    )
  }

  companion object {
    @JvmStatic
    fun pairChangeCases(): List<Array<String>> {
      val c = VimTestCase.c
      return listOf(
        // last pair on line (`l`)
        arrayOf("cIl(", "a ( b ) ( $c ) ( ( x ) ) ( e ) ( f ) g"),
        arrayOf("cil(", "a ( b ) ($c) ( ( x ) ) ( e ) ( f ) g"),
        arrayOf("cal(", "a ( b ) $c ( ( x ) ) ( e ) ( f ) g"),
        arrayOf("cAl(", "a ( b ) $c( ( x ) ) ( e ) ( f ) g"),
        // current pair (around cursor)
        arrayOf("cI(", "a ( b ) ( c ) ( ( $c ) ) ( e ) ( f ) g"),
        arrayOf("ci(", "a ( b ) ( c ) ( ($c) ) ( e ) ( f ) g"),
        arrayOf("ca(", "a ( b ) ( c ) ( $c ) ( e ) ( f ) g"),
        arrayOf("cA(", "a ( b ) ( c ) ( $c) ( e ) ( f ) g"),
        // next pair on line (`n`)
        arrayOf("cIn(", "a ( b ) ( c ) ( ( x ) ) ( $c ) ( f ) g"),
        arrayOf("cin(", "a ( b ) ( c ) ( ( x ) ) ($c) ( f ) g"),
        arrayOf("can(", "a ( b ) ( c ) ( ( x ) ) $c ( f ) g"),
        arrayOf("cAn(", "a ( b ) ( c ) ( ( x ) ) $c( f ) g"),
        // explicit count 1 == no count
        arrayOf("c1i(", "a ( b ) ( c ) ( ($c) ) ( e ) ( f ) g"),
        // count 2 grows outward
        arrayOf("c2Il(", "a ( $c ) ( c ) ( ( x ) ) ( e ) ( f ) g"),
        arrayOf("c2il(", "a ($c) ( c ) ( ( x ) ) ( e ) ( f ) g"),
        arrayOf("c2al(", "a $c ( c ) ( ( x ) ) ( e ) ( f ) g"),
        arrayOf("c2Al(", "a $c( c ) ( ( x ) ) ( e ) ( f ) g"),
        arrayOf("c2I(", "a ( b ) ( c ) ( $c ) ( e ) ( f ) g"),
        arrayOf("c2i(", "a ( b ) ( c ) ($c) ( e ) ( f ) g"),
        arrayOf("c2a(", "a ( b ) ( c ) $c ( e ) ( f ) g"),
        arrayOf("c2A(", "a ( b ) ( c ) $c( e ) ( f ) g"),
        arrayOf("c2In(", "a ( b ) ( c ) ( ( x ) ) ( e ) ( $c ) g"),
        arrayOf("c2in(", "a ( b ) ( c ) ( ( x ) ) ( e ) ($c) g"),
        arrayOf("c2an(", "a ( b ) ( c ) ( ( x ) ) ( e ) $c g"),
        arrayOf("c2An(", "a ( b ) ( c ) ( ( x ) ) ( e ) ${c}g"),
      )
    }
  }
}
