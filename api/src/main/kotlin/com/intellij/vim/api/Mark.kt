/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import org.jetbrains.annotations.Range

/**
 * Represents a Vim mark.
 */
interface Mark {
  /**
   * The character key of the mark (a-z for local marks, A-Z for global marks).
   */
  val key: Char

  /**
   * The 0-based line number of the mark.
   */
  val line: @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int

  /**
   * The 0-based column number of the mark.
   */
  val col: @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int

  /**
   * The file path where the mark is located.
   */
  val filePath: Path
}

interface Path {
  val protocol: String
  val path: Array<String>
}