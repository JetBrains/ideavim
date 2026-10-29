/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.mark.DiffSide
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.mark.JumpSource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VimJumpServiceBaseTest {
  private val scope = "project"
  private val path = "/project/Changed.txt"
  private val rightSide = JumpSource.Diff("diff-1", DiffSide.RIGHT, isInFile = true)

  private val jumpService = object : VimJumpServiceBase() {
    override var lastJumpTimeStamp: Long = 0
    override fun includeCurrentCommandAsNavigation(editor: VimEditor) {}
  }

  @Test
  fun `test a jump to the same place replaces the older one`() {
    jumpService.addJump(scope, Jump(3, 0, path, "file", rightSide), true)
    jumpService.addJump(scope, Jump(3, 4, path, "file", rightSide), true)

    assertEquals(listOf(Jump(3, 4, path, "file", rightSide)), jumpService.getJumps(scope))
  }

  @Test
  fun `test the same line in a diff and in a plain editor are kept as two jumps`() {
    jumpService.addJump(scope, Jump(3, 0, path, "file", rightSide), true)
    jumpService.addJump(scope, Jump(3, 0, path, "file", JumpSource.File), true)

    assertEquals(
      listOf(Jump(3, 0, path, "file", rightSide), Jump(3, 0, path, "file", JumpSource.File)),
      jumpService.getJumps(scope),
    )
  }

  @Test
  fun `test removing a plain file jump keeps the diff jump on the same line`() {
    jumpService.addJump(scope, Jump(3, 0, path, "file", rightSide), true)
    jumpService.addJump(scope, Jump(3, 0, path, "file", JumpSource.File), true)

    jumpService.removeJump(scope, Jump(3, 0, path, "file", JumpSource.File))

    assertEquals(listOf(Jump(3, 0, path, "file", rightSide)), jumpService.getJumps(scope))
  }

  @Test
  fun `test removing a jump removes it whatever its column`() {
    jumpService.addJump(scope, Jump(3, 2, path, "file"), true)

    jumpService.removeJump(scope, Jump(3, 0, path, "file"))

    assertEquals(emptyList(), jumpService.getJumps(scope))
  }
}
