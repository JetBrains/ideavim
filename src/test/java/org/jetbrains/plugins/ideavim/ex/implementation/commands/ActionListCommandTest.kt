/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.util.ArrayUtil
import com.maddyhome.idea.vim.ex.ExOutputModel.Companion.getInstance
import org.jetbrains.plugins.ideavim.VimTestCase
import java.util.*

@Suppress("SpellCheckingInspection")
class ActionListCommandTest : VimTestCase() {
  fun testListAllActions() {
    configureByText("\n")
    typeText(commandToKeys("actionlist"))
    val output = getInstance(myFixture.editor).text
    assertNotNull(output)

    // Header line
    val displayedLines = output!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    assertEquals("--- Actions ---", displayedLines[0])

    // Action lines
    val displayedActionNum = displayedLines.size - 1
    val actionIds = ActionManager.getInstance().getActionIdList("")
    assertEquals(displayedActionNum, actionIds.size)
  }

  fun testSearchByActionName() {
    configureByText("\n")
    typeText(commandToKeys("actionlist quickimpl"))
    val displayedLines = parseActionListOutput()
    for (i in displayedLines.indices) {
      val line = displayedLines[i]
      if (i == 0) {
        assertEquals("--- Actions ---", line)
      } else {
        assertTrue(line.lowercase(Locale.getDefault()).contains("quickimpl"))
      }
    }
  }

  fun testSearchByAssignedShortcutKey() {
    configureByText("\n")
    typeText(commandToKeys("actionlist <M-S-"))
    val displayedLines = parseActionListOutput()
    for (i in displayedLines.indices) {
      val line = displayedLines[i]
      if (i == 0) {
        assertEquals("--- Actions ---", line)
      } else {
        assertTrue(line.lowercase(Locale.getDefault()).contains("<m-s-"))
      }
    }
  }

  private fun parseActionListOutput(): Array<String> {
    val output = getInstance(myFixture.editor).text
    return output?.split("\n".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
      ?: ArrayUtil.EMPTY_STRING_ARRAY
  }
}
