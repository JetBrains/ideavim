/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.autocmd

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
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
  }

  @Test
  fun `should execute command on InsertEnter`() {
    enterCommand("autocmd InsertEnter * echo 23")
    typeText(injector.parser.parseKeys("i"))
    assertExOutput("23")
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
  fun `should do nothing when pattern is not star`() {
    enterCommand("autocmd InsertEnter foo echo 23")
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
  fun `should support BuffEnter event`() {
    enterCommand("autocmd BuffEnter * echo 2")
    openNewBufferWindow("test.txt")
    assertExOutput("2")
  }

  private fun openNewBufferWindow(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, "0"))

      // But our selection changed callback doesn't get called immediately, and that callback will deactivate the ex entry
      // panel (which causes problems if our next command is `:set`). So type something (`0` is a good no-op) to give time
      // for the event to propagate
      typeText("0")
    }

    return fixture.editor

  }
}