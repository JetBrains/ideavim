/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.register.Register
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.annotations.TestWithPrimaryClipboard
import org.jetbrains.plugins.ideavim.annotations.TestWithoutPrimaryClipboard
import org.junit.jupiter.api.Test

class RegistersCommandTest : VimTestCase() {
  @TestWithoutPrimaryClipboard
  fun `test list empty registers`() {
    configureByText("")
    enterCommand("registers")
    assertExOutput("Type Name Content")
  }

  @Test
  fun `test argument filters output`() {
    configureByText("")

    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, injector.parser.parseKeys("Content for register $i"))
    }

    enterCommand("registers abc")
    assertExOutput(
      """Type Name Content
      |  c  "a   Content for register a
      |  c  "b   Content for register b
      |  c  "c   Content for register c
      """.trimMargin(),
    )
  }

  @Test
  fun `test argument allows spaces`() {
    configureByText("")

    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, injector.parser.parseKeys("Content for register $i"))
    }

    enterCommand("registers hello world")
    assertExOutput(
      """Type Name Content
      |  c  "d   Content for register d
      |  c  "e   Content for register e
      |  c  "h   Content for register h
      |  c  "l   Content for register l
      |  c  "o   Content for register o
      |  c  "r   Content for register r
      |  c  "w   Content for register w
      """.trimMargin(),
    )
  }

  @Test
  fun `test list nothing if no registers match`() {
    configureByText("")

    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, injector.parser.parseKeys("Content for register $i"))
    }

    enterCommand("registers Z")
    assertExOutput("Type Name Content")
  }

  @Test
  fun `test list truncates long registers`() {
    configureByText("")

    val indent = " ".repeat(20)
    val text = "Really long line ".repeat(1000)

    VimPlugin.getRegister().setKeys('a', injector.parser.parseKeys(indent + text))

    // Does not trim whitespace
    enterCommand("registers a")
    assertExOutput(
      """
        |Type Name Content
        |  c  "a   ${(indent + text).take(200)}
      """.trimMargin(),
    )
  }

  @TestWithoutPrimaryClipboard
  fun `test correctly encodes non printable characters`() {
    configureByText("")

    VimPlugin.getRegister().setKeys('a', injector.parser.parseKeys("<Tab>Hello<Space>World<CR><Esc>"))

    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
        |  c  "a   ^IHello World^J^[
      """.trimMargin(),
    )
  }

  @Test
  fun `test display synonym for registers command`() {
    configureByText("")

    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, injector.parser.parseKeys("Content for register $i"))
    }

    enterCommand("display abc")
    assertExOutput(
      """
        |Type Name Content
        |  c  "a   Content for register a
        |  c  "b   Content for register b
        |  c  "c   Content for register c
      """.trimMargin(),
    )
  }

  @TestWithoutPrimaryClipboard
  fun `test list all registers in correct order`() {
    configureByText(
      """"<caret>line 0
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
      """.trimMargin(),
    )

    // Populate unnamed "" and numbered "1-9 registers - linewise
    for (i in 1..10) {
      typeText(injector.parser.parseKeys("dd"))
    }

    // Last yank register "0 - "last yank"
    typeText(injector.parser.parseKeys("2yw" + "<CR>"))

    // Small delete register "- - deletes "s"
    typeText(injector.parser.parseKeys("x"))

    // Populate named registers "a-z - characterwise
    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, injector.parser.parseKeys("Hello world $i"))
    }

    // Clipboard registers "* "+
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val clipboardContent = injector.clipboardManager.dumbCopiedText("clipboard content")
    injector.clipboardManager.setClipboardContent(vimEditor, context, clipboardContent)

    // Last search register "/
    enterSearch("search pattern")

    enterCommand("ascii")

    // IdeaVim does not support:
    // ". last inserted text
    // "% current file name
    // "# alternate file name
    // "= expression register
    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
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
        |  c  ":   ascii
        |  c  "/   search pattern
      """.trimMargin(),
    )
  }

  @TestWithoutPrimaryClipboard
  fun `test clipboard registers are not duplicated`() {
    configureByText("<caret>line 0 ")

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.saveRegister(
      vimEditor,
      context,
      '+',
      Register('+', injector.clipboardManager.dumbCopiedText("Lorem ipsum dolor"), SelectionType.LINE_WISE)
    )
    val clipboardContent = injector.clipboardManager.dumbCopiedText("clipboard content")
    injector.clipboardManager.setClipboardContent(vimEditor, context, clipboardContent)
    typeText("V<Esc>")

    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
        |  c  "*   clipboard content
      """.trimMargin(),
    )
  }

  @TestWithoutPrimaryClipboard
  fun `test registers after yank with unnamed and unnamedplus`() {
    configureByText("<caret>line 0 ")
    enterCommand("set clipboard=unnamed,unnamedplus")

    typeText("ye")

    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
        |  c  ""   line
        |  c  "0   line
        |  c  "*   line
        |  c  ":   set clipboard=unnamed,unnamedplus
      """.trimMargin(),
    )
    enterCommand("set clipboard&")
  }

  @TestWithoutPrimaryClipboard
  fun `test registers after delete with unnamed and unnamedplus`() {
    configureByText("<caret>line 0 ")
    enterCommand("set clipboard=unnamed,unnamedplus")

    typeText("de")

    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
        |  c  ""   line
        |  c  "-   line
        |  c  "*   line
        |  c  ":   set clipboard=unnamed,unnamedplus
      """.trimMargin(),
    )
    enterCommand("set clipboard&")
  }

  @TestWithoutPrimaryClipboard
  fun `test registers for nonlinux with unnamedplus`() {
    configureByText("<caret>line 0 ")
    enterCommand("set clipboard=unnamedplus")

    typeText("de")

    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
        |  c  ""   line
        |  c  "-   line
        |  c  "*   line
        |  c  ":   set clipboard=unnamedplus
      """.trimMargin(),
    )
    enterCommand("set clipboard&")
  }

  @Test
  @TestWithPrimaryClipboard
  fun `test list empty registers linux`() {
    configureByText("")
    enterCommand("registers")
    assertExOutput("Type Name Content\n  c  \"*   ")
  }

  @TestWithPrimaryClipboard
  fun `test correctly encodes non printable characters linux`() {
    configureByText("")

    VimPlugin.getRegister().setKeys('a', injector.parser.parseKeys("<Tab>Hello<Space>World<CR><Esc>"))

    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
        |  c  "a   ^IHello World^J^[
        |  c  "*   
      """.trimMargin(),
    )
  }

  @TestWithPrimaryClipboard
  fun `test list all registers in correct order linux`() {
    configureByText(
      """"<caret>line 0
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
      """.trimMargin(),
    )

    // Populate unnamed "" and numbered "1-9 registers - linewise
    for (i in 1..10) {
      typeText(injector.parser.parseKeys("dd"))
    }

    // Last yank register "0 - "last yank"
    typeText(injector.parser.parseKeys("2yw" + "<CR>"))

    // Small delete register "- - deletes "s"
    typeText(injector.parser.parseKeys("x"))

    // Populate named registers "a-z - characterwise
    val registerGroup = VimPlugin.getRegister()
    for (i in 'a'..'z') {
      registerGroup.setKeys(i, injector.parser.parseKeys("Hello world $i"))
    }

    // Clipboard registers "* "+
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val clipboardContent = injector.clipboardManager.dumbCopiedText("clipboard content")
    injector.clipboardManager.setClipboardContent(vimEditor, context, clipboardContent)

    // Last search register "/
    enterSearch("search pattern")

    enterCommand("ascii")
    typeText("V<Esc>")

    // IdeaVim does not support:
    // ". last inserted text
    // "% current file name
    // "# alternate file name
    // "= expression register
    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
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
        |  c  "*   mall delete register
        |  c  "+   clipboard content
        |  c  ":   ascii
        |  c  "/   search pattern
      """.trimMargin(),
    )
  }

  @TestWithPrimaryClipboard
  fun `test clipboard registers are not duplicated linux`() {
    configureByText("<caret>line 0 ")

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val clipboardContent = injector.clipboardManager.dumbCopiedText("clipboard content")
    injector.registerGroup.saveRegister(
      vimEditor,
      context,
      '+',
      Register('+', injector.clipboardManager.dumbCopiedText("Lorem ipsum dolor"), SelectionType.LINE_WISE)
    )
    injector.clipboardManager.setClipboardContent(vimEditor, context, clipboardContent)
    typeText("V<Esc>")

    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
        |  c  "*   line 0 
        |  c  "+   clipboard content
      """.trimMargin(),
    )
  }

  @TestWithPrimaryClipboard
  fun `test registers after yank with unnamed and unnamedplus linux`() {
    configureByText("<caret>line 0 ")
    enterCommand("set clipboard=unnamed,unnamedplus")

    typeText("ye")

    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
        |  c  ""   line
        |  c  "0   line
        |  c  "*   line
        |  c  "+   line
        |  c  ":   set clipboard=unnamed,unnamedplus
      """.trimMargin(),
    )
    enterCommand("set clipboard&")
  }

  @TestWithPrimaryClipboard
  fun `test registers after delete with unnamed and unnamedplus linux`() {
    configureByText("<caret>line 0 ")
    enterCommand("set clipboard=unnamed,unnamedplus")

    typeText("de")

    enterCommand("registers")
    assertExOutput(
      """
        |Type Name Content
        |  c  ""   line
        |  c  "-   line
        |  c  "*   
        |  c  "+   line
        |  c  ":   set clipboard=unnamed,unnamedplus
      """.trimMargin(),
    )
    enterCommand("set clipboard&")
  }
}
