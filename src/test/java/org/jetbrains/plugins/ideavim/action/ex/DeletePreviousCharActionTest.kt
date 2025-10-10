/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class DeletePreviousCharActionTest : VimExTestCase() {
  @Test
  fun `test backspace deletes character in front of caret`() {
    typeText(":set incsearch<BS>")
    assertExText("set incsearc")

    typeText("<C-H>")
    assertExText("set incsear")
  }

  @Test
  fun `test backspace at start of empty line cancels entry`() {
    typeText(":<BS>")
    assertExIsDeactivated()

    deactivateExEntry()

    typeText(":set<BS><BS><BS><BS>")
    assertExIsDeactivated()

    deactivateExEntry()

    typeText(":<C-H>")
    assertExIsDeactivated()
  }

  @Test
  fun `test backspace at start of non-empty line does not cancel entry`() {
    typeText(":set<C-B>")
    assertExOffset(0)
    typeText("<BS>")
    assertExIsActive()
  }

  @Test
  fun `test backspace deletes emoji as single grapheme`() {
    typeText(":echo 👍<BS>")
    assertExText("echo ")
  }

  @Test
  fun `test backspace deletes emoji with skin tone modifier as single grapheme`() {
    typeText(":echo 👍🏽<BS>")
    assertExText("echo ")
  }

  @Test
  fun `test backspace deletes flag emoji as single grapheme`() {
    typeText(":echo 🇺🇸<BS>")
    assertExText("echo ")
  }

  @Test
  fun `test backspace deletes emoji zwj sequence as single grapheme`() {
    // Family emoji (man-woman-boy) uses ZWJ (Zero Width Joiner)
    typeText(":echo 👨‍👩‍👦<BS>")
    assertExText("echo ")
  }

  @Test
  fun `test backspace deletes combining character with base as single grapheme`() {
    // e with combining acute accent (é)
    typeText(":echo e\u0301<BS>")
    assertExText("echo ")
  }
}
