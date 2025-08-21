/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.action.change.insert.InsertCompletedDigraphAction
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
@TestFor(classes = [InsertCompletedDigraphAction::class])
class InsertDigraphActionTest : VimExTestCase() {
  @Test
  fun `test insert digraph`() {
    typeText(":<C-K>OK")
    assertExText("✓")
    assertExOffset(1)
  }

  @Test
  fun `test insert digraph before existing text`() {
    typeText(":set<Home><C-K>OK")
    assertExText("✓set")
    assertExOffset(1)
  }

  @Test
  fun `test insert digraph after existing text`() {
    typeText(":set<Home><Insert><C-K>OK")
    assertExText("✓et")
    assertExOffset(1)
  }

  @Test
  fun `test prompt to insert digraph`() {
    typeText(":<C-K>")
    assertRenderedExText("?")
    assertExOffset(0)
  }

  @Test
  fun `test prompt while inserting digraph`() {
    typeText(":<C-K>O")
    assertRenderedExText("O")
    assertExOffset(0)
  }

  @Test
  fun `test prompt to insert digraph before existing text`() {
    typeText(":set<Home><C-K>")
    assertRenderedExText("?set")
    assertExOffset(0)
  }

  @Test
  fun `test prompt while inserting digraph before existing text`() {
    typeText(":set<Home><C-K>O")
    assertRenderedExText("Oset")
    assertExOffset(0)
  }

  @Test
  fun `test escape cancels digraph entry`() {
    typeText(":<C-K><Esc>OK")
    assertExIsActive()
    assertExText("OK")
  }

  @Test
  fun `test escape cancels digraph during entry`() {
    // Note that the docs state that hitting escape stops digraph entry and cancels command line mode. In practice,
    // this isn't true - digraph entry is stopped, but command line mode continues
    typeText(":<C-K>O<Esc>K")
    assertExIsActive()
    assertExText("K")
  }
}
