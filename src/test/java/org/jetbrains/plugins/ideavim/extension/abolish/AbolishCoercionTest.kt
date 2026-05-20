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

class AbolishCoercionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("abolish")
  }

  @Test
  fun `crs converts camelCase under cursor to snake_case`() {
    doTest(
      "crs",
      "let helloW${c}orld = 1",
      "let ${c}hello_world = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crm converts snake_case under cursor to PascalCase`() {
    doTest(
      "crm",
      "let hello_${c}world = 1",
      "let ${c}HelloWorld = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crc converts snake_case under cursor to camelCase`() {
    doTest(
      "crc",
      "let hello_${c}world = 1",
      "let ${c}helloWorld = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `cru converts snake_case under cursor to UPPER_SNAKE`() {
    doTest(
      "cru",
      "let hello_${c}world = 1",
      "let ${c}HELLO_WORLD = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `cr- converts snake_case under cursor to kebab-case`() {
    doTest(
      "cr-",
      "let hello_${c}world = 1",
      "let ${c}hello-world = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `cr-dot converts snake_case under cursor to dot-case`() {
    doTest(
      "cr.",
      "let hello_${c}world = 1",
      "let ${c}hello.world = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crt converts snake_case under cursor to Title Case`() {
    doTest(
      "crt",
      "let hello_${c}world = 1",
      "let ${c}Hello World = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crs recases each word independently in multi-caret mode`() {
    doTest(
      "crs",
      "fooBa${c}r and bazQu${c}x",
      "${c}foo_bar and ${c}baz_qux",
      Mode.NORMAL(),
    )
  }
}
