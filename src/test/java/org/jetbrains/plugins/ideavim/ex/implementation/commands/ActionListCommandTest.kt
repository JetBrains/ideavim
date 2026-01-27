/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.util.ArrayUtil
import com.maddyhome.idea.vim.ui.OutputModel.Companion.getInstance
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertNotNull

@Suppress("SpellCheckingInspection")
class ActionListCommandTest : VimTestCase() {
  @Test
  fun testListAllActions() {
    configureByText("\n")
    typeText(commandToKeys("actionlist"))
    val output = getInstance(fixture.editor).text
    assertNotNull<Any>(output)

    // Header line
    val displayedLines = output!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    kotlin.test.assertEquals("--- Actions ---", displayedLines[0])

    // Action lines
    val displayedActionNum = displayedLines.size - 1
    val actionIds = ActionManager.getInstance().getActionIdList("")
    kotlin.test.assertEquals(displayedActionNum, actionIds.size)
  }

  @Test
  fun testSearchByActionName() {
    configureByText("\n")
    typeText(commandToKeys("actionlist quickimpl"))
    val displayedLines = parseActionListOutput()
    for (i in displayedLines.indices) {
      val line = displayedLines[i]
      if (i == 0) {
        kotlin.test.assertEquals("--- Actions ---", line)
      } else {
        kotlin.test.assertTrue(line.lowercase(Locale.getDefault()).contains("quickimpl"))
      }
    }
  }

  @Test
  fun testSearchByAssignedShortcutKey() {
    configureByText("\n")
    typeText(commandToKeys("actionlist <M-S-"))
    val displayedLines = parseActionListOutput()
    for (i in displayedLines.indices) {
      val line = displayedLines[i]
      if (i == 0) {
        kotlin.test.assertEquals("--- Actions ---", line)
      } else {
        kotlin.test.assertTrue(line.lowercase(Locale.getDefault()).contains("<m-s-"))
      }
    }
  }

  private fun parseActionListOutput(): Array<String> {
    val output = getInstance(fixture.editor).text
    return output?.split("\n".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
      ?: ArrayUtil.EMPTY_STRING_ARRAY
  }
}
