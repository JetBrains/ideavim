/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
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
      typeText(parseKeys("x"))
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
    typeText(parseKeys("dd"))
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
    typeText(parseKeys("dd"))
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
    typeText(parseKeys("dd"))
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
    typeText(parseKeys("dd"))
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
    typeText(parseKeys("dd"))
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
    typeText(parseKeys("cc"))
    assertState(
      """
      1234567890
      $c
      1234567890
      """.trimIndent()
    )
    assertMode(CommandState.Mode.INSERT)
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
    typeText(parseKeys("O"))
    assertState(
      """
      1234567890
      $c
      1234567890
      1234567890
      """.trimIndent()
    )
    assertMode(CommandState.Mode.INSERT)
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
    typeText(parseKeys("o"))
    assertState(
      """
      1234567890
      1234567890
      $c
      1234567890
      """.trimIndent()
    )
    assertMode(CommandState.Mode.INSERT)
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
    typeText(parseKeys("cc"))
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
    typeText(parseKeys("cc"))
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
    typeText(parseKeys("cc"))
    assertState(
      """
      1234567890
      $c
      """.trimIndent()
    )
    assertMode(CommandState.Mode.INSERT)
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
    typeText(parseKeys("dd"))
    assertState(
      """
      $c
      1234567890
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
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
    typeText(parseKeys("dd"))
    assertState(
      """
      ${c}1234567890
      1234567890
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
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
    typeText(parseKeys("cc"))
    assertState(
      """
      1234567890
      $c
      1234567890
      """.trimIndent()
    )
    assertMode(CommandState.Mode.INSERT)
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
    typeText(parseKeys("dd"))
    assertState(
      """
      1234567890
      1234567890
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
  }
}
