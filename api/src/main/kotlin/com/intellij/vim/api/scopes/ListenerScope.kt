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
  suspend fun onModeChange(callback: suspend VimScope.(Mode) -> Unit)
  suspend fun onYank(callback: suspend VimScope.(Map<CaretId, Range.Simple>) -> Unit)

  suspend fun onEditorCreate(callback: suspend VimScope.() -> Unit)
  suspend fun onEditorRelease(callback: suspend VimScope.() -> Unit)
  suspend fun onEditorFocusGain(callback: suspend VimScope.() -> Unit)
  suspend fun onEditorFocusLost(callback: suspend VimScope.() -> Unit)

  suspend fun onMacroRecordingStart(callback: suspend VimScope.() -> Unit)
  suspend fun onMacroRecordingFinish(callback: suspend VimScope.() -> Unit)

  suspend fun onIdeaVimEnabled(callback: suspend VimScope.() -> Unit)
  suspend fun onIdeaVimDisabled(callback: suspend VimScope.() -> Unit)

  /**
   * Register a callback to be invoked when a global option changes.
   *
   * @param optionName The name of the option to listen for changes
   * @param callback The callback to invoke when the option changes
   */
  suspend fun onGlobalOptionChange(optionName: String, callback: suspend VimScope.() -> Unit)
}
