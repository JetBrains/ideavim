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

  @Test
  fun `test repeat insert at previous insert position`() {
    // In vim, repeating a `gi` insert behaves like `i` at the caret: the `.` inserts on line 2, not back at the
    // previous insert position (:help gi). Verified against Vim 9.1.
    doTest(
      "giX<Esc>" + "j0" + ".",
      """
        Lorem ${c}ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        Lorem Xipsum dolor
        ${c}Xsit amet consectetur
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat insert at previous insert position started by a mapping`() {
    // A change made through a mapping is repeated by running the mapping again, but that must not bring back the `gi`
    // it runs: the repeat is still an insert at the caret (:help gi). Verified against Vim 9.1.
    doTest(
      ":nmap \\d gi<CR>" + "iX<Esc>" + "j0" + "\\dY<Esc>" + "j0" + ".",
      """
        Lorem ${c}ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        Lorem XYipsum dolor
        ${c}Ysit amet consectetur
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat insert at previous insert position started by a mapping keeps the count typed before the mapping`() {
    // Verified against Vim 9.1: `2\dY<Esc>` inserts `YY` at the previous insert position, and `.` inserts `YY` at
    // the caret
    doTest(
      ":nmap \\d gi<CR>" + "iX<Esc>" + "j0" + "2\\dY<Esc>" + "j0" + ".",
      """
        Lorem ${c}ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        Lorem XYYipsum dolor
        Y${c}Ysit amet consectetur
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat insert at previous insert position started by a mapping keeps the count of the mapping`() {
    // The count is part of the mapping: so far, only running the mapping again applied it. Verified against Vim 9.1:
    // the repeat inserts `ZZ` at the caret
    doTest(
      ":nmap xy 2gi<CR>" + "iX<Esc>" + "j0" + "xyZ<Esc>" + "j0" + ".",
      """
        Lorem ${c}ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        Lorem XZZipsum dolor
        Z${c}Zsit amet consectetur
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat insert at previous insert position typed in front of a mapping that starts with gi`() {
    // The `y` typed after `gi` is on record before it's known not to start the `gix` mapping, and it's the first
    // character of the insert. Verified against Vim 9.1
    doTest(
      ":nmap gix dd<CR>" + "iQ<Esc>" + "j0" + "giyZ<Esc>" + "j0" + ".",
      """
        Lorem ${c}ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        Lorem QyZipsum dolor
        y${c}Zsit amet consectetur
      """.trimIndent(),
    )
  }
}
