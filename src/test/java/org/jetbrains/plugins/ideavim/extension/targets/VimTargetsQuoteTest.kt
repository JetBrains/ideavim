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
 * Port of targets.vim `s:testBasic` (test1) for the **quote** source.
 *
 * Trigger characters: `'`, `"`, `` ` ``. Expected results transcribed from `test/test1.ok`
 * (single-quote fixture). Note: as in the targets.vim harness, count 2 without an `n`/`l`
 * qualifier is unsupported for quotes and is intentionally not exercised.
 *
 * See the README "Quote Text Objects" section and `cheatsheet.md` quote chart.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsQuoteTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // Cursor on `x`, which lives inside the quote pair `' x '`.
  private val line = "a ' b ' c ' d ' e ' ${c}x ' g ' h ' i ' k ' l"

  @ParameterizedTest(name = "change quote: {0}")
  @MethodSource("quoteChangeCases")
  fun `change quote`(keys: String, after: String) {
    doTest(keys, line, after, Mode.INSERT)
  }

  // ---- the four modifiers, visual operator ----

  @Test
  fun `visual inside quote`() {
    doTest(
      "vi'",
      line,
      "a ' b ' c ' d ' e '$s x $se' g ' h ' i ' k ' l",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual Inside quote excludes whitespace`() {
    doTest(
      "vI'",
      line,
      "a ' b ' c ' d ' e ' ${s}x$se ' g ' h ' i ' k ' l",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual a quote has no surrounding whitespace`() {
    doTest(
      "va'",
      line,
      "a ' b ' c ' d ' e $s' x '$se g ' h ' i ' k ' l",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual Around quote eats trailing whitespace`() {
    doTest(
      "vA'",
      line,
      "a ' b ' c ' d ' e $s' x ' ${se}g ' h ' i ' k ' l",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ---- double quote and back tick triggers ----

  @Test
  fun `inside double quote`() {
    doTest(
      "ci\"",
      "a \" b \" c \" d \" e \" ${c}x \" g \" h \" i \" k \" l",
      "a \" b \" c \" d \" e \"$c\" g \" h \" i \" k \" l",
      Mode.INSERT,
    )
  }

  @Test
  fun `inside back tick`() {
    doTest(
      "ci`",
      "a ` b ` c ` d ` e ` ${c}x ` g ` h ` i ` k ` l",
      "a ` b ` c ` d ` e `$c` g ` h ` i ` k ` l",
      Mode.INSERT,
    )
  }

  // ---- smart quote pairing: quotes are counted from the start of the line, so `ci"` on the
  // comma between two strings changes the next string, not the junk in between (README example) ----

  @Test
  fun `smart quote skips false inside and changes next string`() {
    doTest(
      "ci\"",
      "join(\"hello\"$c, \"world\")",
      "join(\"hello\", \"$c\")",
      Mode.INSERT,
    )
  }

  // ---- quote seek: not inside a quote, seek on the current line ----

  @Test
  fun `seek to quote on line`() {
    doTest(
      "ci'",
      "a${c} ' bbbbbbbb ' c",
      "a '$c' c",
      Mode.INSERT,
    )
  }

  companion object {
    @JvmStatic
    fun quoteChangeCases(): List<Array<String>> {
      val c = VimTestCase.c
      return listOf(
        // last quote (`l`)
        arrayOf("cIl'", "a ' b ' c ' $c ' e ' x ' g ' h ' i ' k ' l"),
        arrayOf("cil'", "a ' b ' c '$c' e ' x ' g ' h ' i ' k ' l"),
        arrayOf("cal'", "a ' b ' c $c e ' x ' g ' h ' i ' k ' l"),
        arrayOf("cAl'", "a ' b ' c ${c}e ' x ' g ' h ' i ' k ' l"),
        // current quote
        arrayOf("cI'", "a ' b ' c ' d ' e ' $c ' g ' h ' i ' k ' l"),
        arrayOf("ci'", "a ' b ' c ' d ' e '$c' g ' h ' i ' k ' l"),
        arrayOf("ca'", "a ' b ' c ' d ' e $c g ' h ' i ' k ' l"),
        arrayOf("cA'", "a ' b ' c ' d ' e ${c}g ' h ' i ' k ' l"),
        // next quote (`n`)
        arrayOf("cIn'", "a ' b ' c ' d ' e ' x ' g ' $c ' i ' k ' l"),
        arrayOf("cin'", "a ' b ' c ' d ' e ' x ' g '$c' i ' k ' l"),
        arrayOf("can'", "a ' b ' c ' d ' e ' x ' g $c i ' k ' l"),
        arrayOf("cAn'", "a ' b ' c ' d ' e ' x ' g ${c}i ' k ' l"),
        // count 1 == no count
        arrayOf("c1i'", "a ' b ' c ' d ' e '$c' g ' h ' i ' k ' l"),
        // count 2 (only valid with l/n for quotes)
        arrayOf("c2Il'", "a ' $c ' c ' d ' e ' x ' g ' h ' i ' k ' l"),
        arrayOf("c2il'", "a '$c' c ' d ' e ' x ' g ' h ' i ' k ' l"),
        arrayOf("c2al'", "a $c c ' d ' e ' x ' g ' h ' i ' k ' l"),
        arrayOf("c2Al'", "a ${c}c ' d ' e ' x ' g ' h ' i ' k ' l"),
        arrayOf("c2In'", "a ' b ' c ' d ' e ' x ' g ' h ' i ' $c ' l"),
        arrayOf("c2in'", "a ' b ' c ' d ' e ' x ' g ' h ' i '$c' l"),
        arrayOf("c2an'", "a ' b ' c ' d ' e ' x ' g ' h ' i $c l"),
        arrayOf("c2An'", "a ' b ' c ' d ' e ' x ' g ' h ' i ${c}l"),
      )
    }
  }
}
