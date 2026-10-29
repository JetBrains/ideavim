/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.mark

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JumpTest {
  private val path = "/project/Changed.txt"

  @Test
  fun `test jumps on the same line of a file are the same place whatever the column`() {
    assertTrue(Jump(3, 0, path, "file").isSamePlaceAs(Jump(3, 7, path, "file")))
  }

  @Test
  fun `test jumps on different lines are different places`() {
    assertFalse(Jump(3, 0, path, "file").isSamePlaceAs(Jump(4, 0, path, "file")))
  }

  @Test
  fun `test jumps in different files are different places`() {
    assertFalse(Jump(3, 0, path, "file").isSamePlaceAs(Jump(3, 0, "/project/Other.txt", "file")))
  }

  @Test
  fun `test the right side of a diff and a plain editor of the same file are different places`() {
    val inDiff = Jump(3, 0, path, "file", JumpSource.Diff("diff-1", DiffSide.RIGHT, isInFile = true))
    val inFile = Jump(3, 0, path, "file", JumpSource.File)

    assertFalse(inDiff.isSamePlaceAs(inFile))
    assertFalse(inFile.isSamePlaceAs(inDiff))
  }

  @Test
  fun `test the two sides of a diff are different places`() {
    val left = Jump(3, 0, path, "file", JumpSource.Diff("diff-1", DiffSide.LEFT, isInFile = false))
    val right = Jump(3, 0, path, "file", JumpSource.Diff("diff-1", DiffSide.RIGHT, isInFile = true))

    assertFalse(left.isSamePlaceAs(right))
  }

  @Test
  fun `test the same side of two diffs are different places`() {
    val first = Jump(3, 0, path, "file", JumpSource.Diff("diff-1", DiffSide.LEFT, isInFile = false))
    val second = Jump(3, 0, path, "file", JumpSource.Diff("diff-2", DiffSide.LEFT, isInFile = false))

    assertFalse(first.isSamePlaceAs(second))
  }

  @Test
  fun `test the same line of the same diff side is the same place`() {
    val first = Jump(3, 0, path, "file", JumpSource.Diff("diff-1", DiffSide.LEFT, isInFile = false))
    val second = Jump(3, 5, path, "file", JumpSource.Diff("diff-1", DiffSide.LEFT, isInFile = false))

    assertTrue(first.isSamePlaceAs(second))
  }
}
