/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionOuterBlockParenActionTest : VimTestCase() {
  // VIM-1633 |v_a)|
  @Test
  fun `test single letter with single parentheses`() {
    configureByText("(${c}a)")
    typeText(injector.parser.parseKeys("va)"))
    assertSelection("(a)")
  }

  @Test
  fun `test single letter with double parentheses`() {
    configureByText("((${c}a))")
    typeText(injector.parser.parseKeys("va)"))
    assertSelection("(a)")
  }

  @Test
  fun `test multiline outside parentheses`() {
    configureByText(
      """(outer
                      |$c(inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("va)"))
    assertSelection("(inner)")
  }

  @Test
  fun `test multiline in parentheses`() {
    configureByText(
      """(outer
                      |(inner$c))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("va)"))
    assertSelection("(inner)")
  }

  @Test
  fun `test multiline inside of outer parentheses`() {
    configureByText(
      """(outer
                     |$c (inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("va)"))
    assertSelection(
      """(outer
                        | (inner))
      """.trimMargin(),
    )
  }

  @Test
  fun `test double motion`() {
    configureByText(
      """(outer
                      |$c(inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("va)a)"))
    assertSelection(
      """(outer
                      |(inner))
      """.trimMargin(),
    )
  }

  @Test
  fun `test motion with count`() {
    configureByText(
      """(outer
                      |$c(inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("v2a)"))
    assertSelection(
      """(outer
                      |(inner))
      """.trimMargin(),
    )
  }

  @Test
  fun `test text object after motion`() {
    configureByText(
      """(outer
                      |$c(inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vlla)"))
    assertSelection(
      """(outer
                      |(inner))
      """.trimMargin(),
    )
  }

  @Test
  fun `test text object after motion outside parentheses`() {
    configureByText(
      """(outer
                      |(inner$c))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vlla)"))
    assertSelection("(inner)")
  }

  // |d| |v_ab|
  @Test
  fun testDeleteOuterBlock() {
    typeTextInFile(
      injector.parser.parseKeys("da)"),
      "foo(b${c}ar, baz);\n",
    )
    assertState("foo;\n")
  }
}
