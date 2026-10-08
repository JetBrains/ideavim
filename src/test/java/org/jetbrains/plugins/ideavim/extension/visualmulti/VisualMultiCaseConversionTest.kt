/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.junit.jupiter.api.Test

/**
 * Case Conversion Menu (`\\C`): the case of each region, or of the word under each cursor
 */
class VisualMultiCaseConversionTest : VisualMultiTestCase() {

  /**
   * Converts both occurrences of [word] with the menu [key], and checks they are still selected
   */
  private fun assertConversion(word: String, key: String, converted: String) {
    configureByText("$c$word asd $word")

    typeText("<C-n><C-n>" + "<Bslash><Bslash>C" + key)

    assertState("${s}$converted$se asd ${s}$converted$se")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test lowercase`() = assertConversion("fooBar", "u", "foobar")

  @Test
  fun `test uppercase`() = assertConversion("fooBar", "U", "FOOBAR")

  @Test
  fun `test capitalize`() = assertConversion("fooBar", "C", "Foobar")

  @Test
  fun `test title case`() = assertConversion("foo_bar", "t", "Foo Bar")

  @Test
  fun `test camel case`() = assertConversion("foo_bar", "c", "fooBar")

  @Test
  fun `test pascal case`() = assertConversion("foo_bar", "P", "FooBar")

  @Test
  fun `test snake case`() = assertConversion("fooBar", "s", "foo_bar")

  @Test
  fun `test upper snake case`() = assertConversion("fooBar", "S", "FOO_BAR")

  @Test
  fun `test dash case`() = assertConversion("fooBar", "-", "foo-bar")

  @Test
  fun `test dot case`() = assertConversion("fooBar", ".", "foo.bar")

  @Test
  fun `test space case`() = assertConversion("fooBar", "<Space>", "foo bar")

  @Test
  fun `test converts word under each cursor`() {
    configureByText(
      """${c}fooBar
      |fooBar
      """.trimMargin(),
    )

    typeText("<C-Down>" + "<Bslash><Bslash>C" + "s")

    assertState(
      """${c}foo_bar
      |${c}foo_bar
      """.trimMargin(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test escape aborts case conversion`() {
    configureByText("${c}fooBar asd fooBar")

    typeText("<C-n><C-n>" + "<Bslash><Bslash>C" + "<Esc>")

    assertState("${s}fooBar$se asd ${s}fooBar$se")
  }

  @Test
  fun `test VM_maps changes Case Conversion Menu key`() {
    configureByText("${c}fooBar asd fooBar")
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Case Conversion Menu'] = '<C-c>'")

    typeText("<C-n><C-n>" + "<C-c>" + "s")

    assertState("${s}foo_bar$se asd ${s}foo_bar$se")
  }
}
