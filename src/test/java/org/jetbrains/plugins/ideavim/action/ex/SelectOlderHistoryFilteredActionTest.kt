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
    typeText(":set<Up>")
    assertExText("set incsearch")

    // Skips "digraph" because it doesn't match the "set" prefix
    typeText("<Up>")
    assertExText("set digraph")

    typeText("<Up>")
    assertExText("set digraph")
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
}
