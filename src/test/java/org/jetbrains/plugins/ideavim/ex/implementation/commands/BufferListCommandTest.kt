/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
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

    val output = injector.outputPanel.getCurrentOutputPanel()?.text
    kotlin.test.assertNotNull<Any>(output)
    val displayedLines = output.split("\n".toRegex()).toTypedArray()
    kotlin.test.assertEquals(DEFAULT_LS_OUTPUT, displayedLines[0])

    assertPluginError(false)
  }

  @Test
  fun testLsActionWithLongFileName() {
    configureByFileName("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa.txt")
    typeText(commandToKeys("ls"))

    val output = injector.outputPanel.getCurrentOutputPanel()?.text
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

    val output = injector.outputPanel.getCurrentOutputPanel()?.text
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

    val output = injector.outputPanel.getCurrentOutputPanel()?.text
    kotlin.test.assertNotNull<Any>(output)
    val displayedLines = output.split("\n".toRegex()).toTypedArray()
    kotlin.test.assertEquals("", displayedLines[0])

    assertPluginError(false)
  }

  @Test
  fun testBuffersActionWithUnSupportedFilter() {
    configureByText("\n")
    typeText(commandToKeys("buffers x"))

    val output = injector.outputPanel.getCurrentOutputPanel()?.text
    kotlin.test.assertNotNull<Any>(output)
    val displayedLines = output.split("\n".toRegex()).toTypedArray()
    kotlin.test.assertEquals(DEFAULT_LS_OUTPUT, displayedLines[0])

    assertPluginError(false)
  }
}
