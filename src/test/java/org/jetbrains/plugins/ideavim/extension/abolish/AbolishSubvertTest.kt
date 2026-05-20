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
}
