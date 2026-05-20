/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.abolish

import com.maddyhome.idea.vim.extension.abolish.parseBraces
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BraceExpansionTest {

  @Test
  fun `a pattern with no braces is a single variant`() {
    val expansion = parseBraces("hello")
    assertEquals(listOf("hello"), expansion.materialise())
  }

  @Test
  fun `braces expand to one variant per alternative`() {
    val expansion = parseBraces("foo{a,b,c}bar")
    assertEquals(listOf("fooabar", "foobbar", "foocbar"), expansion.materialise())
  }

  @Test
  fun `empty alternative is allowed`() {
    val expansion = parseBraces("box{,es}")
    assertEquals(listOf("box", "boxes"), expansion.materialise())
  }

  @Test
  fun `empty braces have no alternatives of their own`() {
    val expansion = parseBraces("anomal{}")
    assertEquals(listOf("anomal"), expansion.materialise())
  }

  @Test
  fun `materialise with override substitutes external alternatives into the slot`() {
    val expansion = parseBraces("anomal{}")
    assertEquals(listOf("anomaly", "anomalies"), expansion.materialiseWith(listOf("y", "ies")))
  }

  @Test
  fun `materialiseWith on a literal pattern repeats it for each alternative`() {
    val expansion = parseBraces("hello")
    assertEquals(listOf("hello", "hello"), expansion.materialiseWith(listOf("a", "b")))
  }
}
