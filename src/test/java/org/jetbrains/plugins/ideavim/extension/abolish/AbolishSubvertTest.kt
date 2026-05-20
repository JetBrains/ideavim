/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.abolish

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class AbolishSubvertTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("abolish")
  }

  @Test
  fun `S replaces all three case variants on the current line`() {
    doTest(
      ":S /foo/bar/g<CR>",
      "${c}foo and Foo and FOO end",
      "${c}bar and Bar and BAR end",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S with brace alternatives pairs singular and plural variants`() {
    doTest(
      ":S /box{,es}/bag{,s}/g<CR>",
      "${c}box Box BOX boxes Boxes BOXES",
      "${c}bag Bag BAG bags Bags BAGS",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S with placeholder reuses lhs alternatives in rhs`() {
    doTest(
      ":S /anomol{y,ies}/anomal{}/g<CR>",
      "${c}anomoly Anomoly ANOMOLY anomolies",
      "${c}anomaly Anomaly ANOMALY anomalies",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `Subvert long name works the same as S`() {
    doTest(
      ":Subvert /foo/bar/g<CR>",
      "${c}foo Foo FOO end",
      "${c}bar Bar BAR end",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S matches inside larger identifiers like substitute does`() {
    doTest(
      ":S /request/record/g<CR>",
      "${c}setRequestId(String requestId) REQUEST_ID",
      "${c}setRecordId(String recordId) RECORD_ID",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S without g flag replaces only the first match per line`() {
    doTest(
      ":S /foo/bar/<CR>",
      "${c}foo Foo FOO",
      "${c}bar Foo FOO",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S with two brace groups expands multiplicatively`() {
    doTest(
      ":S /{a,b}_{x,y}/{p,q}_{r,s}/g<CR>",
      "${c}a_x b_y a_y b_x",
      "${c}p_r q_s p_s q_r",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S on snake_case input also rewrites PascalCase occurrences`() {
    doTest(
      ":S /foo_bar/baz_qux/g<CR>",
      "${c}foo_bar and FooBar and FOO_BAR",
      "${c}baz_qux and BazQux and BAZ_QUX",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S cycles rhs alternatives when lhs has more of them`() {
    doTest(
      ":S /{red,green,blue,yellow}/{warm,cool}/g<CR>",
      "${c}red green blue yellow",
      "${c}warm cool warm cool",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S accepts no space between command and pattern`() {
    doTest(
      ":S/foo/bar/g<CR>",
      "${c}foo Foo FOO",
      "${c}bar Bar BAR",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S with c flag prompts before each match and applies according to user choice`() {
    doTest(
      listOf(":S/foo/bar/gc<CR>", "y", "n", "y"),
      "${c}foo Foo FOO",
      "${c}bar Foo BAR",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S without rhs searches forward across case variants`() {
    doTest(
      ":S/foo<CR>",
      "${c}begin FOO middle Foo end foo",
      "begin ${c}FOO middle Foo end foo",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `S with question mark delimiter searches backward`() {
    doTest(
      ":S?foo<CR>",
      "begin Foo middle FOO end ${c}some here",
      "begin Foo middle ${c}FOO end some here",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `n key navigates to the next match after a S search`() {
    doTest(
      listOf(":S/foo<CR>", "n"),
      "${c}begin FOO middle Foo end foo",
      "begin FOO middle ${c}Foo end foo",
      Mode.NORMAL(),
    )
  }

}
