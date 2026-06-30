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

class IndentWisePreviousGreaterIndentTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("indentwise")
  }

  @Test
  fun `test previous greater indent`() {
    val before = """
      |def foo():
      |    if cond:
      |        bar()
      |    ${c}baz()
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        ${c}bar()
      |    baz()
    """.trimMargin()
    doTest("[+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous greater indent skips blank lines`() {
    val before = """
      |def foo():
      |    if cond:
      |        bar()
      |
      |    ${c}baz()
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        ${c}bar()
      |
      |    baz()
    """.trimMargin()
    doTest("[+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous greater indent skips lines of same or lesser indent`() {
    val before = """
      |        a()
      |b()
      |    c()
      |    ${c}d()
    """.trimMargin()
    val after = """
      |        ${c}a()
      |b()
      |    c()
      |    d()
    """.trimMargin()
    doTest("[+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous greater indent does nothing at first line`() {
    val before = """
      |        ${c}foo()
      |    bar()
    """.trimMargin()
    val after = """
      |        ${c}foo()
      |    bar()
    """.trimMargin()
    doTest("[+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous greater indent with count`() {
    val before = """
      |            deep()
      |        mid()
      |    ${c}shallow()
    """.trimMargin()
    val after = """
      |            ${c}deep()
      |        mid()
      |    shallow()
    """.trimMargin()
    doTest("2[+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous greater indent does nothing when only lesser indent above`() {
    val before = """
      |    x()
      |        ${c}y()
    """.trimMargin()
    val after = """
      |    x()
      |        ${c}y()
    """.trimMargin()
    doTest("[+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous greater indent skips whitespace-only lines`() {
    val before = "        bar()\n    \n    ${c}baz()"
    val after = "        ${c}bar()\n    \n    baz()"
    doTest("[+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test operator-pending delete is linewise and excludes the greater-indent line`() {
    val before = """
      |def foo():
      |    if cond:
      |        bar()
      |    a()
      |    ${c}b()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        bar()
      |    ${c}return
    """.trimMargin()
    doTest("d[+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test visual mode extends charwise selection to greater-indent line`() {
    val before = """
      |def foo():
      |    if cond:
      |        bar()
      |    ${c}baz()
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        ${s}${c}bar()
      |    b${se}az()
    """.trimMargin()
    doTest("v[+", before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }
}
