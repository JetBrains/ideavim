/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExOutputModel.Companion.getInstance
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author John Weigel
 */
class BufferListCommandTest : VimTestCase() {
  companion object {
    const val DEFAULT_LS_OUTPUT = "   1 %a   \"/src/aaa.txt\"                 line: 1"
  }

  @Test
  fun testLsAction() {
    configureByText("\n")
    typeText(commandToKeys("ls"))

    val output = getInstance(fixture.editor).text
    kotlin.test.assertNotNull<Any>(output)
    val displayedLines = output.split("\n".toRegex()).toTypedArray()
    kotlin.test.assertEquals(DEFAULT_LS_OUTPUT, displayedLines[0])

    assertPluginError(false)
  }

  @Test
  fun testLsActionWithLongFileName() {
    configureByFileName("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa.txt")
    typeText(commandToKeys("ls"))

    val output = getInstance(fixture.editor).text
    kotlin.test.assertNotNull<Any>(output)
    val displayedLines = output.split("\n".toRegex()).toTypedArray()
    kotlin.test.assertEquals("   1 %a   \"/src/aaaaaaaaaaaaaaaaaaaaaaaaaaaaa.txt\" line: 1", displayedLines[0])

    assertPluginError(false)
  }

  @Test
  fun testFilesAction() {
    configureByText("\n")
    typeText(commandToKeys("files"))
    assertPluginError(false)
  }

  @Test
  fun testBuffersAction() {
    configureByText("\n")
    typeText(commandToKeys("buffers"))
    assertPluginError(false)
  }

  @Test
  fun testBuffersActionWithSupportedFilterMatch() {
    configureByFileName("aaa.txt")
    configureByFileName("bbb.txt")
    typeText(injector.parser.parseKeys("aa<esc>:buffers +<enter>"))

    val output = getInstance(fixture.editor).text
    kotlin.test.assertNotNull<Any>(output)
    val displayedLines = output.split("\n".toRegex()).toTypedArray()

    // Ignore buffer number because IJ sometimes returns different order of buffers
    val line = displayedLines[0].replaceRange(3, 4, "_")
    kotlin.test.assertEquals("   _ %a + \"/src/bbb.txt\"                 line: 1", line)

    assertPluginError(false)
  }

  @Test
  fun testBuffersActionWithSupportedFilterDoesNotMatch() {
    configureByText("\n")
    typeText(injector.parser.parseKeys("aa<esc>:buffers #<enter>"))

    val output = getInstance(fixture.editor).text
    kotlin.test.assertNotNull<Any>(output)
    val displayedLines = output.split("\n".toRegex()).toTypedArray()
    kotlin.test.assertEquals("", displayedLines[0])

    assertPluginError(false)
  }

  @Test
  fun testBuffersActionWithUnSupportedFilter() {
    configureByText("\n")
    typeText(commandToKeys("buffers x"))

    val output = getInstance(fixture.editor).text
    kotlin.test.assertNotNull<Any>(output)
    val displayedLines = output.split("\n".toRegex()).toTypedArray()
    kotlin.test.assertEquals(DEFAULT_LS_OUTPUT, displayedLines[0])

    assertPluginError(false)
  }

  @Test
  fun testEnterCommandModeAfterBuffersOutput() {
    // VIM-2508: Command mode should stay open after pressing : following "Hit ENTER or type command to continue"
    configureByText("\n")

    // Execute buffers command which produces output
    typeText(commandToKeys("buffers"))

    // Verify output was produced
    val output = getInstance(fixture.editor).text
    kotlin.test.assertNotNull<Any>(output)

    // Simulate pressing : to enter a new command after the output
    // Note: This simulates the user pressing : at the "Hit ENTER or type command to continue" prompt
    typeText(injector.parser.parseKeys(":"))

    // Command mode should be active
    kotlin.test.assertTrue(
      ExEntryPanel.getOrCreatePanelInstance().isActive,
      "Command mode should be active after pressing : at the output prompt"
    )

    // Cancel the command entry
    typeText(injector.parser.parseKeys("<Esc>"))
  }
}
