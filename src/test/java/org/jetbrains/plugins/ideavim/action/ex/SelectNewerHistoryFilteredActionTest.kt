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

class SelectNewerHistoryFilteredActionTest : VimExTestCase() {
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
  fun `test Down with no command history beeps`() {
    typeText(":<Down>")
    assertExText("")
    assertPluginError(true)
  }

  @Test
  fun `test Down with no command history and typed entry beeps and does not change typed entry`() {
    typeText(":map<Down>")
    assertExText("map")
    assertPluginError(true)
  }

  @Test
  fun `test Down selects newer command history entry filtered by typed prefix`() {
    typeText(":set<S-Up>")  // Not filtered. Selects #3 as current entry
    assertExText("set incsearch")
    typeText("<S-Up>")      // Not filtered. Selects #2 as current entry
    assertExText("digraph")
    typeText("<S-Up>")      // Not filtered. Selects #1 as current entry
    assertExText("set digraph")

    // Skips "digraph" because it doesn't match the "set" prefix
    typeText("<Down>")
    assertExText("set incsearch")
  }

  @Test
  fun `test Down on last saved entry selects last typed command value`() {
    typeText(":set<S-Up>")  // Selects #3 as current entry
    assertExText("set incsearch")

    typeText("<Down>")      // Next newer entry is the in-progress typed entry "set"
    assertExText("set")
  }

  @Test
  fun `test Down after hitting start of history selects second oldest entry`() {
    typeText(":<S-Up>")   // Selects #3 as current entry
    typeText("<S-Up>")    // Selects #2 as current entry
    typeText("<S-Up>")    // Selects #1 as current entry
    typeText("<S-Up>")    // Tries to move past start of history, beeps, but doesn't change content
    assertExText("set digraph")
    assertPluginError(true)

    typeText("<Down>")    // #1 is still current entry. Selects #2 as current entry
    assertExText("digraph")
    assertPluginError(false)
  }

  @Test
  fun `test Down after hitting start of history selects second oldest entry matching prefix`() {
    typeText(":set<S-Up>")  // Selects #3 as current entry
    typeText("<S-Up>")      // Selects #2 as current entry
    typeText("<S-Up>")      // Selects #1 as current entry
    typeText("<S-Up>")      // Tries to move past start of history, beeps, but doesn't change content
    assertExText("set digraph")
    assertPluginError(true)

    typeText("<Down>")      // #1 is still current entry. Skips #3 due to matching prefix
    assertExText("set incsearch")
    assertPluginError(false)
  }

  @Test
  fun `test Down after failing to select newer filtered entry remains on last typed value and beeps`() {
    typeText(":foo<Up>")    // No filtered previous item. Remains no current entry
    assertExText("foo")
    assertPluginError(true)

    typeText("<Down>")      // No current entry, so nowhere to move to
    assertExText("foo")
    assertPluginError(true)
  }

  @Test
  fun `test select older entry after hitting end of history`() {
    typeText(":map<S-Up>")   // Selects #3 as current entry
    typeText("<Down>")       // Selects in-progress entry as current entry
    assertExText("map")
    assertPluginError(false)

    typeText("<Down>")      // Can't select anything, error
    assertExText("map")
    assertPluginError(true)

    typeText("<S-Up>")      // Selects #3 as current entry
    assertExText("set incsearch")
    assertPluginError(false)
  }

  @Test
  fun `test Down with no search history beeps`() {
    typeText("/<Down>")
    assertExText("")
    assertPluginError(true)
  }

  @Test
  fun `test Down with no search history and typed entry beeps and does not change typed entry`() {
    typeText("/map<Down>")
    assertExText("map")
    assertPluginError(true)
  }

  @Test
  fun `test Down selects newer search history entry filtered by typed prefix`() {
    typeText("/so<S-Up>")   // Not filtered. Selects #3 as current entry
    assertExText("so cool")
    typeText("<S-Up>")      // Not filtered. Selects #2 as current entry
    assertExText("not cool")
    typeText("<S-Up>")      // Not filtered. Selects #1 as current entry
    assertExText("something cool")

    // Skips "digraph" because it doesn't match the "so" prefix
    typeText("<Down>")
    assertExText("so cool")
  }

  @Test
  fun `test Down on last saved entry selects last typed search value`() {
    typeText("/so<S-Up>")   // Not filtered. Selects #3 as current entry
    assertExText("so cool")

    typeText("<Down>")
    assertExText("so")
  }
}
