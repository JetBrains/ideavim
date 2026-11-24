/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.VimBell
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.mock.MockTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// VIM-485 |'errorbells'| |'noerrorbells'|
@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class ErrorbellsOptionTest : MockTestCase() {
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

  // The bell is a mock service, so the tests assert its calls instead of listening for the beep

  @Test
  fun `test an error message does not ring the bell with noerrorbells`() {
    val bell = mockService(VimBell::class.java)
    enterCommand("set noerrorbells")
    enterCommand("s/found/replaced")
    assertPluginErrorMessage("E486: Pattern not found: found")
    verify(bell, never()).ring()
  }

  // The fixture is a single line, so `k` fails and nothing is shown for it
  @Test
  fun `test an error without a message rings the bell with noerrorbells`() {
    val bell = mockService(VimBell::class.java)
    enterCommand("set noerrorbells")
    typeText(injector.parser.parseKeys("k"))
    verify(bell).ring()
  }

  @Test
  fun `test an error message rings the bell with errorbells`() {
    val bell = mockService(VimBell::class.java)
    enterCommand("set errorbells")
    enterCommand("s/found/replaced")
    assertPluginErrorMessage("E486: Pattern not found: found")
    verify(bell).ring()
  }

  @Test
  fun `test an error without a message rings the bell with errorbells`() {
    val bell = mockService(VimBell::class.java)
    enterCommand("set errorbells")
    typeText(injector.parser.parseKeys("k"))
    verify(bell).ring()
  }

  @Test
  fun `test an error message of an unknown command follows errorbells`() {
    val bell = mockService(VimBell::class.java)
    // Unlike `:s`, an unknown command reports its message by appending it to the output
    enterCommand("set noerrorbells")
    enterCommand("nosuchcommand")
    assertPluginErrorMessage("E492: Not an editor command: nosuchcommand")
    verify(bell, never()).ring()

    enterCommand("set errorbells")
    enterCommand("nosuchcommand")
    verify(bell).ring()
  }
}
