/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

interface Mark {
  val key: Char
  val line: Int
  val col: Int
  val filePath: Path
}

interface Path {
  val protocol: String
  val path: Array<String>
}