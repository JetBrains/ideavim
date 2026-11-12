/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.annotations.TestWithPrimaryClipboard
import kotlin.test.assertEquals

/**
 * The `*` register read from PRIMARY, which holds text only: the type of a put comes from what IdeaVim wrote there,
 * as long as PRIMARY still holds that text.
 */
class PrimaryRegisterTest : VimTestCase() {
  @TestWithPrimaryClipboard
  fun `test yank to primary register is written to PRIMARY`() {
    configureByText("${c}Lorem\nipsum")
    typeText("\"*yy")
    assertEquals("Lorem\n", primarySelection.currentText)
  }

  @TestWithPrimaryClipboard
  fun `test linewise yank is put linewise while IdeaVim owns PRIMARY`() {
    configureByText("${c}Lorem\nipsum")
    typeText("\"*yy", "j", "\"*p")
    assertState("Lorem\nipsum\n${c}Lorem\n")
  }

  @TestWithPrimaryClipboard
  fun `test text of another application is put characterwise`() {
    configureByText("${c}Lorem\nipsum")
    typeText("\"*yy")
    primarySelection.writeFromOtherApplication("dolor")
    typeText("j", "\"*P")
    assertState("Lorem\ndolo${c}ripsum")
  }

  // Another application can republish our line without the trailing newline, the type still comes from our yank
  @TestWithPrimaryClipboard
  fun `test our linewise yank keeps its type when PRIMARY comes back without the newline`() {
    configureByText("${c}Lorem\nipsum")
    typeText("\"*yy")
    primarySelection.writeFromOtherApplication("Lorem")
    typeText("j", "\"*p")
    assertState("Lorem\nipsum\n${c}Lorem\n")
  }

  @TestWithPrimaryClipboard
  fun `test text of another application that only looks like our yank is not given its type`() {
    configureByText("${c}Lorem\nipsum")
    typeText("\"*yy")
    primarySelection.writeFromOtherApplication("Lore")
    typeText("j", "\"*P")
    assertState("Lorem\nLor${c}eipsum")
  }
}
