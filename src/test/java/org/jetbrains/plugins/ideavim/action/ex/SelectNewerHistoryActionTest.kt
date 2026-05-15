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

class SelectNewerHistoryActionTest : VimExTestCase() {
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
  fun `test Shift+Down with no command history beeps`() {
    typeText(":<S-Down>")
    assertExText("")
    assertPluginError(true)
  }

  @Test
  fun `test Shift+Down with no command history and typed entry beeps and does not change typed entry`() {
    typeText(":map<S-Down>")
    assertExText("map")
    assertPluginError(true)
  }

  @Test
  fun `test Shift+Down selects newer command history entry ignoring typed prefix`() {
    typeText(":map<S-Up>")  // Selects #3 as current entry
    assertExText("set incsearch")
    typeText("<S-Up>")      // Selects #2 as current entry
    assertExText("digraph")

    // <S-Down> ignores the typed `map` prefix and selects next entry (#3 - "set incsearch")
    typeText("<S-Down>")
    assertExText("set incsearch")
  }

  @Test
  fun `test Shift+Down on last saved entry selects last typed command value`() {
    typeText(":map<S-Up>")  // Selects #3 as current entry
    assertExText("set incsearch")

    typeText("<S-Down>")    // Next newer entry is the in-progress typed entry "map"
    assertExText("map")
  }

  @Test
  fun `test Shift+Down after hitting start of history selects second oldest entry`() {
    typeText(":<S-Up>")   // Selects #3 as current entry
    typeText("<S-Up>")    // Selects #2 as current entry
    typeText("<S-Up>")    // Selects #1 as current entry
    typeText("<S-Up>")    // Tries to move past start of history, beeps, but doesn't change content
    assertExText("set digraph")
    assertPluginError(true)

    typeText("<S-Down>")  // #1 is still current entry. Selects #2 as current entry
    assertExText("digraph")
    assertPluginError(false)
  }

  @Test
  fun `test select older entry after hitting end of history`() {
    typeText(":map<S-Up>")   // Selects #3 as current entry
    typeText("<S-Down>")     // Selects in-progress entry as current entry
    assertExText("map")
    assertPluginError(false)

    typeText("<S-Down>")    // Can't select anything, error
    assertExText("map")
    assertPluginError(true)

    typeText("<S-Up>")      // Selects #3 as current entry
    assertExText("set incsearch")
    assertPluginError(false)
  }

  @Test
  fun `test Ctrl-N with no command history beeps`() {
    typeText(":<C-N>")
    assertExText("")
    assertPluginError(true)
  }

  @Test
  fun `test Ctrl-N with no command history and typed entry beeps and does not change typed entry`() {
    typeText(":map<C-N>")
    assertExText("map")
    assertPluginError(true)
  }

  @Test
  fun `test Ctrl-N selects newer command history entry ignoring typed prefix`() {
    typeText(":map<C-P>")   // Selects #3 as current entry
    assertExText("set incsearch")
    typeText("<C-P>")       // Selects #2 as current entry
    assertExText("digraph")

    // <C-N> ignores the typed `map` prefix and selects next entry (#3 - "set incsearch")
    typeText("<C-N>")
    assertExText("set incsearch")
  }

  @Test
  fun `test Ctrl-N on last saved entry selects last typed command value`() {
    typeText(":map<C-P>")   // Selects #3 as current entry
    assertExText("set incsearch")

    typeText("<C-N>")       // Next newer entry is in-progress typed entry "map"
    assertExText("map")
  }

  @Test
  fun `test PageDown with no command history beeps`() {
    typeText(":<PageDown>")
    assertExText("")
    assertPluginError(true)
  }

  @Test
  fun `test PageDown with no command history and typed entry beeps and does not change typed entry`() {
    typeText(":map<PageDown>")
    assertExText("map")
    assertPluginError(true)
  }

