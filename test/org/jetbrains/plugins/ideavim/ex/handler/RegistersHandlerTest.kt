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

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.TestClipboardModel
import org.jetbrains.plugins.ideavim.VimTestCase

class RegistersHandlerTest : VimTestCase() {
  override fun tearDown() {
    super.tearDown()
    TestClipboardModel.clearClipboard()
  }

  fun `test list empty registers`() {
    TestClipboardModel.clearClipboard()
    configureByText("")
    enterCommand("registers")
    assertExOutput("Type Name Content\n")
  }

  fun `test argument filters output`() {
    configureByText("")

    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, parseKeys("Content for register $i"))
    }

    enterCommand("registers abc")
    assertExOutput("""Type Name Content
      |  c  "a   Content for register a
      |  c  "b   Content for register b
      |  c  "c   Content for register c
    """.trimMargin())
  }

  fun `test argument allows spaces`() {
    configureByText("")

    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, parseKeys("Content for register $i"))
    }

    enterCommand("registers hello world")
    assertExOutput("""Type Name Content
      |  c  "d   Content for register d
      |  c  "e   Content for register e
      |  c  "h   Content for register h
      |  c  "l   Content for register l
      |  c  "o   Content for register o
      |  c  "r   Content for register r
      |  c  "w   Content for register w
    """.trimMargin())
  }

  fun `test list nothing if no registers match`() {
    configureByText("")

    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, parseKeys("Content for register $i"))
    }

    enterCommand("registers Z")
    assertExOutput("Type Name Content\n")
  }

  fun `test list truncates long registers`() {
    configureByText("")

    val indent = " ".repeat(20)
    val text = "Really long line ".repeat(1000)

    VimPlugin.getRegister().setKeys('a', parseKeys(indent + text))

    // Does not trim whitespace
    enterCommand("registers a")
    assertExOutput("""Type Name Content
                     |  c  "a   ${(indent + text).take(200)}
      """.trimMargin())
  }

  fun `test correctly encodes non printable characters`() {
    configureByText("")

    VimPlugin.getRegister().setKeys('a', parseKeys("<Tab>Hello<Space>World<CR><Esc>"))

    enterCommand("registers")
    assertExOutput("""Type Name Content
                     |  c  "a   ^IHello World^J^[
      """.trimMargin())
  }

  fun `test display synonym for registers command`() {
    configureByText("")

    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, parseKeys("Content for register $i"))
    }

    enterCommand("display abc")
    assertExOutput("""Type Name Content
      |  c  "a   Content for register a
      |  c  "b   Content for register b
      |  c  "c   Content for register c
    """.trimMargin())
  }

  fun `test list all registers in correct order`() {
    configureByText(""""<caret>line 0
      |line 1
      |line 2
      |line 3
      |line 4
      |line 5
      |line 6
      |line 7
      |line 8
      |line 9
      |last yank register
      |small delete register
    """.trimMargin())

    // Populate unnamed "" and numbered "1-9 registers - linewise
    for (i in 1..10) {
      typeText(parseKeys("dd"))
    }

    // Last yank register "0 - "last yank"
    typeText(parseKeys("2yw", "<CR>"))

    // Small delete register "- - deletes "s"
    typeText(parseKeys("x"))

    // Populate named registers "a-z - characterwise
    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, parseKeys("Hello world $i"))
    }

    // Clipboard registers "* "+
    TestClipboardModel.setClipboardText("clipboard content")

    // Last search register "/
    enterSearch("search pattern")

    enterCommand("ascii")

    // IdeaVim does not support:
    // ". last inserted text
    // "% current file name
    // "# alternate file name
    // "= expression register
    enterCommand("registers")
    assertExOutput("""Type Name Content
      |  c  ""   s
      |  c  "0   last yank 
      |  l  "1   line 9^J
      |  l  "2   line 8^J
      |  l  "3   line 7^J
      |  l  "4   line 6^J
      |  l  "5   line 5^J
      |  l  "6   line 4^J
      |  l  "7   line 3^J
      |  l  "8   line 2^J
      |  l  "9   line 1^J
      |  c  "a   Hello world a
      |  c  "b   Hello world b
      |  c  "c   Hello world c
      |  c  "d   Hello world d
      |  c  "e   Hello world e
      |  c  "f   Hello world f
      |  c  "g   Hello world g
      |  c  "h   Hello world h
      |  c  "i   Hello world i
      |  c  "j   Hello world j
      |  c  "k   Hello world k
      |  c  "l   Hello world l
      |  c  "m   Hello world m
      |  c  "n   Hello world n
      |  c  "o   Hello world o
      |  c  "p   Hello world p
      |  c  "q   Hello world q
      |  c  "r   Hello world r
      |  c  "s   Hello world s
      |  c  "t   Hello world t
      |  c  "u   Hello world u
      |  c  "v   Hello world v
      |  c  "w   Hello world w
      |  c  "x   Hello world x
      |  c  "y   Hello world y
      |  c  "z   Hello world z
      |  c  "-   s
      |  c  "*   clipboard content
      |  c  "+   clipboard content
      |  c  ":   ascii
      |  c  "/   search pattern
    """.trimMargin())
  }
}
