/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Provides methods to redraw the screen
 *
 * Vim redraws the screen in response to various changes, such as adding/removing lines, scrolling, entering
 * Command-line mode, etc. Typically, in an IDE, redrawing the editor is automatic, but it can be necessary to force a
 * redraw, particularly when updating the status line.
 */
interface VimRedrawService {
  /**
   * Redraw the screen.
   *
   * Used when the screen is scrolled or when lines are added/removed.
   */
  fun redraw()

  /**
   * Clears and redraws the status line.
   *
   * Used when the status line needs to be redrawn, e.g., when entering Command-line mode.
   */
  fun redrawStatusLine()
}
