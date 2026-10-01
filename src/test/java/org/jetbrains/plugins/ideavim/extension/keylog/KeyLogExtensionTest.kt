/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.keylog

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.unsetToggleOption
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.ui.OutputPanel
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KeyLogExtensionTest : VimTestCase() {
  @TempDir
  lateinit var tempDir: Path

  private val logFile: Path
    get() = tempDir.resolve("ideavim.log")

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("${c}one two three\nfour five six\n")
    setLogFile(logFile)
    enableExtensions("keylog")
  }

  @Test
  fun `test typed keys are written to the log`() {
    typeText("wdwihello<Esc>")
    assertLog("wdwihello<Esc>")
  }

  @Test
  fun `test command line keys are written to the log`() {
    enterCommand("s/one/1/")
    assertLog(":s/one/1/<CR>")
  }

  @Test
  fun `test left-hand side of mapping is written instead of right-hand side`() {
    enterCommand("nnoremap Q dd")
    typeText("Q")
    assertLog(":nnoremap Q dd<CR>Q")
  }

  @Test
  fun `test macro playback is not written`() {
    typeText("qaxq@a")
    assertLog("qaxq@a")
  }

  @Test
  fun `test dot repeat is not written`() {
    typeText("dw..")
    assertLog("dw..")
  }

  @Test
  fun `test special keys are written in key notation`() {
    typeText("i<C-W><BS><Up><Del><CR><Esc>")
    assertLog("i<C-W><BS><Up><Del><CR><Esc>")
  }

  @Test
  fun `test typed less than is written as lt`() {
    typeText("i<lt>Esc><Esc>")
    assertLog("i<lt>Esc><Esc>")
  }

  @Test
  fun `test multibyte characters are written as UTF-8`() {
    typeText("iżółw<Esc>")
    assertLog("iżółw<Esc>")
  }

  @Test
  fun `test keys typed in the output panel are written to the log`() {
    enterCommand("echo \"one\\ntwo\"")
    val outputPanel = injector.outputPanel.getCurrentOutputPanel() as OutputPanel
    ApplicationManager.getApplication().invokeAndWait {
      outputPanel.handleKey(injector.parser.parseKeys("q").single())
    }
    assertLog(":echo \"one\\ntwo\"<CR>q")
  }

  @Test
  fun `test keys are appended to existing log`() {
    logFile.writeText("previous")
    typeText("x")
    assertLog("previousx")
  }

  @Test
  fun `test changing the log file variable switches the log file`() {
    typeText("x")
    val otherFile = tempDir.resolve("nested/other.log")
    // Typed keys of the command go to the old file, up to the moment the variable is set
    enterCommand("let g:keylog_file = '${vimPath(otherFile)}'")
    typeText("x")

    assertLog("x:let g:keylog_file = '${vimPath(otherFile)}'<CR>")
    assertEquals("x", otherFile.readText())
  }

  @Test
  fun `test log file that cannot be opened is skipped until the variable changes`() {
    setLogFile(tempDir)
    typeText("x")
    setLogFile(logFile)
    typeText("y")
    assertLog("y")
  }

  @Test
  fun `test variable that is not a string does not break typing`() {
    executeVimscript("let g:keylog_file = []")
    typeText("x")
    assertState("${c}ne two three\nfour five six\n")

    setLogFile(logFile)
    typeText("x")
    assertLog("x")
  }

  @Test
  fun `test nothing is written when extension is disabled`() {
    enterCommand("set nokeylog")
    typeText("x")
    assertLog(":set nokeylog<CR>")
  }

  @Test
  fun `test log file is not created before a key is typed`() {
    stopLogging()
    assertFalse(logFile.exists())
  }

  // Executed instead of typed, so the command itself isn't logged
  private fun setLogFile(path: Path) {
    executeVimscript("let g:keylog_file = '${vimPath(path)}'")
  }

  private fun vimPath(path: Path) = path.toString().replace("\\", "/")

  // Disabling the extension writes the pending keys. Done through the option, so no keys are typed
  private fun stopLogging() {
    val option = injector.optionGroup.getOption("keylog") as ToggleOption
    injector.optionGroup.unsetToggleOption(option, OptionAccessScope.GLOBAL(null))
  }

  private fun assertLog(expected: String) {
    stopLogging()
    assertEquals(expected, logFile.readText())
  }
}
