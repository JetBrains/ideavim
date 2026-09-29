/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * `.` applied to the insert- and change-commands that had no dot-repeat coverage.
 *
 * `i`, `a`, `A`, `o` and `R` were already covered elsewhere; these are the rest. Every test goes
 * through [doTest] so that it is diffed against real vim when the suite runs with `-Dnvim=true`.
 */
class RepeatInsertCommandsTest : VimTestCase() {

  @Test
  fun `test repeat insert before first non-blank`() {
    doTest(
      "I>><Esc>" + "j" + ".",
      """
          ${c}Lorem ipsum dolor
          sit amet consectetur
      """.trimIndent(),
      """
          >>Lorem ipsum dolor
          >${c}>sit amet consectetur
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat open line above`() {
    doTest(
      "Oconsectetur<Esc>" + ".",
      """
        Lorem ipsum dolor
        ${c}sit amet
      """.trimIndent(),
      """
        Lorem ipsum dolor
        consectetu${c}r
        consectetur
        sit amet
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat substitute character`() {
    doTest(
      "sX<Esc>" + "l" + ".",
      "${c}Lorem ipsum dolor",
      "X${c}Xrem ipsum dolor",
    )
  }

  @Test
  fun `test repeat change whole line`() {
    doTest(
      "Sreplaced<Esc>" + "j" + ".",
      """
        ${c}Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        replaced
        replace${c}d
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat change to end of line`() {
    doTest(
      "Cfoo<Esc>" + "j0" + ".",
      """
        ${c}Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        foo
        fo${c}o
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat delete to end of line`() {
    doTest(
      "D" + "j" + ".",
      """
        Lorem${c} ipsum dolor
        sitametconsectetur
      """.trimIndent(),
      """
        Lorem
        sit${c}a
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat toggle case`() {
    doTest(
      "~" + "." + ".",
      "${c}lorem ipsum dolor",
      "LOR${c}em ipsum dolor",
    )
  }

  @Test
  fun `test repeat put after`() {
    doTest(
      "yy" + "p" + ".",
      """
        ${c}Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        Lorem ipsum dolor
        Lorem ipsum dolor
        ${c}Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat put before`() {
    doTest(
      "yy" + "jP" + ".",
      """
        ${c}Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        Lorem ipsum dolor
        ${c}Lorem ipsum dolor
        Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
        Lorem Xipsum dolor
        ${c}Xsit amet consectetur
    """,
    description = "In vim, repeating a `gi` insert behaves like `i` at the caret, so the `.` inserts on " +
      "line 2. IdeaVim re-executes `gi` itself, which jumps back to the previous insert position, so the " +
      "`.` inserts next to the original change and `.` can never be used to apply a `gi` insert elsewhere.",
    shouldBeFixed = true,
  )
  @Test
  fun `test repeat insert at previous insert position`() {
    doTest(
      "giX<Esc>" + "j0" + ".",
      """
        Lorem ${c}ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        Lorem X${c}Xipsum dolor
        sit amet consectetur
      """.trimIndent(),
    )
  }
}
