/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SublistExpressionTest : VimTestCase("\n") {

  @Test
  fun `test string sublist`() {
    assertCommandOutput("echo 'abc'[0:1]", "ab")
  }

  @Test
  fun `test negative index with string`() {
    assertCommandOutput("echo 'abc'[-1]", "")
  }

  @Test
  fun `test index greater than size with string`() {
    assertCommandOutput("echo 'abc'[1000]", "")
  }

  // TODO: This (and above) are indexed expressions, not sublist expressions
  @Test
  fun `test negative index with list`() {
    assertCommandOutput("echo [1, 2][-1]", "2")
  }

  @Test
  fun `test index greater than size with list`() {
    enterCommand("echo [1, 2][1000]")
    assertPluginErrorMessage("E684: List index out of range: 1000")
  }

  @Test
  fun `test list with correct index`() {
    assertCommandOutput("echo [1, 2][1]", "2")
  }

  @Test
  fun `test negative first index`() {
    assertCommandOutput("echo 'abc'[-1:]", "c")
  }

  @Test
  fun `test negative last index`() {
    assertCommandOutput("echo 'abc'[0:-2]", "ab")
  }

  @Test
  fun `test negative last index2`() {
    assertCommandOutput("echo 'abc'[0:-1]", "abc")
  }

  @Test
  fun `test last index bigger sting size`() {
    assertCommandOutput("echo 'abc'[1:10000]", "bc")
  }

  @Test
  fun `test both indexes bigger sting size`() {
    assertCommandOutput("echo 'abc'[100:10000]", "")
  }

  @Test
  fun `test first index is bigger than second`() {
    assertCommandOutput("echo 'abc'[100:10]", "")
  }
}
