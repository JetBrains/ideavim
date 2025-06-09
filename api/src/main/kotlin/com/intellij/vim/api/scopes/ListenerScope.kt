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
  fun onModeChange(callback: VimScope.(Mode) -> Unit)
  fun onYank(callback: VimScope.(Map<CaretId, Range>) -> Unit)

  fun onEditorCreate(callback: VimScope.() -> Unit)
  fun onEditorRelease(callback: VimScope.() -> Unit)
  fun onEditorFocusGain(callback: VimScope.() -> Unit)
  fun onEditorFocusLost(callback: VimScope.() -> Unit)

  fun onMacroRecordingStart(callback: VimScope.() -> Unit)
  fun onMacroRecordingFinish(callback: VimScope.() -> Unit)

  fun onPluginTurnOn(callback: VimScope.() -> Unit)
  fun onPluginTurnOff(callback: VimScope.() -> Unit)

  fun onReplaceCharChange(callback: VimScope.() -> Unit)
}