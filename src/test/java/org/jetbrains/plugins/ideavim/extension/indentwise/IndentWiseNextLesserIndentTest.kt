/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.indentwise

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class IndentWiseNextLesserIndentTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("indentwise")
  }

  @Test
  fun `test next lesser indent`() {
    val before = """
      |def foo():
      |    if cond:
      |        ${c}bar()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        bar()
      |    ${c}return
    """.trimMargin()
    doTest("]-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next lesser indent skips blank lines`() {
    val before = """
      |def foo():
      |    if cond:
      |        ${c}bar()
      |
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        bar()
      |
      |    ${c}return
    """.trimMargin()
    doTest("]-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next lesser indent skips lines of same or greater indent`() {
    val before = """
      |def foo():
      |        ${c}bar()
      |        baz()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |        bar()
      |        baz()
      |    ${c}return
    """.trimMargin()
    doTest("]-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next lesser indent does nothing at last line`() {
    val before = """
      |def foo():
      |    ${c}bar()
    """.trimMargin()
    val after = """
      |def foo():
      |    ${c}bar()
    """.trimMargin()
    doTest("]-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next lesser indent with count`() {
    val before = """
      |def foo():
      |    if a:
      |        if b:
      |            ${c}bar()
      |        baz()
      |    qux()
    """.trimMargin()
    val after = """
      |def foo():
      |    if a:
      |        if b:
      |            bar()
      |        baz()
      |    ${c}qux()
    """.trimMargin()
    doTest("2]-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next lesser indent does nothing when only greater indent below`() {
    val before = """
      |    ${c}y()
      |        x()
    """.trimMargin()
    val after = """
      |    ${c}y()
      |        x()
    """.trimMargin()
    doTest("]-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next lesser indent skips whitespace-only lines`() {
    val before = "def foo():\n        ${c}bar()\n    \n    baz()"
    val after = "def foo():\n        bar()\n    \n    ${c}baz()"
    doTest("]-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test operator-pending delete is linewise and excludes the lesser-indent line`() {
    val before = """
      |def foo():
      |    if cond:
      |        ${c}a()
      |        b()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |    ${c}return
    """.trimMargin()
    doTest("d]-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test visual mode extends charwise selection to lesser-indent line`() {
    val before = """
      |def foo():
      |        ${c}bar()
      |    baz()
    """.trimMargin()
    val after = """
      |def foo():
      |        ${s}bar()
      |    ${c}b${se}az()
    """.trimMargin()
    doTest("v]-", before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }
}
