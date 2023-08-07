/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.match

/**
 * The resulting match of a capture group
 */
public class VimMatchGroup(
  /**
   * The range of indices in the editor text of where the match was found
   */
  public val range: IntRange,

  /**
   * The string value of the match found
   */
  public val value: String
)