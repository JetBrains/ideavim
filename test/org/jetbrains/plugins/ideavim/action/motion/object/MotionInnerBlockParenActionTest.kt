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

class MotionInnerBlockParenActionTest : VimTestCase() {
  // VIM-1633 |v_i)|
  fun `test single letter with single parentheses`() {
    configureByText("(${c}a)")
    typeText(parseKeys("vi)"))
    assertSelection("a")
  }

  fun `test single letter with double parentheses`() {
    configureByText("((${c}a))")
    typeText(parseKeys("vi)"))
    assertSelection("(a)")
  }

  fun `test multiline outside parentheses`() {
    configureByText("""(outer
                        |${c}(inner))""".trimMargin())
    typeText(parseKeys("vi)"))
    assertSelection("inner")
  }

  fun `test multiline in parentheses`() {
    configureByText("""(outer
                        |(inner${c}))""".trimMargin())
    typeText(parseKeys("vi)"))
    assertSelection("inner")
  }

  fun `test multiline inside of outer parentheses`() {
    configureByText("""(outer
                         |${c} (inner))""".trimMargin())
    typeText(parseKeys("vi)"))
    assertSelection("""outer
                        | (inner)""".trimMargin())
  }

  fun `test double motion`() {
    configureByText("""(outer
                      |${c}(inner))""".trimMargin())
    typeText(parseKeys("vi)i)"))
    assertSelection("""outer
                          |(inner)""".trimMargin())
  }

  fun `test motion with count`() {
    configureByText("""(outer
                          |${c}(inner))""".trimMargin())
    typeText(parseKeys("v2i)"))
    assertSelection("""outer
                      |(inner)""".trimMargin())
  }

  fun `test text object after motion`() {
    configureByText("""(outer
                      |${c}(inner))""".trimMargin())
    typeText(parseKeys("vlli)"))
    assertSelection("""outer
                      |(inner)""".trimMargin())
  }

  fun `test text object after motion outside parentheses`() {
    configureByText("""(outer
                      |(inner${c}))""".trimMargin())
    typeText(parseKeys("vlli)"))
    assertSelection("inner")
  }

  fun `test text object after motion inside parentheses`() {
    configureByText("""(outer
                      |(${c}inner))""".trimMargin())
    typeText(parseKeys("vllli)"))
    assertSelection("inner")
  }

  // VIM-326 |d| |v_ib|
  fun testDeleteInnerBlock() {
    typeTextInFile(parseKeys("di)"),
      "foo(\"b${c}ar\")\n")
    myFixture.checkResult("foo()\n")
  }

  // VIM-1008 |d| |v_ib|
  fun testDeleteInnerBlockWithQuote() {
    typeTextInFile(parseKeys("di)"),
      "(abc${c}def'ghi)"
    )
    myFixture.checkResult("()")
  }

  // VIM-1008 |d| |v_ib|
  fun testDeleteInnerBlockWithDoubleQuote() {
    typeTextInFile(parseKeys("di)"),
      """(abc${c}def"ghi)"""
    )
    myFixture.checkResult("()")
  }

  // VIM-326 |d| |v_ib|
  fun testDeleteInnerBlockCaretBeforeString() {
    typeTextInFile(parseKeys("di)"),
      "foo(${c}\"bar\")\n")
    myFixture.checkResult("foo()\n")
  }

  // VIM-326 |c| |v_ib|
  fun testChangeInnerBlockCaretBeforeString() {
    typeTextInFile(parseKeys("ci)"),
      "foo(${c}\"bar\")\n")
    myFixture.checkResult("foo()\n")
  }

  // VIM-392 |c| |v_ib|
  fun testChangeInnerBlockCaretBeforeBlock() {
    typeTextInFile(parseKeys("ci)"),
      "foo${c}(bar)\n")
    myFixture.checkResult("foo()\n")
    assertOffset(4)
  }

  // |v_ib|
  fun testInnerBlockCrashWhenNoDelimiterFound() {
    typeTextInFile(parseKeys("di)"), "(x\n")
    myFixture.checkResult("(x\n")
  }

  // VIM-275 |d| |v_ib|
  fun testDeleteInnerParensBlockBeforeOpen() {
    typeTextInFile(parseKeys("di)"),
      "foo${c}(bar)\n")
    myFixture.checkResult("foo()\n")
    assertOffset(4)
  }

  // |d| |v_ib|
  fun testDeleteInnerParensBlockBeforeClose() {
    typeTextInFile(parseKeys("di)"),
      "foo(bar${c})\n")
    myFixture.checkResult("foo()\n")
  }
}
