/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

/**
 * Decides whether the occurrences of a text are searched ignoring case
 */
internal fun interface CaseSensitivity {
  fun ignoresCase(text: String): Boolean
}

/**
 * vim-multiple-cursors ignores 'ignorecase' and 'smartcase'
 */
internal val AlwaysCaseSensitive = CaseSensitivity { false }
