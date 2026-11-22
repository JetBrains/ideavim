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
 * Counts and `.`.
 *
 * `:h .` - "Without a count, the count of the last change is used. If you enter a count, it will
 * replace the last one." Replace, not multiply: `2dd` then `3.` deletes three lines, not six.
 *
 * Vim keeps the count as a field on the recorded change and re-renders the `["x][count]` prefix on
 * replay (`CmdSpec.count` / `redo_prefix()` in nvim's `input.c`), which is why a count means
 * whatever the repeated command decides it means - "three lines" for `dd`, "line three" for `G`,
 * "three characters" for `r`.
 */
class RepeatCountTest : VimTestCase() {

  @Test
  fun `test repeat reuses the count baked into an insert`() {
    doTest(
      "3ifoo<Esc>" + "j0" + ".",
      """
        ${c}Lorem ipsum
        sit amet
      """.trimIndent(),
      """
        foofoofooLorem ipsum
        foofoofo${c}osit amet
      """.trimIndent(),
    )
  }

  @Test
  fun `test count given to dot replaces the original count`() {
    // `2dd` deleted two lines; `3.` must delete three, not six
    doTest(
      "2dd" + "3.",
      """
        ${c}Lorem ipsum
        dolor sit amet
        consectetur adipiscing
        elit sed do
        eiusmod tempor
        incididunt ut labore
      """.trimIndent(),
      "${c}incididunt ut labore",
    )
  }

  @Test
  fun `test dot without a count reuses the original count`() {
    doTest(
      "3dd" + ".",
      """
        ${c}Lorem ipsum
        dolor sit amet
        consectetur adipiscing
        elit sed do
        eiusmod tempor
        incididunt ut labore
        ut enim ad minim
      """.trimIndent(),
      "${c}ut enim ad minim",
    )
  }

  @Test
  fun `test repeat join with a count`() {
    // `3J` joins three lines, i.e. performs two joins - not three
    doTest(
      "3J" + ".",
      """
        ${c}Lorem ipsum
        dolor sit
        amet consectetur
        adipiscing elit
        sed do eiusmod
      """.trimIndent(),
      "Lorem ipsum dolor sit amet consectetur adipiscing elit${c} sed do eiusmod",
    )
  }

  @Test
  fun `test replace character with a count`() {
    // Vim leaves the caret on the last replaced character, see `:h r`
    doTest(
      "3rX",
      "${c}Lorem ipsum dolor",
      "XX${c}Xem ipsum dolor",
    )
  }

  @Test
  fun `test repeat replace character with a count`() {
    // `3rX` leaves the caret on the last replaced character, so `l` moves to the next one and `.`
    // replaces from there. (Vim/Neovim: "XXXXX Xipsum dolor" with the caret on the last X)
    doTest(
      "3rX" + "l" + ".",
      "${c}Lorem ipsum dolor",
      "XXXXX${c}Xipsum dolor",
    )
  }

  @Test
  fun `test count given to dot replaces the operator count but not the motion count`() {
    // VIM-3729 covers `2c3l` + `.`; this pins the count-replacement variant
    doTest(
      "2c3l" + "foo<Esc>" + "j0" + "3.",
      """
        ${c}banana banana banana
        cherry cherry cherry
      """.trimIndent(),
      """
        foo banana banana
        fo${c}orry cherry cherry
      """.trimIndent(),
    )
  }
}
