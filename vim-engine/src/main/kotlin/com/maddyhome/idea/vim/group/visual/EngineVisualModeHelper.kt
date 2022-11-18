/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor

/**
 * Set selection without calling SelectionListener
 */
fun VimCaret.vimSetSystemSelectionSilently(start: Int, end: Int) =
  SelectionVimListenerSuppressor.lock().use { this.setSelection(start.offset, end.offset) }
