/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class GuardedBlocksTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  fun `test delete char with block`() {
    configureAndGuard("[123${c}4567890]")
    assertDoesNotThrow {
      typeText(injector.parser.parseKeys("x"))
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  fun `test delete line with block`() {
    configureAndGuard(
      """
      [1234567890
      ]123${c}4567890[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      1234567890
      $c
      1234567890
      """.trimIndent(),
    )
  }

  // Probably it's better to put the caret after 1
  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  @Disabled("Caret positioning incorrect after deleting line with guarded block extending into previous line")
  fun `test delete line with block and longer start`() {
    configureAndGuard(
      """
      [1234567890
      1]23${c}4567890[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      1234567890
      ${c}1
      1234567890
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  @Disabled("Caret positioning incorrect after deleting line with guarded block extending into next line")
  fun `test delete line with block and shorter end`() {
    configureAndGuard(
      """
      [1234567890
      ]123${c}456789[0
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      1234567890
      ${c}0
      1234567890
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  @Disabled("Delete operation should be blocked when line is fully within guarded blocks")
  fun `test delete line fully unmodifiable`() {
    configureAndGuard(
      """
      [123${c}4567890
      ]123456789[0
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      123${c}4567890
      1234567890
      1234567890
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  @Disabled("Delete operation should be blocked when caret is in line fully within guarded blocks")
  fun `test delete line fully unmodifiable end`() {
    configureAndGuard(
      """
      [1234567890
      ]123456789[0
      123456${c}7890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      1234567890
      1234567890
      123456${c}7890
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  @Disabled("Change line operation with guarded blocks needs proper implementation")
  fun `test change line with block`() {
    configureAndGuard(
      """
      [1234567890
      ]123${c}4567890[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("cc"))
    assertState(
      """
      1234567890
      $c
      1234567890
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  fun `test change line with block1`() {
    configureAndGuard(
      """
      [1234567890
      ]123${c}4567890[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("O"))
    assertState(
      """
      1234567890
      $c
      1234567890
      1234567890
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  fun `test change line with block2`() {
    configureAndGuard(
      """
      [1234567890
      ]123${c}4567890[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("o"))
    assertState(
      """
      1234567890
      1234567890
      $c
      1234567890
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  @Disabled("Caret positioning incorrect after changing line with guarded block extending into previous line")
  fun `test change line with block with longer start`() {
    configureAndGuard(
      """
      [1234567890
      1]23${c}4567890[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("cc"))
    assertState(
      """
      1234567890
      1$c
      1234567890
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  @Disabled("Caret positioning incorrect after changing line with guarded block extending into next line")
  fun `test change line with block with shorter end`() {
    configureAndGuard(
      """
      [1234567890
      ]123${c}456789[0
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("cc"))
    assertState(
      """
      1234567890
      ${c}0
      1234567890
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  @Disabled("Change line operation with guarded block at end needs proper implementation")
  fun `test change line with block at the end`() {
    configureAndGuard(
      """
      [1234567890
      ]12345${c}67890
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("cc"))
    assertState(
      """
      1234567890
      $c
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  fun `test delete line near the guard`() {
    configureAndGuard(
      """
      123456${c}7890[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      $c
      1234567890
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  @Disabled("Caret positioning incorrect after deleting line adjacent to guarded block with line above")
  fun `test delete line near the guard with line above`() {
    configureAndGuard(
      """
      1234567890
      123456${c}7890[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      ${c}1234567890
      1234567890
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  fun `test change line near the guard with line above`() {
    configureAndGuard(
      """
      1234567890
      123456${c}7890[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("cc"))
    assertState(
      """
      1234567890
      $c
      1234567890
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  @Test
  fun `test delete line near the guard with line above on empty line`() {
    configureAndGuard(
      """
      1234567890
      $c[
      1234567890]
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      1234567890
      1234567890
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
  }
}
