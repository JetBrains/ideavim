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
