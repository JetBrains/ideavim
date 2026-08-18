/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Provides a stable id for the window (split) that an editor lives in
 *
 * IdeaVim's [VimEditor] is per split *and* per tab - opening another file in the same split creates a new [VimEditor].
 * A Vim window, on the other hand, keeps its identity when the buffer it shows changes. This service provides the
 * identity of the containing window, so that state which Vim scopes to a window (e.g. the jump list) can outlive a
 * change of buffer.
 */
interface VimWindowIdService {
  /**
   * The id of the window (split) containing the given editor, or null if the editor doesn't belong to one - e.g. the
   * fallback window, a diff view, or a console
   */
  fun getWindowId(editor: VimEditor): String?
}
