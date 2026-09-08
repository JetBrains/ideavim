/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualstarsearch

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for the vim-visual-star-search extension.
 *
 * See https://github.com/bronson/vim-visual-star-search
 *
 * The plugin makes `*` and `#` work in Visual mode: instead of searching for the word under the caret, they search
 * forwards/backwards for the selected text, taken literally (no `\<`/`\>` word boundaries, no magic characters).
 */
class VisualStarSearchTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("visual-star-search")
  }

  // ------------------------------------------------------------------------------------------------ core: * and #

  @Test
  fun `test star searches forwards for the selected text`() {
    doTest(
      "ve*",
      """
        ${c}hello world
        hello world
      """.trimIndent(),
      """
        hello world
        ${c}hello world
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test hash searches backwards for the selected text`() {
    doTest(
      "ve#",
      """
        hello world
        ${c}hello world
      """.trimIndent(),
      """
        ${c}hello world
        hello world
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star matches text without word boundaries`() {
    doTest(
      "ve*",
      """
        ${c}foo bar
        foobar baz
        foo end
      """.trimIndent(),
      """
        foo bar
        ${c}foobar baz
        foo end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star matches part of a word`() {
    doTest(
      "lvl*",
      """
        ${c}foo
        xoo
        foo
      """.trimIndent(),
      """
        foo
        x${c}oo
        foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star searches for selection containing spaces`() {
    doTest(
      "v6l*",
      """
        ${c}foo bar baz
        foo baz
        foo bar end
      """.trimIndent(),
      """
        foo bar baz
        foo baz
        ${c}foo bar end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star searches multiline selection`() {
    doTest(
      "vjll*",
      """
        ${c}foo
        bar
        baz
        foo
        bar
      """.trimIndent(),
      """
        foo
        bar
        baz
        ${c}foo
        bar
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star wraps around the end of the file`() {
    doTest(
      "ve*",
      """
        foo bar
        ${c}foo end
      """.trimIndent(),
      """
        ${c}foo bar
        foo end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test hash wraps around the start of the file`() {
    doTest(
      "ve#",
      """
        ${c}foo bar
        foo end
      """.trimIndent(),
      """
        foo bar
        ${c}foo end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // ------------------------------------------------------------------------------- the search pattern is remembered

  @Test
  fun `test n repeats the visual star search`() {
    doTest(
      "ve*n",
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      """
        foo bar
        foobar
        ${c}foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test N reverses the visual star search`() {
    doTest(
      "ve*nN",
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      """
        foo bar
        ${c}foobar
        foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test n continues backwards after hash`() {
    doTest(
      "ve#n",
      """
        foo one
        foo two
        ${c}foo three
      """.trimIndent(),
      """
        ${c}foo one
        foo two
        foo three
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // -------------------------------------------------------------------- the selection is searched for literally

  @Test
  fun `test dot in selection is taken literally`() {
    doTest(
      "vll*",
      """
        ${c}a.b
        axb
        a.b
      """.trimIndent(),
      """
        a.b
        axb
        ${c}a.b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star in selection is taken literally`() {
    doTest(
      "vll*",
      """
        ${c}a*b
        aab
        a*b
      """.trimIndent(),
      """
        a*b
        aab
        ${c}a*b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test square bracket in selection is taken literally`() {
    doTest(
      "v3l*",
      """
        ${c}x[0] = 1
        x[i] = 2
        x[0] = 3
      """.trimIndent(),
      """
        x[0] = 1
        x[i] = 2
        ${c}x[0] = 3
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test tilde in selection is taken literally`() {
    doTest(
      "vll*",
      """
        ${c}a~b
        axb
        a~b
      """.trimIndent(),
      """
        a~b
        axb
        ${c}a~b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test backslash in selection is taken literally`() {
    doTest(
      "vll*",
      """
        ${c}a\b
        axb
        a\b
      """.trimIndent(),
      """
        a\b
        axb
        ${c}a\b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test slash in selection does not break the forward search`() {
    doTest(
      "vll*",
      """
        ${c}a/b
        axb
        a/b
      """.trimIndent(),
      """
        a/b
        axb
        ${c}a/b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test question mark in selection does not break the backward search`() {
    doTest(
      "vll#",
      """
        a?b
        axb
        ${c}a?b
      """.trimIndent(),
      """
        ${c}a?b
        axb
        a?b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // ----------------------------------------------------------------------------------------------- no side effects

  @Test
  fun `test normal mode star still searches for the whole word`() {
    doTest(
      "*",
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      """
        foo bar
        foobar
        ${c}foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test unnamed register is not clobbered`() {
    doTest(
      "yiwve*P",
      """
        ${c}foo bar
        foo baz
      """.trimIndent(),
      """
        foo bar
        fo${c}ofoo baz
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}
