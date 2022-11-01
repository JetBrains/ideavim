/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.experimentalApi
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class GuardedBlocksTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test delete char with block`() {
    if (!experimentalApi()) return
    configureAndGuard("[123${c}4567890]")
    try {
      typeText(injector.parser.parseKeys("x"))
    } catch (e: Throwable) {
      // Catch exception
      return
    }
    fail()
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test delete line with block`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      [1234567890
      ]123${c}4567890[
      1234567890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      1234567890
      $c
      1234567890
      """.trimIndent()
    )
  }

/*
  // Probably it's better to put the caret after 1
  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test delete line with block and longer start`() {
    if (!experimentalApi()) return
    configureAndGuard("""
      [1234567890
      1]23${c}4567890[
      1234567890]
      """.trimIndent())
    typeText(injector.parser.parseKeys("dd"))
    assertState("""
      1234567890
      ${c}1
      1234567890
    """.trimIndent())
  }
*/

/*
  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test delete line with block and shorter end`() {
    if (!experimentalApi()) return
    configureAndGuard("""
      [1234567890
      ]123${c}456789[0
      1234567890]
      """.trimIndent())
    typeText(injector.parser.parseKeys("dd"))
    assertState("""
      1234567890
      ${c}0
      1234567890
    """.trimIndent())
  }
*/

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test delete line fully unmodifiable`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      [123${c}4567890
      ]123456789[0
      1234567890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      123${c}4567890
      1234567890
      1234567890
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test delete line fully unmodifiable end`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      [1234567890
      ]123456789[0
      123456${c}7890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      1234567890
      1234567890
      123456${c}7890
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test change line with block`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      [1234567890
      ]123${c}4567890[
      1234567890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("cc"))
    assertState(
      """
      1234567890
      $c
      1234567890
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test change line with block1`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      [1234567890
      ]123${c}4567890[
      1234567890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("O"))
    assertState(
      """
      1234567890
      $c
      1234567890
      1234567890
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test change line with block2`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      [1234567890
      ]123${c}4567890[
      1234567890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("o"))
    assertState(
      """
      1234567890
      1234567890
      $c
      1234567890
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.INSERT)
  }

/*
  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test change line with block with longer start`() {
    if (!experimentalApi()) return
    configureAndGuard("""
      [1234567890
      1]23${c}4567890[
      1234567890]
      """.trimIndent())
    typeText(injector.parser.parseKeys("cc"))
    assertState("""
      1234567890
      1${c}
      1234567890
    """.trimIndent())
    assertMode(CommandState.Mode.INSERT)
  }
*/

/*
  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test change line with block with shorter end`() {
    if (!experimentalApi()) return
    configureAndGuard("""
      [1234567890
      ]123${c}456789[0
      1234567890]
      """.trimIndent())
    typeText(injector.parser.parseKeys("cc"))
    assertState("""
      1234567890
      ${c}0
      1234567890
    """.trimIndent())
    assertMode(CommandState.Mode.INSERT)
  }
*/

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test change line with block at the end`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      [1234567890
      ]12345${c}67890
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("cc"))
    assertState(
      """
      1234567890
      $c
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test delete line near the guard`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      123456${c}7890[
      1234567890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      $c
      1234567890
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.COMMAND)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test delete line near the guard with line above`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      1234567890
      123456${c}7890[
      1234567890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      ${c}1234567890
      1234567890
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.COMMAND)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test change line near the guard with line above`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      1234567890
      123456${c}7890[
      1234567890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("cc"))
    assertState(
      """
      1234567890
      $c
      1234567890
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.INSERT)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.GUARDED_BLOCKS)
  fun `test delete line near the guard with line above on empty line`() {
    if (!experimentalApi()) return
    configureAndGuard(
      """
      1234567890
      $c[
      1234567890]
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("dd"))
    assertState(
      """
      1234567890
      1234567890
      """.trimIndent()
    )
    assertMode(VimStateMachine.Mode.COMMAND)
  }
}
