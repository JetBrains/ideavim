/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for the "U" command ("undo line").
 *
 * `:help U` -> "Undo all latest changes on one line, the line where the latest change was made."
 *
 * Unlike `u`, which steps back one change at a time through the undo tree, `U` restores the most
 * recently changed line to the state it had *before the first change was made to it*, no matter how
 * many edits happened on that line. A second `U` toggles the change back. See `u_saveline` /
 * `u_undoline` in Neovim's `undo.c`.
 */
class LineUndoActionTest : VimTestCase() {

  @Test
  fun `test U restores line after multiple changes`() {
    configureByText("${c}one two three")
    // Three separate changes, all on the same line, cursor never leaves the line.
    typeText("cwA<Esc>")   // "A two three"
    typeText("wcwB<Esc>")  // "A B three"
    typeText("wcwC<Esc>")  // "A B C"
    assertState("A B ${c}C")

    typeText("U")
    assertState("${c}one two three")
  }

  @Test
  fun `test U differs from lowercase u`() {
    configureByText("${c}one two three")
    typeText("cwA<Esc>")   // "A two three"
    typeText("wcwB<Esc>")  // "A B three"
    typeText("wcwC<Esc>")  // "A B C"
    assertState("A B ${c}C")

    // Lowercase u only reverts the most recent change, not the whole line.
    typeText("u")
    assertState("A B ${c}three")
  }

  @Test
  fun `test U after a single change restores the line`() {
    configureByText("${c}one two three")
    typeText("cwA<Esc>")
    assertState("${c}A two three")

    typeText("U")
    assertState("${c}one two three")
  }

  @Test
  fun `test U toggles back when pressed twice`() {
    configureByText("${c}one two three")
    typeText("cwA<Esc>")   // "A two three"
    typeText("wcwB<Esc>")  // "A B three"
    assertState("A ${c}B three")

    // First U restores the line.
    typeText("U")
    assertState("${c}one two three")

    // Second U toggles the changes back ("U can be undone with the next U").
    typeText("U")
    assertState("A B three")
  }

  @Test
  fun `test U toggles repeatedly`() {
    configureByText("${c}one two three")
    typeText("cwA<Esc>")
    assertState("${c}A two three")

    typeText("U")
    assertState("${c}one two three")
    typeText("U")
    assertState("A two three")
    typeText("U")
    assertState("${c}one two three")
  }

  @Test
  fun `test U does nothing when the line was not changed`() {
    configureByText("${c}one two three")
    typeText("U")
    assertState("${c}one two three")
  }

  @Test
  fun `test U restores deletions made within the line`() {
    configureByText("${c}hello world")
    typeText("xxx")  // deletes "hel" -> "lo world"
    assertState("${c}lo world")

    typeText("U")
    assertState("${c}hello world")
  }

  @Test
  fun `test U restores insertions made within the line`() {
    configureByText("${c}hello")
    typeText("iABC<Esc>")  // "ABChello"
    assertState("AB${c}Chello")

    typeText("U")
    assertState("${c}hello")
  }

  @Test
  fun `test U restores the cursor column of the first change`() {
    configureByText("hello ${c}world")
    typeText("cwX<Esc>")  // "hello X"
    assertState("hello ${c}X")

    typeText("U")
    assertState("hello ${c}world")
  }

  @Test
  fun `test U restores only the most recently changed line`() {
    configureByText(
      """
        ${c}one
        two
        three
      """.trimIndent(),
    )
    typeText("cwX<Esc>")    // line 0 -> "X"
    typeText("jcwY<Esc>")   // line 1 -> "Y"
    assertState(
      """
        X
        ${c}Y
        three
      """.trimIndent(),
    )

    // U restores the line where the latest change was made (line 1), leaving line 0 changed.
    typeText("U")
    assertState(
      """
        X
        ${c}two
        three
      """.trimIndent(),
    )
  }

  @Test
  fun `test plain u undoes a U`() {
    configureByText("${c}one two")
    typeText("cwX<Esc>")  // "X two"
    assertState("${c}X two")

    typeText("U")
    assertState("${c}one two")

    // U is a normal, undoable change, so plain u reverts it.
    typeText("u")
    assertState("${c}X two")
  }

  @Test
  fun `test U after switching lines back and forth`() {
    configureByText(
      """
        ${c}alpha
        beta
      """.trimIndent(),
    )
    typeText("cwX<Esc>")   // line 0 -> "X"
    typeText("jcwY<Esc>")  // line 1 -> "Y"
    typeText("kcwZ<Esc>")  // back to line 0, change again -> "Z"
    assertState(
      """
        ${c}Z
        Y
      """.trimIndent(),
    )

    // The latest change was on line 0, and its snapshot was retaken when we returned to it,
    // so U restores line 0 to the state it had right before that latest change ("X").
    typeText("U")
    assertState(
      """
        ${c}X
        Y
      """.trimIndent(),
    )
  }
}
