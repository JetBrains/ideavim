/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Rings the bell for an error. The caller decides whether the bell is wanted at all ('errorbells', 'visualbell').
 */
interface VimBell {
  fun ring()
}
