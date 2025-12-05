/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class FlipTest : VimTestCase() {

  @Test
  fun `test flip line when cursor inside word`() {
    val before = """The h${c}ello world""".trimIndent()
    configureByText(before)
    enterCommand("flip")
    val after = """The o${c}lleh world""".trimIndent()
    assertState(after)
  }

  @Test
  fun `should flip on multiline`() {
    val before = """
        Lorem ipsum
        The: h${c}ello world
      """.trimIndent()
    configureByText(before)
    enterCommand("flip")
    val after = """
        Lorem ipsum
        The: o${c}lleh world
      """.trimIndent().trimIndent()
    assertState(after)
  }

  @Test
  fun `should flip on yml`() {
    val before = """
      sql:
        init:
          mode: ne${c}ver
      """.trimIndent()
    configureByText(before)
    enterCommand("flip")
    val after = """
      sql:
        init:
          mode: re${c}ven
      """.trimIndent().trimIndent()
    assertState(after)
  }
}