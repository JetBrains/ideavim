/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.textobjuser

import com.maddyhome.idea.vim.state.mode.Mode
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
}
