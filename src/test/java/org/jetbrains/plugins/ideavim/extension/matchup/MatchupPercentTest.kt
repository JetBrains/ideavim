/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchup

import com.intellij.ide.highlighter.HtmlFileType
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MatchupPercentTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchup")
  }

  @Test
  fun `test jump forward lands on the end of the middle word`() {
    doTest(
      "%",
      """
        ${c}if x
          a
        else
          b
        end
      """.trimIndent(),
      """
        if x
          a
        els${c}e
          b
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test jump forward lands on the start of the word without cursor end`() {
    doTest(
      "%",
      """
        ${c}if x
          a
        else
          b
        end
      """.trimIndent(),
      """
        if x
          a
        ${c}else
          b
        end
      """.trimIndent(),
      fileName = "ruby.rb",
      afterEditorInitialized = { enterCommand("let g:matchup_motion_cursor_end = 0") },
    )
  }

  @Test
  fun `test reverse jump from the opening word lands on the end of the closing word`() {
    doTest(
      "g%",
      """
        ${c}if x
          a
        else
          b
        end
      """.trimIndent(),
      """
        if x
          a
        else
          b
        en${c}d
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test jump forward in visual mode lands on the end of the word`() {
    doTest(
      "v%",
      """
        ${c}if x
          a
        else
          b
        end
      """.trimIndent(),
      """
        ${s}if x
          a
        els${c}e${se}
          b
        end
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test jump to html closing tag lands before its angle bracket`() {
    doTest(
      "%",
      """
        <${c}div>
          hello
        </div>
      """.trimIndent(),
      """
        <div>
          hello
        </di${c}v>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test delete over whole lines deletes the lines`() {
    doTest(
      "d%",
      """
        a
          ${c}if x
            b
          end
        c
      """.trimIndent(),
      """
        a
        ${c}c
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete with text after the closing word keeps the lines`() {
    doTest(
      "d%",
      """
        a
          ${c}if x
            b
          end; c
        d
      """.trimIndent(),
      """
        a
          ${c}; c
        d
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test forced characterwise delete leaves out the closing word`() {
    doTest(
      "dv%",
      """
        a
          ${c}if x
            b
          end
        c
      """.trimIndent(),
      """
        a
          ${c}end
        c
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test match beyond the stopline is not found`() {
    configureByText("")
    executeVimscript("let g:matchup_delim_stopline = 2")
    doTest("%", "${c}if x\n  a\n  b\n  c\nend", "${c}if x\n  a\n  b\n  c\nend", fileName = "test.rb")
  }

  @Test
  fun `test match within the stopline is found`() {
    configureByText("")
    executeVimscript("let g:matchup_delim_stopline = 4")
    doTest("%", "${c}if x\n  a\n  b\n  c\nend", "if x\n  a\n  b\n  c\nen${c}d", fileName = "test.rb")
  }

  @Test
  fun `test match back beyond the stopline is not found`() {
    configureByText("")
    executeVimscript("let g:matchup_delim_stopline = 2")
    doTest("%", "if x\n  a\n  b\n  c\n${c}end", "if x\n  a\n  b\n  c\n${c}end", fileName = "test.rb")
  }
}
