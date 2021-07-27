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

import com.maddyhome.idea.vim.ex.ExOutputModel.Companion.getInstance
import com.maddyhome.idea.vim.helper.StringHelper
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author John Weigel
 */
class BufferListHandlerTest : VimTestCase() {
  companion object {
    const val DEFAULT_LS_OUTPUT = "   1 %a   \"/src/aaa.txt\"                 line: 1"
  }

  fun testLsAction() {
    configureByText("\n")
    typeText(commandToKeys("ls"))

    val output = getInstance(myFixture.editor).text
    TestCase.assertNotNull(output)
    val displayedLines = output!!.split("\n".toRegex()).toTypedArray()
    TestCase.assertEquals(DEFAULT_LS_OUTPUT, displayedLines[0])

    assertPluginError(false)
  }

  fun testLsActionWithLongFileName() {
    configureByFileName("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa.txt")
    typeText(commandToKeys("ls"))

    val output = getInstance(myFixture.editor).text
    TestCase.assertNotNull(output)
    val displayedLines = output!!.split("\n".toRegex()).toTypedArray()
    TestCase.assertEquals("   1 %a   \"/src/aaaaaaaaaaaaaaaaaaaaaaaaaaaaa.txt\" line: 1", displayedLines[0])

    assertPluginError(false)
  }

  fun testFilesAction() {
    configureByText("\n")
    typeText(commandToKeys("files"))
    assertPluginError(false)
  }

  fun testBuffersAction() {
    configureByText("\n")
    typeText(commandToKeys("buffers"))
    assertPluginError(false)
  }

  fun testBuffersActionWithSupportedFilterMatch() {
    configureByFileName("aaa.txt")
    configureByFileName("bbb.txt")
    typeText(StringHelper.parseKeys("aa<esc>:buffers +<enter>"))

    val output = getInstance(myFixture.editor).text
    TestCase.assertNotNull(output)
    val displayedLines = output!!.split("\n".toRegex()).toTypedArray()

    // Ignore buffer number because IJ sometimes returns different order of buffers
    val line = displayedLines[0].replaceRange(3, 4, "_")
    TestCase.assertEquals("   _ %a + \"/src/bbb.txt\"                 line: 1", line)

    assertPluginError(false)
  }

  fun testBuffersActionWithSupportedFilterDoesNotMatch() {
    configureByText("\n")
    typeText(StringHelper.parseKeys("aa<esc>:buffers #<enter>"))

    val output = getInstance(myFixture.editor).text
    TestCase.assertNotNull(output)
    val displayedLines = output!!.split("\n".toRegex()).toTypedArray()
    TestCase.assertEquals("", displayedLines[0])

    assertPluginError(false)
  }

  fun testBuffersActionWithUnSupportedFilter() {
    configureByText("\n")
    typeText(commandToKeys("buffers x"))

    val output = getInstance(myFixture.editor).text
    TestCase.assertNotNull(output)
    val displayedLines = output!!.split("\n".toRegex()).toTypedArray()
    TestCase.assertEquals(DEFAULT_LS_OUTPUT, displayedLines[0])

    assertPluginError(false)
  }
}
