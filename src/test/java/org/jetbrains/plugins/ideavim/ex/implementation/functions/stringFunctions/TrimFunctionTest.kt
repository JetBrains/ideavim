/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.stringFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class TrimFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test trim basic whitespace`() {
    assertCommandOutput("echo trim('  hello  ')", "hello")
  }

  @Test
  fun `test trim leading whitespace only`() {
    assertCommandOutput("echo trim('  hello  ', '', 1)", "hello  ")
  }

  @Test
  fun `test trim trailing whitespace only`() {
    assertCommandOutput("echo trim('  hello  ', '', 2)", "  hello")
  }

  @Test
  fun `test trim with custom mask`() {
    assertCommandOutput("echo trim('xxhelloxx', 'x')", "hello")
  }

  @Test
  fun `test trim with multiple characters in mask`() {
    assertCommandOutput("echo trim('xyhelloxy', 'xy')", "hello")
  }

  @Test
  fun `test trim no whitespace`() {
    assertCommandOutput("echo trim('hello')", "hello")
  }

  @Test
  fun `test trim empty string`() {
    assertCommandOutput("echo trim('')", "")
  }

  @Test
  fun `test trim only whitespace`() {
    assertCommandOutput("echo trim('   ')", "")
  }

  @Test
  fun `test trim tabs and newlines`() {
    assertCommandOutput("echo trim('\t\nhello\n\t')", "hello")
  }

  @Test
  fun `test trim with custom mask does not affect middle`() {
    assertCommandOutput("echo trim('rm<Xrm<>X>rrm', 'rm<>')", "Xrm<>X")
  }

  @Test
  fun `test trim only trailing with direction 2`() {
    assertCommandOutput("echo trim('  vim  ', ' ', 2)", "  vim")
  }

  @Test
  fun `test trim with empty mask uses default`() {
    assertCommandOutput("echo trim('  hello  ', '')", "hello")
  }

  @Test
  fun `test trim with non-breaking space`() {
    assertCommandOutput("echo trim('\u00A0hello\u00A0')", "hello")
  }
}
