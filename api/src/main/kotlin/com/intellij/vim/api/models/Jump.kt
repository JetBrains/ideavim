/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.models

import org.jetbrains.annotations.Range

/**
 * Represents a Vim jump location.
 */
data class Jump(
  /**
   * The 0-based line number of the jump.
   */
  val line: @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int,

  /**
   * The 0-based column number of the jump.
   */
  val col: @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int,

  /**
   * The file path where the jump is located.
   */
  val filepath: Path,
)