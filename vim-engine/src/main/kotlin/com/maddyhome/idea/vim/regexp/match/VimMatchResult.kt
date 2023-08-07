/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.match

/**
 * The result of matching a pattern against an editor
 */
public sealed class VimMatchResult {

  /**
   * Successful match
   */
  public data class Success(
    /**
     * The range of indices in the editor text of where the match was found
     */
    public val range: IntRange,

    /**
     * The string value of the match found
     */
    public val value: String,

    /**
     * The results of sub-matches corresponding to capture groups
     */
    public val groups: VimMatchGroupCollection
  ) : VimMatchResult()

  /**
   * Match was unsuccessful or not found
   */
  public object Failure : VimMatchResult()
}