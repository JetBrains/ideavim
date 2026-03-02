/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener

/**
 * Delegates scroll option changes to the scroll group implementation.
 * Extracted from ScrollGroup to allow ScrollGroup to live in the frontend module
 * while this listener remains in common (used by VimListenerManager).
 */
object ScrollOptionsChangeListener : EffectiveOptionValueChangeListener {
  override fun onEffectiveValueChanged(editor: VimEditor) {
    injector.scroll.onScrollOptionChanged(editor)
  }
}
