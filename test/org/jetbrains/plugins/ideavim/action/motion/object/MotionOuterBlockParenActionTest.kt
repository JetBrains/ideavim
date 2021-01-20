/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionOuterBlockParenActionTest : VimTestCase() {
  // VIM-1633 |v_a)|
  fun `test single letter with single parentheses`() {
    configureByText("(${c}a)")
    typeText(parseKeys("va)"))
    assertSelection("(a)")
  }

  fun `test single letter with double parentheses`() {
    configureByText("((${c}a))")
    typeText(parseKeys("va)"))
    assertSelection("(a)")
  }

  fun `test multiline outside parentheses`() {
    configureByText("""(outer
                      |${c}(inner))""".trimMargin())
    typeText(parseKeys("va)"))
    assertSelection("(inner)")
  }

  fun `test multiline in parentheses`() {
    configureByText("""(outer
                      |(inner${c}))""".trimMargin())
    typeText(parseKeys("va)"))
    assertSelection("(inner)")
  }

  fun `test multiline inside of outer parentheses`() {
    configureByText("""(outer
                     |${c} (inner))""".trimMargin())
    typeText(parseKeys("va)"))
    assertSelection("""(outer
                        | (inner))""".trimMargin())
  }

  fun `test double motion`() {
    configureByText("""(outer
                      |${c}(inner))""".trimMargin())
    typeText(parseKeys("va)a)"))
    assertSelection("""(outer
                      |(inner))""".trimMargin())
  }

  fun `test motion with count`() {
    configureByText("""(outer
                      |${c}(inner))""".trimMargin())
    typeText(parseKeys("v2a)"))
    assertSelection("""(outer
                      |(inner))""".trimMargin())
  }

  fun `test text object after motion`() {
    configureByText("""(outer
                      |${c}(inner))""".trimMargin())
    typeText(parseKeys("vlla)"))
    assertSelection("""(outer
                      |(inner))""".trimMargin())
  }

  fun `test text object after motion outside parentheses`() {
    configureByText("""(outer
                      |(inner${c}))""".trimMargin())
    typeText(parseKeys("vlla)"))
    assertSelection("(inner)")
  }

  // |d| |v_ab|
  fun testDeleteOuterBlock() {
    typeTextInFile(parseKeys("da)"),
      "foo(b${c}ar, baz);\n")
    myFixture.checkResult("foo;\n")
  }
}
