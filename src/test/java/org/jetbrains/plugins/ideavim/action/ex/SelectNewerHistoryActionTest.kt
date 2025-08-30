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
  fun `test Shift+Down selects newer command history entry ignoring typed prefix`() {
    typeText(":map<S-Up>")
    assertExText("set incsearch")
    typeText("<S-Up>")
    assertExText("digraph")

    typeText("<S-Down>")
    assertExText("set incsearch")
  }

  @Test
  fun `test Shift+Down selects last typed command value`() {
    typeText(":map<S-Up>")
    assertExText("set incsearch")

    typeText("<S-Down>")
    assertExText("map")
  }

  @Test
  fun `test Ctrl-N selects newer command history entry ignoring typed prefix`() {
    typeText(":map<C-P>")
    assertExText("set incsearch")
    typeText("<C-P>")
    assertExText("digraph")

    typeText("<C-N>")
    assertExText("set incsearch")
  }

  @Test
  fun `test Ctrl-N selects last typed comamnd value`() {
    typeText(":map<C-P>")
    assertExText("set incsearch")

    typeText("<C-N>")
    assertExText("map")
  }

  @Test
  fun `test PageDown selects newer command history entry ignoring typed prefix`() {
    typeText(":map<PageUp>")
    assertExText("set incsearch")
    typeText("<PageUp>")
    assertExText("digraph")

    typeText("<PageDown>")
    assertExText("set incsearch")
  }

  @Test
  fun `test PageDown selects last typed command value`() {
    typeText(":map<PageUp>")
    assertExText("set incsearch")

    typeText("<PageDown>")
    assertExText("map")
  }

  @Test
  fun `test Shift+Down selects newer search history entry ignoring typed prefix`() {
    typeText("/map<S-Up>")
    assertExText("so cool")
    typeText("<S-Up>")
    assertExText("not cool")

    typeText("<S-Down>")
    assertExText("so cool")
  }

  @Test
  fun `test Shift+Down selects last typed search value`() {
    typeText("/map<S-Up>")
    assertExText("so cool")

    typeText("<S-Down>")
    assertExText("map")
  }

  @Test
  fun `test Ctrl-N selects newer search history entry ignoring typed prefix`() {
    typeText("/map<C-P>")
    assertExText("so cool")
    typeText("<C-P>")
    assertExText("not cool")

    typeText("<C-N>")
    assertExText("so cool")
  }

  @Test
  fun `test Ctrl-N selects last typed search value`() {
    typeText("/map<C-P>")
    assertExText("so cool")

    typeText("<C-N>")
    assertExText("map")
  }

  @Test
  fun `test PageDown selects newer search history entry ignoring typed prefix`() {
    typeText("/map<PageUp>")
    assertExText("so cool")
    typeText("<PageUp>")
    assertExText("not cool")

    typeText("<PageDown>")
    assertExText("so cool")
  }

  @Test
  fun `test PageDown selects last typed search value`() {
    typeText("/map<PageUp>")
    assertExText("so cool")

    typeText("<PageDown>")
    assertExText("map")
  }
}
