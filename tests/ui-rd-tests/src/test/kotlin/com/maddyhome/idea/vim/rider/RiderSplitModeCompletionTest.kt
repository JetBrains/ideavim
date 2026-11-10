/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent

/**
 * Completion in Rider split mode, where keys are handled by the JetBrains Client frontend
 */
class RiderSplitModeCompletionTest : IdeaVimRiderTestBase() {

  override val splitMode: Boolean = true

  @AfterEach
  fun resetMode() {
    esc()
  }

  @Test
  fun `escape with the completion popup open exits insert mode`() {
    // The dot is in the text already: typed while the popup is open, it would insert the selected item
    setText("Console.WriteLine();\nSystem.\n")

    typeVim("jAC")
    assertTrue(waitUntil(timeoutMs = 10_000) { isLookupShown() }) { "The completion popup was not shown" }
    esc()
    assertTrue(waitUntil { !isLookupShown() }) { "The completion popup was not closed" }
    // In Normal mode, `dd` deletes the line instead of being inserted
    typeVim("dd")

    assertText("Console.WriteLine();\n")
  }

  // VIM-4369
  @Test
  fun `escape with the summary popup next to the completion popup exits insert mode`() {
    setText("Console.WriteLine();\nSystem.\n")

    typeVim("jAC")
    assertTrue(waitUntil(timeoutMs = 10_000) { isLookupShown() }) { "The completion popup was not shown" }
    showLookupSummary("struct System.Char")
    assertTrue(waitUntil { isLookupSummaryShown() }) { "The summary popup was not shown" }
    esc()
    assertTrue(waitUntil { !isLookupShown() && !isLookupSummaryShown() }) { "The popups were not closed" }
    typeVim("dd")

    assertText("Console.WriteLine();\n")
  }

  @Test
  fun `escape owned by the IDE closes the summary and completion popups and stays in insert mode`() {
    setText("Console.WriteLine();\nSystem.\n")
    exCommand("sethandler <Esc> i:ide")
    try {
      typeVim("jAC")
      assertTrue(waitUntil(timeoutMs = 10_000) { isLookupShown() }) { "The completion popup was not shown" }
      showLookupSummary("struct System.Char")
      assertTrue(waitUntil { isLookupSummaryShown() }) { "The summary popup was not shown" }
      esc()
      assertTrue(waitUntil { !isLookupShown() && !isLookupSummaryShown() }) { "The popups were not closed" }
      // In Insert mode, `x` is inserted instead of deleting a character
      typeVim("x")

      assertText("Console.WriteLine();\nSystem.Cx\n")
    } finally {
      // Escape doesn't leave Insert mode now
      ctrl(KeyEvent.VK_OPEN_BRACKET)
      exCommand("sethandler <Esc> a:vim")
    }
  }
}