  @Test
  fun `test PageDown selects newer command history entry ignoring typed prefix`() {
    typeText(":map<PageUp>")  // Selects #3 as current entry
    assertExText("set incsearch")
    typeText("<PageUp>")      // Selects #2 as current entry
    assertExText("digraph")

    // <PageDown> ignores the typed `map` prefix and selects the next entry (#3 - "set incsearch")
    typeText("<PageDown>")
    assertExText("set incsearch")
  }

  @Test
  fun `test PageDown on last saved entry selects last typed command value`() {
    typeText(":map<PageUp>")  // Selects #3 as current entry
    assertExText("set incsearch")

    typeText("<PageDown>")    // Next newer entry is the in-progress typed entry "map"
    assertExText("map")
  }

  @Test
  fun `test Shift+Down with no search history beeps`() {
    typeText("/<S-Down>")
    assertExText("")
    assertPluginError(true)
  }

  @Test
  fun `test Shift+Down with no search history and typed entry beeps and does not change typed entry`() {
    typeText("/map<S-Down>")
    assertExText("map")
    assertPluginError(true)
  }

  @Test
  fun `test Shift+Down selects newer search history entry ignoring typed prefix`() {
    typeText("/map<S-Up>")  // Selects #3 as current entry
    assertExText("so cool")
    typeText("<S-Up>")      // Selects #2 as current entry
    assertExText("not cool")

    // <S-Down> ignores the typed `map` prefix and selects next entry (#3 - "so cool")
    typeText("<S-Down>")
    assertExText("so cool")
  }

  @Test
  fun `test Shift+Down on last saved entry selects last typed search value`() {
    typeText("/map<S-Up>")  // Selects #3 as current entry
    assertExText("so cool")

    typeText("<S-Down>")    // Next newer entry is the in-progress typed entry "map"
    assertExText("map")
  }

  @Test
  fun `test Ctrl-N with no search history beeps`() {
    typeText("/<C-N>")
    assertExText("")
    assertPluginError(true)
  }

  @Test
  fun `test Ctrl-N with no search history and typed entry beeps and does not change typed entry`() {
    typeText("/map<C-N>")
    assertExText("map")
    assertPluginError(true)
  }

  @Test
  fun `test Ctrl-N selects newer search history entry ignoring typed prefix`() {
    typeText("/map<C-P>")   // Selects #3 as current entry
    assertExText("so cool")
    typeText("<C-P>")       // Selects #2 as current entry
    assertExText("not cool")

    // <C-N> ignores the typed `map` prefix and selects next entry (#3 - "so cool")
    typeText("<C-N>")
    assertExText("so cool")
  }

  @Test
  fun `test Ctrl-N on last saved entry selects last typed search value`() {
    typeText("/map<C-P>")   // Selects #3 as current entry
    assertExText("so cool")

    typeText("<C-N>")       // Next newer entry is in-progress typed entry "map"
    assertExText("map")
  }

  @Test
  fun `test PageDown with no search history beeps`() {
    typeText("/<PageDown>")
    assertExText("")
    assertPluginError(true)
  }

  @Test
  fun `test PageDown with no search history and typed entry beeps and does not change typed entry`() {
    typeText("/map<PageDown>")
    assertExText("map")
    assertPluginError(true)
  }

  @Test
  fun `test PageDown selects newer search history entry ignoring typed prefix`() {
    typeText("/map<PageUp>")  // Selects #3 as current entry
    assertExText("so cool")
    typeText("<PageUp>")      // Selects #2 as current entry
    assertExText("not cool")

    // <PageDown> ignores the typed `map` prefix and selects the next entry (#3 - "so cool")
    typeText("<PageDown>")
    assertExText("so cool")
  }

  @Test
  fun `test PageDown on last saved entry selects last typed search value`() {
    typeText("/map<PageUp>")  // Selects #3 as current entry
    assertExText("so cool")

    typeText("<PageDown>")    // Next newer entry is the in-progress typed entry "map"
    assertExText("map")
  }
}
