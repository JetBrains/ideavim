/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.models

/**
 * Represents a path.
 */
interface Path {
  /**
   * The protocol part of the path.
   */
  val protocol: String

  /**
   * The segments of the path as an array of strings.
   */
  val path: Array<String>

  companion object
}
