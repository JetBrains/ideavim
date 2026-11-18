/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.textobjquote

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimTextObjQuoteExtensionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("textobj-quote")
  }

  @Test
  fun `test delete inside curly double quotes`() {
    doTest(
      "diq",
      "She said “he${c}llo there” and left",
      "She said “${c}” and left",
    )
  }

  @Test
  fun `test delete around curly double quotes`() {
    doTest(
      "daq",
      "She said “he${c}llo there” and left",
      "She said ${c} and left",
    )
  }

  @Test
  fun `test delete inside curly double quotes with caret on opening quote`() {
    doTest(
      "diq",
      "She said ${c}“hello there” and left",
      "She said “${c}” and left",
    )
  }

  @Test
  fun `test delete inside curly double quotes with caret on closing quote`() {
    doTest(
      "diq",
      "She said “hello there${c}” and left",
      "She said “${c}” and left",
    )
  }

  @Test
  fun `test delete inside does nothing when caret is between two quoted strings`() {
    doTest(
      "diq",
      "“hello” and ${c}then “bye”",
      "“hello” and ${c}then “bye”",
    )
  }

  @Test
  fun `test delete inside selects outer pair of nested quotes`() {
    doTest(
      "diq",
      "She said “a${c}sk “why” first” and left",
      "She said “${c}” and left",
    )
  }

  @Test
  fun `test delete inside selects inner pair of nested quotes`() {
    doTest(
      "diq",
      "She said “ask “w${c}hy” first” and left",
      "She said “ask “${c}” first” and left",
    )
  }

  @Test
  fun `test delete inside in empty buffer does nothing`() {
    doTest(
      "diq",
      "${c}",
      "${c}",
    )
  }

  @Test
  fun `test delete inside on empty last line does nothing`() {
    doTest(
      "diq",
      "“hello”\n${c}",
      "“hello”\n${c}",
    )
  }

  @Test
  fun `test select inside curly double quotes`() {
    doTest(
      "viq",
      "She said “he${c}llo there” and left",
      "She said “${s}hello ther${c}e${se}” and left",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select around curly double quotes`() {
    doTest(
      "vaq",
      "She said “he${c}llo there” and left",
      "She said ${s}“hello there${c}”${se} and left",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test delete inside curly single quotes`() {
    doTest(
      "diQ",
      "She said ‘he${c}llo there’ and left",
      "She said ‘${c}’ and left",
    )
  }

  @Test
  fun `test delete around curly single quotes`() {
    doTest(
      "daQ",
      "She said ‘he${c}llo there’ and left",
      "She said ${c} and left",
    )
  }

  @Test
  fun `test delete inside single quotes skips apostrophe after caret`() {
    doTest(
      "diQ",
      "She said ‘I do${c}n’t know’ and left",
      "She said ‘${c}’ and left",
    )
  }

  @Test
  fun `test delete inside single quotes skips apostrophe before caret`() {
    doTest(
      "diQ",
      "She said ‘I don’t k${c}now’ and left",
      "She said ‘${c}’ and left",
    )
  }

  @Test
  fun `test delete inside single quotes closed before punctuation`() {
    doTest(
      "diQ",
      "‘he${c}llo there’, she said",
      "‘${c}’, she said",
    )
  }

  @Test
  fun `test delete inside quotes spanning multiple lines`() {
    doTest(
      "diq",
      """
        |She said “hello
        |wo${c}rld” and left
      """.trimMargin(),
      "She said “${c}” and left",
    )
  }

  @Test
  fun `test change inside curly double quotes`() {
    doTest(
      "ciqbye<Esc>",
      "She said “he${c}llo there” and left",
      "She said “by${c}e” and left",
    )
  }

  @Test
  fun `test repeat change inside curly double quotes`() {
    doTest(
      "ciqX<Esc>fb.",
      "“a${c}a” “bb”",
      "“X” “${c}X”",
    )
  }

  @Test
  fun `test select inside single quotes with apostrophes before and after caret`() {
    doTest(
      "viQ",
      "‘Really, I’d rather not re${c}live the ’70s,’ said zombie Elvis.",
      "‘${s}Really, I’d rather not relive the ’70s${c},${se}’ said zombie Elvis.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}
