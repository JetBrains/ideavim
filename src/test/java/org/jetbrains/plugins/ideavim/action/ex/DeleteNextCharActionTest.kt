/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

class DeleteNextCharActionTest : VimExTestCase() {
  @Test
  fun `test delete deletes character under caret`() {
    typeText(":set<Left>")
    typeText("<Del>")
    assertExText("se")
  }

  @Test
  fun `test delete at end of string deletes character to left of caret`() {
    typeText(":set")
    typeText("<Del>")
    assertExText("se")
  }

  @Test
  fun `test delete with no text cancels entry`() {
    typeText(":set")
    typeText("<Del><Del><Del><Del>")
    assertExIsDeactivated()
  }

  @Test
  fun `test delete deletes emoji as single grapheme`() {
    typeText(":echo 👍<Left>")
    typeText("<Del>")
    assertExText("echo ")
  }

  @Test
  fun `test delete deletes emoji with skin tone modifier as single grapheme`() {
    typeText(":echo 👍🏽<Left><Left>")
    typeText("<Del>")
    assertExText("echo ")
  }

  @Test
  fun `test delete deletes flag emoji as single grapheme`() {
    typeText(":echo 🇺🇸<Left><Left>")
    typeText("<Del>")
    assertExText("echo ")
  }

  @Test
  fun `test delete deletes emoji zwj sequence as single grapheme`() {
    // Family emoji (man-woman-boy) uses ZWJ (Zero Width Joiner)
    typeText(":echo 👨‍👩‍👦<Left><Left>")
    typeText("<Del>")
    assertExText("echo ")
  }

  @Test
  fun `test delete deletes combining character with base as single grapheme`() {
    // e with combining acute accent (é)
    typeText(":echo e\u0301<Left><Left>")
    typeText("<Del>")
    assertExText("echo ")
  }
}
