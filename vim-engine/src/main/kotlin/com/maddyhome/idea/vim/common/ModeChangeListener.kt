/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.state.mode.Mode

interface ModeChangeListener: Listener {
  fun modeChanged(editor: VimEditor, oldMode: Mode)
}

/**
 * Listener that is notified BEFORE a mode change occurs.
 * Use this when you need to execute code while still in the old mode.
 */
interface ModeWillChangeListener : Listener {
  fun modeWillChange(editor: VimEditor, oldMode: Mode, newMode: Mode)
}