/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.autocmd

import com.maddyhome.idea.vim.vimscript.model.commands.AutoCmdCommand
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import com.maddyhome.idea.vim.vimscript.parser.errors.IdeavimErrorListener
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AutoCmdParseTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private fun parseAutocmd(text: String): AutoCmdCommand {
    val script = VimscriptParser.parse(text)
    assertTrue(IdeavimErrorListener.testLogger.isEmpty(), "Parser errors: ${IdeavimErrorListener.testLogger}")
    assertEquals(1, script.units.size)
    return assertIs<AutoCmdCommand>(script.units.first())
  }

  @Test
  fun `parse single event with star pattern`() {
    val cmd = parseAutocmd("autocmd InsertEnter * echo hi")
    assertEquals(listOf("InsertEnter"), cmd.eventNames)
    assertEquals("*", cmd.filePattern)
    assertEquals("echo hi", cmd.commandText)
  }

  @Test
  fun `parse single event with extension pattern`() {
    val cmd = parseAutocmd("autocmd InsertEnter *.py echo hi")
    assertEquals(listOf("InsertEnter"), cmd.eventNames)
    assertEquals("*.py", cmd.filePattern)
    assertEquals("echo hi", cmd.commandText)
  }

  @Test
  fun `parse comma-separated events with pattern`() {
    val cmd = parseAutocmd("autocmd InsertEnter,InsertLeave *.txt echo hi")
    assertEquals(listOf("InsertEnter", "InsertLeave"), cmd.eventNames)
    assertEquals("*.txt", cmd.filePattern)
    assertEquals("echo hi", cmd.commandText)
  }

  @Test
  fun `parse events with spaces around commas`() {
    val cmd = parseAutocmd("autocmd InsertEnter , InsertLeave * echo hi")
    assertEquals(listOf("InsertEnter", "InsertLeave"), cmd.eventNames)
    assertEquals("*", cmd.filePattern)
    assertEquals("echo hi", cmd.commandText)
  }

  @Test
  fun `parse brace pattern`() {
    val cmd = parseAutocmd("autocmd InsertEnter *.{py,txt} echo hi")
    assertEquals(listOf("InsertEnter"), cmd.eventNames)
    assertEquals("*.{py,txt}", cmd.filePattern)
    assertEquals("echo hi", cmd.commandText)
  }

  @Test
  fun `parse bang has no events or pattern`() {
    val cmd = parseAutocmd("autocmd!")
    assertTrue(cmd.eventNames.isEmpty())
    assertEquals(null, cmd.filePattern)
    assertEquals(null, cmd.commandText)
  }

  @Test
  fun `parse command with multiple spaces`() {
    val cmd = parseAutocmd("autocmd InsertEnter * echo \"hello world\"")
    assertEquals("*", cmd.filePattern)
    assertEquals("echo \"hello world\"", cmd.commandText)
  }

  @Test
  fun `parse exact filename pattern`() {
    val cmd = parseAutocmd("autocmd InsertEnter Makefile echo hi")
    assertEquals(listOf("InsertEnter"), cmd.eventNames)
    assertEquals("Makefile", cmd.filePattern)
    assertEquals("echo hi", cmd.commandText)
  }

  @Test
  fun `parse unknown event name without errors`() {
    val script = VimscriptParser.parse("autocmd BufReadPost * echo hi")
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
    assertEquals(1, script.units.size)
    val cmd = assertIs<AutoCmdCommand>(script.units.first())
    assertEquals(listOf("BufReadPost"), cmd.eventNames)
  }

  @Test
  fun `parse multiline autocmd without errors`() {
    val script = VimscriptParser.parse(
      """
        autocmd BufReadPost *
        \ if line("'\"") > 0 && line ("'\"") <= line("$") |
        \   exe "normal! g'\"" |
        \ endif
      """.trimIndent(),
    )
    assertEquals(1, script.units.size)
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }
}
