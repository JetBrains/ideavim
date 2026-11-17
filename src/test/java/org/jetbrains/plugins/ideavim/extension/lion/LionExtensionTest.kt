/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.lion

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class LionExtensionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("lion")
  }

  @Test
  fun `test align paragraph by equals sign`() {
    doTest(
      "glip=",
      """
        |${c}a = 1
        |username = 'tommcdo'
        |stuff = 3
      """.trimMargin(),
      """
        |${c}a        = 1
        |username = 'tommcdo'
        |stuff    = 3
      """.trimMargin(),
    )
  }

  @Test
  fun `test align when last line has the rightmost char`() {
    doTest(
      "glip=",
      """
        |${c}a = 1
        |bb = 2
        |
        |ccc = 3
      """.trimMargin(),
      """
        |${c}a  = 1
        |bb = 2
        |
        |ccc = 3
      """.trimMargin(),
    )
  }

  @Test
  fun `test align all occurrences of the char`() {
    doTest(
      "glip=",
      """
        |${c}a = 1 = x
        |bb = 22 = y
        |c = 3
      """.trimMargin(),
      """
        |${c}a  = 1  = x
        |bb = 22 = y
        |c  = 3
      """.trimMargin(),
    )
  }

  @Test
  fun `test align later occurrences when earlier ones are already aligned`() {
    doTest(
      "glip=",
      """
        |${c}a = 1 = x
        |b = 22 = y
      """.trimMargin(),
      """
        |${c}a = 1  = x
        |b = 22 = y
      """.trimMargin(),
    )
  }

  @Test
  fun `test count limits number of aligned occurrences`() {
    doTest(
      "3glip=",
      """
        |${c}a = 1 = x = p = z
        |bb = 22 = yy = qq = w
        |
        |ccc = 3
      """.trimMargin(),
      """
        |${c}a  = 1  = x  = p = z
        |bb = 22 = yy = qq = w
        |
        |ccc = 3
      """.trimMargin(),
    )
  }

  @Test
  fun `test gL aligns text after the char`() {
    doTest(
      "gLip:",
      """
        |${c}a: 1
        |bbb: 2
      """.trimMargin(),
      """
        |${c}a:   1
        |bbb: 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test gL aligns text after the char when char is preceded by space`() {
    doTest(
      "gLip=",
      """
        |${c}a = 1
        |bb = 22
      """.trimMargin(),
      """
        |${c}a =  1
        |bb = 22
      """.trimMargin(),
    )
  }

  @Test
  fun `test gL aligns text directly after the char`() {
    doTest(
      "gLip:",
      """
        |${c}a:1
        |bbb:2
      """.trimMargin(),
      """
        |${c}a:  1
        |bbb:2
      """.trimMargin(),
    )
  }

  @Test
  fun `test dot repeats alignment with the same char`() {
    doTest(
      listOf("glip=", "}j", "."),
      """
        |${c}a = 1
        |bb = 2
        |
        |ccc = 3
        |d = 4
      """.trimMargin(),
      """
        |a  = 1
        |bb = 2
        |
        |${c}ccc = 3
        |d   = 4
      """.trimMargin(),
    )
  }

  @Test
  fun `test align linewise visual selection`() {
    doTest(
      "Vjgl=",
      """
        |${c}a = 1
        |bb = 2
        |ccc = 3
      """.trimMargin(),
      """
        |a  = 1
        |${c}bb = 2
        |ccc = 3
      """.trimMargin(),
    )
  }

  @Test
  fun `test align linewise visual selection ending on last line of file`() {
    doTest(
      "Vjgl=",
      """
        |${c}a = 1
        |bb = 2
      """.trimMargin(),
      """
        |a  = 1
        |${c}bb = 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test gL in linewise visual selection`() {
    doTest(
      "VjgL:",
      """
        |${c}a: 1
        |bbb: 2
      """.trimMargin(),
      """
        |a:   1
        |${c}bbb: 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test dot repeats visual alignment on the same number of lines from cursor`() {
    doTest(
      listOf("Vjgl=", "jj", "."),
      """
        |${c}a = 1
        |bb = 2
        |
        |ccc = 3
        |d = 4
      """.trimMargin(),
      """
        |a  = 1
        |bb = 2
        |
        |${c}ccc = 3
        |d   = 4
      """.trimMargin(),
    )
  }

  @Test
  fun `test align skips lines without the char`() {
    doTest(
      "glip=",
      """
        |${c}a = 1
        |no equals here
        |bb = 2
      """.trimMargin(),
      """
        |${c}a  = 1
        |no equals here
        |bb = 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test operator moves caret to start of the aligned text`() {
    doTest(
      "glip=",
      """
        |a = 1
        |bb ${c}= 2
      """.trimMargin(),
      """
        |${c}a  = 1
        |bb = 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test space aligns the first space after a word`() {
    doTest(
      "glip<Space>",
      """
        |${c}a  b
        |ccc d
      """.trimMargin(),
      """
        |${c}a    b
        |ccc d
      """.trimMargin(),
    )
  }

  @Test
  fun `test gL with space aligns the following word`() {
    doTest(
      "gLip<Space>",
      """
        |${c}a  b
        |ccc d
      """.trimMargin(),
      """
        |${c}a   b
        |ccc d
      """.trimMargin(),
    )
  }

  @Test
  fun `test number before char skips leading occurrences`() {
    doTest(
      "glip2,",
      """
        |${c}a,b,c
        |aa,bb,c
      """.trimMargin(),
      """
        |${c}a,b  ,c
        |aa,bb,c
      """.trimMargin(),
    )
  }

  @Test
  fun `test count with number before char aligns only that occurrence`() {
    doTest(
      "1glip2,",
      """
        |${c}a,b,c,d
        |aa,bb,cc,d
      """.trimMargin(),
      """
        |${c}a,b  ,c,d
        |aa,bb,cc,d
      """.trimMargin(),
    )
  }

  @Test
  fun `test number followed by enter aligns to the number`() {
    doTest(
      "glip2<CR>",
      """
        |${c}x2
        |yyy2
      """.trimMargin(),
      """
        |${c}x  2
        |yyy2
      """.trimMargin(),
    )
  }

  @Test
  fun `test slash aligns to a pattern`() {
    doTest(
      "glip/-><CR>",
      """
        |${c}a -> 1
        |bbb -> 2
      """.trimMargin(),
      """
        |${c}a   -> 1
        |bbb -> 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test slash aligns to a regular expression`() {
    doTest(
      "glip/[:=]<CR>",
      """
        |${c}a: 1
        |bbb = 2
      """.trimMargin(),
      """
        |${c}a   : 1
        |bbb = 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test slash with empty pattern aligns to slash`() {
    doTest(
      "glip/<CR>",
      """
        |${c}a/b
        |ccc/d
      """.trimMargin(),
      """
        |${c}a  /b
        |ccc/d
      """.trimMargin(),
    )
  }

  @Test
  fun `test invalid pattern leaves text unchanged`() {
    val text = """
        |${c}a(1
        |bbb(2
      """.trimMargin()
    doTest("glip/\\(<CR>", text, text)
  }

  @Test
  fun `test escape instead of char leaves text unchanged`() {
    val text = """
        |${c}a = 1
        |bbb = 2
      """.trimMargin()
    doTest("glip<Esc>", text, text)
  }

  @Test
  fun `test squeeze spaces before aligning keeps indent`() {
    configureByText(
      """
        |${c}    a     = 1
        |    bb = 2
      """.trimMargin(),
    )
    executeVimscript("let g:lion_squeeze_spaces = 1")
    typeText("glip=")
    assertState(
      """
        |${c}    a  = 1
        |    bb = 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test buffer squeeze spaces variable overrides global one`() {
    configureByText(
      """
        |${c}a     = 1
        |bb = 2
      """.trimMargin(),
    )
    executeVimscript("let g:lion_squeeze_spaces = 1")
    executeVimscript("let b:lion_squeeze_spaces = 0")
    typeText("glip=")
    assertState(
      """
        |${c}a     = 1
        |bb    = 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test tabs count as their screen width`() {
    configureByText("${c}a\t= 1\nbbbbbb = 2")
    ApplicationManager.getApplication().invokeAndWait { fixture.editor.settings.setTabSize(4) }
    typeText("glip=")
    assertState("${c}a\t   = 1\nbbbbbb = 2")
  }

  @Test
  fun `test characterwise visual selection limits the first and last line`() {
    doTest(
      "vjgl=",
      """
        |x = ${c}1 = 2
        |yy = 3 = 4
      """.trimMargin(),
      """
        |x = 1 = 2
        |yy  ${c}  = 3 = 4
      """.trimMargin(),
    )
  }

  @Test
  fun `test blockwise visual selection limits all lines to the block columns`() {
    doTest(
      "<C-V>2j4lgl=",
      """
        |a = ${c}1 = x
        |bb = 2 = y
        |c = 3 = z
      """.trimMargin(),
      """
        |a = 1  = x
        |bb = 2 = y
        |c = 3  =${c} z
      """.trimMargin(),
    )
  }

  @Test
  fun `test dot repeat keeps the count`() {
    doTest(
      listOf("3glip=", "}j", "."),
      """
        |${c}a = 1 = x = p = z
        |bb = 22 = yy = qq = w
        |
        |a = 1 = x = p = z
        |bb = 22 = yy = qq = w
      """.trimMargin(),
      """
        |a  = 1  = x  = p = z
        |bb = 22 = yy = qq = w
        |
        |${c}a  = 1  = x  = p = z
        |bb = 22 = yy = qq = w
      """.trimMargin(),
    )
  }
}
