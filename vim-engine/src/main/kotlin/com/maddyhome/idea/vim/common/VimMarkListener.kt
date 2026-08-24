/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

/**
 * Notified after mark has been modified.
 */
interface VimMarkListener : Listener {

  /**
   * @param markChar the mark that changed, or `null` when an unknown number of marks may have changed - a batch reset,
   * or an editor that has just been opened and whose marks have never been seen. Listeners that only care about some of
   * the marks still have to do the full work when this is `null`.
   */
  fun marksChanged(markChar: Char?)
}
