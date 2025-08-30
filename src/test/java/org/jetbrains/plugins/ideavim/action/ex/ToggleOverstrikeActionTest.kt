/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import com.maddyhome.idea.vim.ui.ex.ExDocument
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("SpellCheckingInspection")
class ToggleOverstrikeActionTest : VimExTestCase() {
  @Test
  fun `test text field defaults to insert`() {
    assertInsertMode()

    // Insert "digraph" before "set" to verify insert mode
    typeText(":set<C-B>digraph")
    assertExText("digraphset")
  }

  @Test
  fun `test Insert toggles to overstrike and back`() {
    typeText(":set<C-B><Insert>digraph")
    assertExText("digraph")
    assertOverstrikeMode()

    typeText("<Insert><C-B>set ")
    assertInsertMode()
    assertExText("set digraph")
  }

  private fun assertInsertMode() {
    val exDocument = exEntryPanel.entry.document as ExDocument
    assertFalse(exDocument.isOverwrite)
  }

  private fun assertOverstrikeMode() {
    val exDocument = exEntryPanel.entry.document as ExDocument
    assertTrue(exDocument.isOverwrite)
  }
}
