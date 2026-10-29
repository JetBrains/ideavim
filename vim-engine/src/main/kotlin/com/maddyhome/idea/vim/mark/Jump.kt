/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.mark

// Todo maybe we should use something like IDE's RangeMarker here?
/**
 * @param filepath the file the jump is in. For a side of a diff that is not the file itself, the file the diff is about.
 */
data class Jump(
  var line: Int,
  val col: Int,
  var filepath: String,
  val protocol: String,
  val source: JumpSource = JumpSource.File,
) {
  /** Whether a plain editor of [filepath] shows the place of the jump */
  val isInFile: Boolean
    get() = source !is JumpSource.Diff || source.isInFile

  /** Whether both jumps bring the user to the same place. The jump list keeps one jump per place */
  fun isSamePlaceAs(other: Jump): Boolean {
    return filepath == other.filepath && line == other.line && source == other.source
  }
}
