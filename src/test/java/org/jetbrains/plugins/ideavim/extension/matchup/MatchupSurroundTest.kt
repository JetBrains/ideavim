/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchup

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

@TestWithoutNeovim(SkipNeovimReason.PLUGIN)
class MatchupSurroundTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("")
    executeVimscript("let g:matchup_surround_enabled = 1")
    enableExtensions("matchup")
  }

  @Test
  fun `test delete surrounding words`() {
    doTest(
      "ds%",
      """
      |if x
      |  ${c}foo
      |end
      """.trimMargin(),
      """
      |$c x
      |  foo
      |
      """.trimMargin(),
      fileName = "test.rb",
    )
    assertRegister("if end")
  }

  @Test
  fun `test delete keeps middle words`() {
    doTest(
      "ds%",
      "if x\n  a\nel${c}se\n  b\nend",
      "$c x\n  a\nelse\n  b\n",
      fileName = "test.rb",
    )
  }

  @Test
  fun `test delete from the opening word`() {
    doTest("ds%", "${c}if x\nend", "$c x\n", fileName = "test.rb")
  }

  @Test
  fun `test delete surrounding brackets`() {
    doTest("ds%", "foo(b${c}ar)", "foo${c}bar", fileName = "test.rb")
  }

  @Test
  fun `test delete with count`() {
    doTest(
      "2ds%",
      "def foo\n  if x\n    ${c}a\n  end\nend",
      "$c foo\n  if x\n    a\n  end\n",
      fileName = "test.rb",
    )
  }

  @Test
  fun `test delete into a named register`() {
    doTest("\"ads%", "if x\n${c}a\nend", "$c x\na\n", fileName = "test.rb")
    assertEquals("if end", register('a'))
  }

  @Test
  fun `test delete without a surrounding block does nothing`() {
    doTest("ds%", "f${c}oo", "f${c}oo", fileName = "test.rb")
  }

  @Test
  fun `test repeat delete`() {
    doTest(
      "ds%.",
      "def foo\n  if x\n    ${c}a\n  end\nend",
      "$c foo\n   x\n    a\n  \n",
      fileName = "test.rb",
    )
  }

  @Test
  fun `test undo delete`() {
    doTest("ds%u", "if x\n${c}a\nend", "if x\n${c}a\nend", fileName = "test.rb")
  }

  @Test
  fun `test change to a bracket pair`() {
    doTest("cs%(", "if x\n${c}a\nend", "$c( x\na\n)", fileName = "test.rb")
  }

  @Test
  fun `test change with the closing bracket`() {
    doTest("cs%]", "if x\n${c}a\nend", "$c[ x\na\n]", fileName = "test.rb")
  }

  @Test
  fun `test change to another character`() {
    doTest("cs%\"", "foo(b${c}ar)", "foo$c\"bar\"", fileName = "test.rb")
  }

  @Test
  fun `test change is cancelled with escape`() {
    doTest("cs%<Esc>", "if x\n${c}a\nend", "if x\n${c}a\nend", fileName = "test.rb")
  }

  @Test
  fun `test change with surround`() {
    enableExtensions("surround")
    doTest("cs%)", "foo[b${c}ar]", "foo$c(bar)", fileName = "test.rb")
  }

  @Test
  fun `test change to a tag with surround`() {
    enableExtensions("surround")
    doTest("cs%tdiv<CR>", "foo(b${c}ar)", "foo$c<div>bar</div>", fileName = "test.rb")
  }

  @Test
  fun `test plug mappings without the default keys`() {
    executeVimscript("set nomatchup")
    executeVimscript("let g:matchup_surround_enabled = 0")
    executeVimscript("set matchup")
    executeVimscript("nmap <leader>d <Plug>(matchup-ds%)")
    doTest("\\d", "if x\n${c}a\nend", "$c x\na\n", fileName = "test.rb")
  }

  private fun assertRegister(expected: String) {
    assertEquals(expected, register('"'))
  }

  private fun register(name: Char): String? {
    val editor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    return injector.registerGroup.getRegister(editor, context, name)?.text
  }
}
