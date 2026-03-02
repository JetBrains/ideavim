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
 * Listener for 'number' and 'relativenumber' option changes.
 * Delegates to VimEditorGroup to update line number display.
 *
 * Extracted from EditorGroup to avoid a compile-time dependency on the frontend module.
 */
object NumberChangeListener : EffectiveOptionValueChangeListener {
  override fun onEffectiveValueChanged(editor: VimEditor) {
    injector.editorGroup.onNumberOptionChanged(editor)
  }
}
