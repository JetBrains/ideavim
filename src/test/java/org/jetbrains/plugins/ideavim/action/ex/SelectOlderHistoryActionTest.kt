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


class SelectOlderHistoryActionTest : VimExTestCase() {
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
  fun `test Shift-Up selects older command history entry ignoring typed prefix`() {
    typeText(":map<S-Up>")
    assertExText("set incsearch")

    typeText("<S-Up>")
    assertExText("digraph")

    typeText("<S-Up>")
    assertExText("set digraph")

    // Hit the end of history, beep and stay where we are
    typeText("<S-Up>")
    assertExText("set digraph")
    assertPluginError(true)
  }

  @Test
  fun `test Ctrl-P selects older command history entry ignoring typed prefix`() {
    typeText(":map<C-P>")
    assertExText("set incsearch")

    typeText("<C-P>")
    assertExText("digraph")

    typeText("<C-P>")
    assertExText("set digraph")

    // Hit the end of history, beep and stay where we are
    typeText("<C-P>")
    assertExText("set digraph")
    assertPluginError(true)
  }

  @Test
  fun `test PageUp selects older command history entry ignoring typed prefix`() {
    typeText(":map<PageUp>")
    assertExText("set incsearch")

    typeText("<PageUp>")
    assertExText("digraph")

    typeText("<PageUp>")
    assertExText("set digraph")

    // Hit the end of history, beep and stay where we are
    typeText("<PageUp>")
    assertExText("set digraph")
    assertPluginError(true)
  }

  @Test
  fun `test Shift-Up selects next older command history entry from current entry even after typing`() {
    typeText(":<S-Up><S-Up>")
    assertExText("digraph")   // #2 is the current history entry
    typeText("22")
    assertExText("digraph22")

    typeText("<S-Up>")  // #2 ("digraph") is still the current history entry
    assertExText("set digraph") // #1
  }

  @Test
  fun `test Shift-Up does not maintain current history entry across command line instances`() {
    typeText(":<S-Up><S-Up>")   // #2 is the current history entry
    assertExText("digraph")

    // Cancel the command line. This will add the current ex-entry as the newest history entry #4 (removing #2 since the
    // text is the same) and resets the current history entry to past the end of the command history list
    typeText("<Esc>")

    typeText(":<S-Up><S-Up>") // Select #4 and then #3 as the current history entry
    assertExText("set incsearch")
  }

  @Test
  fun `test Shift-Up selects older search history entry ignoring typed prefix`() {
    typeText("/map<S-Up>")
    assertExText("so cool")

    typeText("<S-Up>")
    assertExText("not cool")

    typeText("<S-Up>")
    assertExText("something cool")

    // Tries to move past the start of history. Beep, but keep the text
    typeText("<S-Up>")
    assertExText("something cool")
    assertPluginError(true)
  }

  @Test
  fun `test Ctrl-P selects older search history entry ignoring typed prefix`() {
    typeText("/map<C-P>")
    assertExText("so cool")

    typeText("<C-P>")
    assertExText("not cool")

    typeText("<C-P>")
    assertExText("something cool")

    // Tries to move past the start of history. Beep, but keep the text
    typeText("<C-P>")
    assertExText("something cool")
    assertPluginError(true)
  }

  @Test
  fun `test PageUp selects older search history entry ignoring typed prefix`() {
    typeText("/map<PageUp>")
    assertExText("so cool")

    typeText("<PageUp>")
    assertExText("not cool")

    typeText("<PageUp>")
    assertExText("something cool")

    // Tries to move past the start of history. Beep, but keep the text
    typeText("<PageUp>")
    assertExText("something cool")
    assertPluginError(true)
  }

  @Test
  fun `test Shift-Up selects next older search history entry from current entry even after typing`() {
    typeText("/<S-Up><S-Up>")
    assertExText("not cool")    // #2 is the current entry
    typeText("22")
    assertExText("not cool22")

    typeText("<S-Up>")  // #2 ("not cool") is still the current history entry
    assertExText("something cool") // #1
  }

  @Test
  fun `test Shift-Up does not maintain current history entry across search instances`() {
    typeText("/<S-Up><S-Up>")   // #2 is the current history entry
    assertExText("not cool")

    // Cancel the command line. This will add the current ex-entry as the newest history entry #4 (removing #2 since the
    // text is the same) and resets the current history entry to past the end of the command history list
    typeText("<Esc>")

    typeText("/<S-Up><S-Up>") // Select #4 and then #3 as the current history entry
    assertExText("so cool")
  }
}
