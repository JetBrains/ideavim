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
 * Port of targets.vim `s:testBasic` (test1 tag fixture) and `s:testModifiers` (test5) for the
 * **tag** source. Trigger `t` (`it at It At`). Expected results transcribed from
 * `test/test1.ok` and `test/test5.ok`.
 *
 * See the README "Pair Text Objects" section (tags are listed there) and `cheatsheet.md`.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsTagTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // Cursor on `x`, inside `<d> x </d>`, itself nested in `<c> ... </c>`.
  private val line =
    "a <a> b </a> <b> c </b> <c> <d> ${c}x </d> </c> <e> e </e> <f> f </f> g"

  @ParameterizedTest(name = "change tag: {0}")
  @MethodSource("tagChangeCases")
  fun `change tag`(keys: String, after: String) {
    doTest(keys, line, after, Mode.INSERT)
  }

  // ---- the four modifiers, visual operator ----

  @Test
  fun `visual inside tag`() {
    doTest(
      "vit",
      line,
      "a <a> b </a> <b> c </b> <c> <d>$s x $se</d> </c> <e> e </e> <f> f </f> g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual a tag`() {
    doTest(
      "vat",
      line,
      "a <a> b </a> <b> c </b> <c> $s<d> x </d>$se </c> <e> e </e> <f> f </f> g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ---- test5: Inside tag selects the content without surrounding whitespace ----

  @Test
  fun `visual Inside tag selects bare content`() {
    doTest(
      "vIt",
      "<a>a${c}xb</a>",
      "<a>${s}axb$se</a>",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  companion object {
    @JvmStatic
    fun tagChangeCases(): List<Array<String>> {
      val c = VimTestCase.c
      val tail = "</c> <e> e </e> <f> f </f> g"
      val head = "a <a> b </a> <b> c </b>"
      return listOf(
        // current tag (around cursor): <d> ... </d>
        arrayOf("cit", "$head <c> <d>$c</d> $tail"),
        arrayOf("cIt", "$head <c> <d> $c </d> $tail"),
        arrayOf("cat", "$head <c> $c </c> <e> e </e> <f> f </f> g"),
        arrayOf("cAt", "$head <c> $c</c> <e> e </e> <f> f </f> g"),
        // last tag (`l`): the <b> ... </b> before the cursor
        arrayOf("cilt", "a <a> b </a> <b>$c</b> <c> <d> x </d> $tail"),
        arrayOf("cIlt", "a <a> b </a> <b> $c </b> <c> <d> x </d> $tail"),
        arrayOf("calt", "a <a> b </a> $c <c> <d> x </d> $tail"),
        arrayOf("cAlt", "a <a> b </a> $c<c> <d> x </d> $tail"),
        // next tag (`n`): the <e> ... </e> after the cursor
        arrayOf("cint", "$head <c> <d> x </d> </c> <e>$c</e> <f> f </f> g"),
        arrayOf("cInt", "$head <c> <d> x </d> </c> <e> $c </e> <f> f </f> g"),
        arrayOf("cant", "$head <c> <d> x </d> </c> $c <f> f </f> g"),
        arrayOf("cAnt", "$head <c> <d> x </d> </c> $c<f> f </f> g"),
      )
    }
  }
}
