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

    typeText("<S-Up>")
    assertExText("set digraph")
  }

  @Test
  fun `test Ctrl-P selects older command history entry ignoring typed prefix`() {
    typeText(":map<C-P>")
    assertExText("set incsearch")

    typeText("<C-P>")
    assertExText("digraph")

    typeText("<C-P>")
    assertExText("set digraph")

    typeText("<C-P>")
    assertExText("set digraph")
  }

  @Test
  fun `test PageUp selects older command history entry ignoring typed prefix`() {
    typeText(":map<PageUp>")
    assertExText("set incsearch")

    typeText("<PageUp>")
    assertExText("digraph")

    typeText("<PageUp>")
    assertExText("set digraph")

    typeText("<PageUp>")
    assertExText("set digraph")
  }

  @Test
  fun `test Shift-Up selects older search history entry ignoring typed prefix`() {
    typeText("/map<S-Up>")
    assertExText("so cool")

    typeText("<S-Up>")
    assertExText("not cool")

    typeText("<S-Up>")
    assertExText("something cool")

    typeText("<S-Up>")
    assertExText("something cool")
  }

  @Test
  fun `test Ctrl-P selects older search history entry ignoring typed prefix`() {
    typeText("/map<C-P>")
    assertExText("so cool")

    typeText("<C-P>")
    assertExText("not cool")

    typeText("<C-P>")
    assertExText("something cool")

    typeText("<C-P>")
    assertExText("something cool")
  }

  @Test
  fun `test PageUp selects older search history entry ignoring typed prefix`() {
    typeText("/map<PageUp>")
    assertExText("so cool")

    typeText("<PageUp>")
    assertExText("not cool")

    typeText("<PageUp>")
    assertExText("something cool")

    typeText("<PageUp>")
    assertExText("something cool")
  }
}
