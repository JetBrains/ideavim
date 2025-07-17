/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Mode
import com.intellij.vim.api.Range

interface ListenersScope {
  fun onModeChange(callback: suspend VimScope.(Mode) -> Unit)
  fun onYank(callback: suspend VimScope.(Map<CaretId, Range.Simple>) -> Unit)

  fun onEditorCreate(callback: suspend VimScope.() -> Unit)
  fun onEditorRelease(callback: suspend VimScope.() -> Unit)
  fun onEditorFocusGain(callback: suspend VimScope.() -> Unit)
  fun onEditorFocusLost(callback: suspend VimScope.() -> Unit)

  fun onMacroRecordingStart(callback: suspend VimScope.() -> Unit)
  fun onMacroRecordingFinish(callback: suspend VimScope.() -> Unit)

  fun onIdeaVimEnabled(callback: suspend VimScope.() -> Unit)
  fun onIdeaVimDisabled(callback: suspend VimScope.() -> Unit)
}
