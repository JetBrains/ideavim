/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.autocmd

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class AutoCmdTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
    enterCommand("autocmd!")
    injector.outputPanel.getCurrentOutputPanel()?.close()
  }

  @Test
  fun `should execute command on InsertEnter`() {
    enterCommand("autocmd InsertEnter * echo \"hi\"")
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("hi")
  }


  @Test
  fun `should do nothing on invalid syntax`() {
    enterCommand("autocmd InsertEnter  echo 23")
    typeText(injector.parser.parseKeys("i"))
    assertNoExOutput()
  }

  @Test
  fun `should execute command on InsertLeave`() {
    enterCommand("autocmd InsertLeave * echo 23")
    typeText(injector.parser.parseKeys("i"))
    typeText(injector.parser.parseKeys("<esc>"))
    assertState(Mode.NORMAL())
    assertExOutput("23")
  }

  @Test
  fun `should clear commands`() {
    enterCommand("autocmd InsertEnter * echo 23")
    enterCommand("autocmd!")
    typeText(injector.parser.parseKeys("i"))
    assertNoExOutput()
  }

  @Test
  fun `should do nothing when pattern does not match file`() {
    enterCommand("autocmd InsertEnter *.py echo 23")
    typeText(injector.parser.parseKeys("i"))
    assertNoExOutput()
  }

  @Test
  fun `should execute command every time InsertEnter is triggered`() {
    enterCommand("autocmd InsertEnter * echo 23")

    typeText(injector.parser.parseKeys("i"))
    typeText(injector.parser.parseKeys("<esc>"))
    assertExOutput("23")

    typeText(injector.parser.parseKeys("i"))
    typeText(injector.parser.parseKeys("<esc>"))
    assertExOutput("23")
  }

  @Test
  fun `should not execute InsertLeave command if insert mode is not left`() {
    enterCommand("autocmd InsertLeave * echo 23")
    typeText(injector.parser.parseKeys("i"))
    assertNoExOutput()
  }

  @Test
  fun `should execute multiple handlers for same event`() {
    enterCommand("autocmd InsertEnter * echo 1")
    enterCommand("autocmd InsertEnter * echo 2")

    typeText(injector.parser.parseKeys("i"))

    assertExOutput("1\n2")
  }

  @Test
  fun `should execute only matching event handlers`() {
    enterCommand("autocmd InsertEnter * echo 1")
    enterCommand("autocmd InsertLeave * echo 2")

    typeText(injector.parser.parseKeys("i"))
    assertExOutput("1")

    typeText(injector.parser.parseKeys("<esc>"))
    assertExOutput("2")
  }

  @Test
  fun `autocmd bang should clear all event handlers`() {
    enterCommand("autocmd InsertEnter * echo 1")
    enterCommand("autocmd InsertLeave * echo 2")

    enterCommand("autocmd!")

    typeText(injector.parser.parseKeys("i"))
    typeText(injector.parser.parseKeys("<esc>"))

    assertNoExOutput()
  }

  @Test
  fun `should execute InsertEnter when entering insert from visual mode with c`() {
    configureByText("hello world")
    enterCommand("autocmd InsertEnter * echo \"entering insert\"")

    typeText(injector.parser.parseKeys("viw"))  // select word
    typeText(injector.parser.parseKeys("c"))    // change (enters insert)

    assertExOutput("entering insert")
    assertState(Mode.INSERT)
  }

  @Test
  fun `should execute InsertEnter when entering insert from visual mode with s`() {
    configureByText("hello world")
    enterCommand("autocmd InsertEnter * echo \"substitute\"")

    typeText(injector.parser.parseKeys("viw"))  // select word
    typeText(injector.parser.parseKeys("s"))    // substitute (enters insert)

    assertExOutput("substitute")
    assertState(Mode.INSERT)
  }

  @Test
  fun `should execute InsertLeave after entering from visual mode`() {
    configureByText("hello world")
    enterCommand("autocmd InsertLeave * echo \"leaving insert\"")

    typeText(injector.parser.parseKeys("viw"))  // select word
    typeText(injector.parser.parseKeys("c"))    // change (enters insert)
    typeText(injector.parser.parseKeys("<esc>"))

    assertExOutput("leaving insert")
    assertState(Mode.NORMAL())
  }

  @Test
  fun `should execute both InsertEnter and InsertLeave from visual mode`() {
    configureByText("hello world")
    enterCommand("autocmd InsertEnter * echo \"enter\"")
    enterCommand("autocmd InsertLeave * echo \"leave\"")

    typeText(injector.parser.parseKeys("viw"))  // select word
    typeText(injector.parser.parseKeys("c"))    // change (enters insert)
    assertExOutput("enter")

    typeText(injector.parser.parseKeys("<esc>"))
    assertExOutput("leave")
  }

  @Test
  fun `should register multiple events with comma-separated syntax`() {
    enterCommand("autocmd InsertEnter,InsertLeave * echo \"triggered\"")

    typeText(injector.parser.parseKeys("i"))
    assertExOutput("triggered")

    typeText(injector.parser.parseKeys("<esc>"))
    assertExOutput("triggered")
  }

  @Test
  fun `should handle spaces around commas in multiple events`() {
    enterCommand("autocmd InsertEnter , InsertLeave * echo \"triggered\"")

    typeText(injector.parser.parseKeys("i"))
    assertExOutput("triggered")

    typeText(injector.parser.parseKeys("<esc>"))
    assertExOutput("triggered")
  }

  @Test
  fun `should register three events with comma-separated syntax`() {
    configureByText("hello")
    enterCommand("autocmd InsertEnter,InsertLeave,BuffEnter * echo \"event\"")

    // InsertEnter
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("event")

    // InsertLeave
    typeText(injector.parser.parseKeys("<esc>"))
    assertExOutput("event")
  }

  @Test
  fun `should fail gracefully with invalid event in comma-separated list`() {
    enterCommand("autocmd InsertEnter,InvalidEvent * echo \"test\"")
    typeText(injector.parser.parseKeys("i"))
    assertNoExOutput()
  }

  @Test
  fun `should match file extension pattern`() {
    configureByFileName("test.txt")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter *.txt echo \"matched\"")
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("matched")
  }

  @Test
  fun `should not match wrong file extension pattern`() {
    configureByFileName("test.txt")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter *.py echo \"matched\"")
    typeText(injector.parser.parseKeys("i"))
    assertNoExOutput()
  }

  @Test
  fun `should match brace alternation pattern`() {
    configureByFileName("test.txt")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter *.{py,txt} echo \"matched\"")
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("matched")
  }

  @Test
  fun `should not match brace alternation when extension not listed`() {
    configureByFileName("test.kt")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter *.{py,txt} echo \"matched\"")
    typeText(injector.parser.parseKeys("i"))
    assertNoExOutput()
  }

  @Test
  fun `should match question mark single char pattern`() {
    configureByFileName("test.py")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter *.?y echo \"matched\"")
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("matched")
  }

  @Test
  fun `should match exact filename pattern`() {
    configureByFileName("Makefile")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter Makefile echo \"matched\"")
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("matched")
  }

  @Test
  fun `should not match different exact filename`() {
    configureByFileName("Rakefile")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter Makefile echo \"matched\"")
    typeText(injector.parser.parseKeys("i"))
    assertNoExOutput()
  }

  @Test
  fun `should match star pattern on any file`() {
    configureByFileName("anything.xyz")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter * echo \"matched\"")
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("matched")
  }

  @Test
  fun `should execute only commands with matching pattern`() {
    configureByFileName("test.py")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter *.py echo \"python\"")
    enterCommand("autocmd InsertEnter *.txt echo \"text\"")
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("python")
  }

  @Test
  fun `should execute commands with star and specific pattern`() {
    configureByFileName("test.py")
    enterCommand("autocmd!")
    enterCommand("autocmd InsertEnter * echo \"all\"")
    enterCommand("autocmd InsertEnter *.py echo \"python\"")
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("all\npython")
  }
}