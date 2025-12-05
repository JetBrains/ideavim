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
      """.trimIndent()
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
      """.trimIndent()
    assertState(after)
  }

  @Test
  fun `should do nothing on empty document`() {
    val before = c
    configureByText(before)
    enterCommand("flip")
    val after = c
    assertState(after)
  }

  @Test
  fun `should do nothing when cursor not on a word - whitespace`() {
    val before = """The${c}  hello world""".trimIndent()
    configureByText(before)
    enterCommand("flip")
    val after = """The${c}  hello world""".trimIndent()
    assertState(after)
  }

  @Test
  fun `should do nothing when cursor not on a word - punctuation`() {
    val before = """The:${c} hello world""".trimIndent()
    configureByText(before)
    enterCommand("flip")
    val after = """The:${c} hello world""".trimIndent()
    assertState(after)
  }

  @Test
  fun `should do nothing on line with only whitespace`() {
    val before = """
      Lorem ipsum
      
      ${c}    
      Dolor sit amet
    """.trimIndent()
    configureByText(before)
    enterCommand("flip")
    val after = """
      Lorem ipsum
      
      ${c}    
      Dolor sit amet
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `should do nothing when cursor after last character on line`() {
    val before = """hello world${c}""".trimIndent()
    configureByText(before)
    enterCommand("flip")
    val after = """hello world${c}""".trimIndent()
    assertState(after)
  }

  @Test
  fun `should flip multiple cursors`() {
    val before = """
      Lorem i${c}psum
      Dolor sit a${c}met
    """.trimIndent()
    configureByText(before)
    enterCommand("flip")
    val after = """
      Lorem m${c}uspi
      Dolor sit t${c}ema
    """.trimIndent()
    assertState(after)
  }

}
