/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.newapi.IjVimMessages
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// VIM-485 |'errorbells'| |'noerrorbells'|
@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class ErrorbellsOptionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private fun errorbells() = injector.options(fixture.editor.vim).errorbells

  @Test
  fun `test noerrorbells is the default, as in Vim`() {
    assertFalse(errorbells())
  }

  @Test
  fun `test noerrorbells is accepted`() {
    enterCommand("set noerrorbells")
    assertPluginError(false)
    assertFalse(errorbells())
  }

  @Test
  fun `test errorbells can be re-enabled`() {
    enterCommand("set noerrorbells")
    enterCommand("set errorbells")
    assertPluginError(false)
    assertTrue(errorbells())
  }

  @Test
  fun `test noerrorbells abbreviation is accepted`() {
    enterCommand("set noeb")
    assertPluginError(false)
    assertFalse(errorbells())
  }

  // In Vim 'errorbells' only makes a difference for error messages: the bell is always used for errors without a
  // message, such as `k` on the first line (:help 'errorbells'). Verified in Vim 9.1 with the bell replaced by a
  // visible marker.

  @Test
  fun `test an error message does not ring the bell with noerrorbells`() {
    enterCommand("set noerrorbells")
    assertEquals(0, bellsRungBy { enterCommand("s/found/replaced") })
    assertPluginErrorMessage("E486: Pattern not found: found")
  }

  // The fixture is a single line, so `k` fails and nothing is shown for it
  @Test
  fun `test an error without a message rings the bell with noerrorbells`() {
    enterCommand("set noerrorbells")
    assertEquals(1, bellsRungBy { typeText(injector.parser.parseKeys("k")) })
  }

  @Test
  fun `test an error message rings the bell with errorbells`() {
    enterCommand("set errorbells")
    assertEquals(1, bellsRungBy { enterCommand("s/found/replaced") })
    assertPluginErrorMessage("E486: Pattern not found: found")
  }

  @Test
  fun `test an error without a message rings the bell with errorbells`() {
    enterCommand("set errorbells")
    assertEquals(1, bellsRungBy { typeText(injector.parser.parseKeys("k")) })
  }

  @Test
  fun `test an error message of an unknown command follows errorbells`() {
    // Unlike `:s`, an unknown command reports its message by appending it to the output
    enterCommand("set noerrorbells")
    assertEquals(0, bellsRungBy { enterCommand("nosuchcommand") })
    assertPluginErrorMessage("E492: Not an editor command: nosuchcommand")

    enterCommand("set errorbells")
    assertEquals(1, bellsRungBy { enterCommand("nosuchcommand") })
  }

  // The beep itself is skipped in unit-test mode, so the tests count how many times the bell was asked to ring
  private fun bellsRungBy(action: () -> Unit): Int {
    val messages = injector.messages as IjVimMessages
    injector.messages.clearError()
    val bellsBefore = messages.unitTestBellCount
    action()
    assertTrue(injector.messages.isError(), "The action should report an error")
    return messages.unitTestBellCount - bellsBefore
  }
}
