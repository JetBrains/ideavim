/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.textobjuser

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for a port of kana/vim-textobj-user.
 *
 * vim-textobj-user is a framework that lets users declaratively define their own text objects via
 * `textobj#user#plugin({name}, {specs})`. These tests exercise the two headline mechanisms from the
 * plugin's README:
 *  - a single "pattern" wired to `select` (the datetime example), and
 *  - a pair "pattern" wired to `select-a` / `select-i` (the braces example).
 *
 * See: https://github.com/kana/vim-textobj-user
 */
@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimTextObjUserExtensionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
    enableExtensions("textobj-user")
  }

  /**
   * Define `ad`/`id` to select a date such as `2013-03-16`.
   *
   * ```vim
   * call textobj#user#plugin('datetime', {
   * \   'date': {
   * \     'pattern': '\<\d\d\d\d-\d\d-\d\d\>',
   * \     'select': ['ad', 'id'],
   * \   },
   * \ })
   * ```
   */
  private fun defineDatetime() {
    executeVimscript(
      """
      call textobj#user#plugin('datetime', {
      \   'date': {
      \     'pattern': '\<\d\d\d\d-\d\d-\d\d\>',
      \     'select': ['ad', 'id'],
      \   },
      \ })
      """.trimIndent(),
      true,
    )
  }

  /**
   * Define `aA` to select text from `<<` to the matching `>>`, and
   * `iA` to select the text inside `<<` and `>>`.
   *
   * ```vim
   * call textobj#user#plugin('braces', {
   * \   'angle': {
   * \     'pattern': ['<<', '>>'],
   * \     'select-a': 'aA',
   * \     'select-i': 'iA',
   * \   },
   * \ })
   * ```
   */
  private fun defineBraces() {
    executeVimscript(
      """
      call textobj#user#plugin('braces', {
      \   'angle': {
      \     'pattern': ['<<', '>>'],
      \     'select-a': 'aA',
      \     'select-i': 'iA',
      \   },
      \ })
      """.trimIndent(),
      true,
    )
  }

  /**
   * Like [defineDatetime], but with a linewise `region-type`, so operators act on whole lines.
   *
   * ```vim
   * call textobj#user#plugin('datetime', {
   * \   'date': {
   * \     'pattern': '\<\d\d\d\d-\d\d-\d\d\>',
   * \     'select': ['ad', 'id'],
   * \     'region-type': 'V',
   * \   },
   * \ })
   * ```
   */
  private fun defineLinewiseDatetime() {
    executeVimscript(
      """
      call textobj#user#plugin('datetime', {
      \   'date': {
      \     'pattern': '\<\d\d\d\d-\d\d-\d\d\>',
      \     'select': ['ad', 'id'],
      \     'region-type': 'V',
      \   },
      \ })
      """.trimIndent(),
      true,
    )
  }

  /**
   * Like [defineBraces], but with an explicit charwise `region-type` (the default), used to confirm `'v'` behaves like
   * omitting the key.
   */
  private fun defineCharwiseBraces() {
    executeVimscript(
      """
      call textobj#user#plugin('braces', {
      \   'angle': {
      \     'pattern': ['<<', '>>'],
      \     'select-a': 'aA',
      \     'select-i': 'iA',
      \     'region-type': 'v',
      \   },
      \ })
      """.trimIndent(),
      true,
    )
  }

  /**
   * Define motions that jump between date objects: `]d` / `[d` to the beginning of the next / previous date, and
   * `]D` / `[D` to the end of the next / previous date.
   *
   * ```vim
   * call textobj#user#plugin('datetime', {
   * \   'date': {
   * \     'pattern': '\<\d\d\d\d-\d\d-\d\d\>',
   * \     'move-n': ']d',
   * \     'move-p': '[d',
   * \     'move-N': ']D',
   * \     'move-P': '[D',
   * \   },
   * \ })
   * ```
   */
  private fun defineDatetimeMotions() {
    executeVimscript(
      """
      call textobj#user#plugin('datetime', {
      \   'date': {
      \     'pattern': '\<\d\d\d\d-\d\d-\d\d\>',
      \     'move-n': ']d',
      \     'move-p': '[d',
      \     'move-N': ']D',
      \     'move-P': '[D',
      \   },
      \ })
      """.trimIndent(),
      true,
    )
  }

  @Test
  fun `select deletes the date under the cursor`() {
    defineDatetime()
    doTest(
      "dad",
      "released on 2013-<caret>03-16 today",
      "released on <caret> today",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `both select keys select the date`() {
    defineDatetime()
    // 'ad' and 'id' both map to the same <Plug> name, so both must be bound.
    doTest(
      "did",
      "released on 2013-<caret>03-16 today",
      "released on <caret> today",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `select finds the date forward from the cursor`() {
    defineDatetime()
    doTest(
      "dad",
      "<caret>released on 2013-03-16 today",
      "released on <caret> today",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `select-a includes the surrounding pattern pair`() {
    defineBraces()
    doTest(
      "daA",
      "prefix <<in<caret>ner>> suffix",
      "prefix <caret> suffix",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `select-i excludes the surrounding pattern pair`() {
    defineBraces()
    doTest(
      "diA",
      "prefix <<in<caret>ner>> suffix",
      "prefix <<<caret>>> suffix",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `linewise region-type makes the operator delete whole lines`() {
    defineLinewiseDatetime()
    doTest(
      "dad",
      """
      first line
      released on 2013-<caret>03-16 today
      third line
      """.trimIndent(),
      """
      first line
      <caret>third line
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `linewise region-type selects whole lines in visual mode`() {
    defineLinewiseDatetime()
    doTest(
      "vad",
      """
      first line
      released on 2013-<caret>03-16 today
      third line
      """.trimIndent(),
      """
      first line
      <selection>released on 2013-03-1<caret>6 today
      </selection>third line
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `explicit charwise region-type deletes only the match`() {
    defineCharwiseBraces()
    doTest(
      "daA",
      "prefix <<in<caret>ner>> suffix",
      "prefix <caret> suffix",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `move-n jumps to the beginning of the next match`() {
    defineDatetimeMotions()
    doTest(
      "]d",
      "2013-03-16 <caret>x 2014-04-17",
      "2013-03-16 x <caret>2014-04-17",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `move-p jumps to the beginning of the previous match`() {
    defineDatetimeMotions()
    doTest(
      "[d",
      "2013-03-16 <caret>x 2014-04-17",
      "<caret>2013-03-16 x 2014-04-17",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `move-N jumps to the end of the next match`() {
    defineDatetimeMotions()
    doTest(
      "]D",
      "2013-03-16 <caret>x 2014-04-17",
      "2013-03-16 x 2014-04-1<caret>7",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `move-P jumps to the end of the previous match`() {
    defineDatetimeMotions()
    doTest(
      "[D",
      "2013-03-16 <caret>x 2014-04-17",
      "2013-03-1<caret>6 x 2014-04-17",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `move-n works as an operator motion`() {
    defineDatetimeMotions()
    doTest(
      "d]d",
      "2013-03-16 <caret>x 2014-04-17",
      "2013-03-16 <caret>2014-04-17",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `move-n extends the selection in visual mode`() {
    defineDatetimeMotions()
    doTest(
      "v]d",
      "2013-03-16 <caret>x 2014-04-17",
      "2013-03-16 <selection>x <caret>2</selection>014-04-17",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `select is reachable through its Plug mapping`() {
    defineDatetime()
    // The bare "select" op yields <Plug>(textobj-datetime-date) (no operation suffix).
    executeVimscript("omap gd <Plug>(textobj-datetime-date)", true)
    doTest(
      "dgd",
      "released on 2013-<caret>03-16 today",
      "released on <caret> today",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `select-a is reachable through its Plug mapping`() {
    defineBraces()
    executeVimscript("omap gA <Plug>(textobj-braces-angle-a)", true)
    doTest(
      "dgA",
      "prefix <<in<caret>ner>> suffix",
      "prefix <caret> suffix",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `move-n is reachable through its Plug mapping`() {
    defineDatetimeMotions()
    executeVimscript("nmap gn <Plug>(textobj-datetime-date-n)", true)
    doTest(
      "gn",
      "2013-03-16 <caret>x 2014-04-17",
      "2013-03-16 x <caret>2014-04-17",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `textobj#user#map binds extra keys to an existing text object`() {
    defineDatetime()
    executeVimscript(
      """
      call textobj#user#map('datetime', {
      \   'date': {
      \     'select': 'gd',
      \   },
      \ })
      """.trimIndent(),
      true,
    )
    doTest(
      "dgd",
      "released on 2013-<caret>03-16 today",
      "released on <caret> today",
      Mode.NORMAL(),
    )
  }
}
