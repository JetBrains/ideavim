/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class SelectOlderHistoryFilteredActionTest : VimExTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enterCommand("set digraph")
    enterCommand("digraph")
    enterCommand("set incsearch")
    enterSearch("something cool")
    enterSearch("not cool")
    enterSearch("so cool")
  }

  @Test
  fun `test Up selects older command history entry filtered by typed prefix`() {
    typeText(":set<Up>")    // Not filtered. Selects #3 as current entry
    assertExText("set incsearch")

    typeText("<Up>")        // Skips #2, selects #1 as current entry
    assertExText("set digraph")

    // Hit the end of history, beep and stay where we are
    typeText("<Up>")
    assertExText("set digraph")
    assertPluginError(true)
  }

  @Test
  fun `test Up selects older search history entry filtered by typed prefix`() {
    typeText("/so<Up>")
    assertExText("so cool")

    // Skips "not cool" because it doesn't match the "so" prefix
    typeText("<Up>")
    assertExText("something cool")

    typeText("<Up>")
    assertExText("something cool")
  }

  @Test
  fun `test Up selects next older filtered command history entry after current entry`() {
    enterCommand("echo food_is_nice")   // #1
    enterCommand("echo fool")           // #2
    enterCommand("echo baz")            // #3
    enterCommand("echo foo")            // #4
    enterCommand("echo food_yes")       // #5
    enterCommand("echo bar")            // #6
    typeText(":echo f<Up><Up>") // Skip #6 and #5 and make #4 the current entry
    assertExText("echo foo")
    typeText("d<Up>") // Next oldest from #4 matching "echo food" -> #1
    assertExText("echo food_is_nice")
  }
}
