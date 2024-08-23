/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.forgetAllReplaceMasks
import com.maddyhome.idea.vim.state.mode.Mode

abstract class VimEditorBase : VimEditor {
  override var mode: Mode
    get() = injector.vimState.mode
    set(value) {
      val vimState = injector.vimState
      if (vimState.mode == value) return

      val oldValue = vimState.mode
      if (oldValue == Mode.REPLACE) {
        forgetAllReplaceMasks()
      }
      updateMode(value)
      injector.listenersNotifier.notifyModeChanged(this, oldValue)
    }

  override var isReplaceCharacter: Boolean
    get() = injector.vimState.isReplaceCharacter
    set(value) {
      val vimState = injector.vimState
      if (value != vimState.isReplaceCharacter) {
        updateIsReplaceCharacter(value)
        injector.listenersNotifier.notifyIsReplaceCharChanged(this)
      }
    }

  protected abstract fun updateMode(mode: Mode)
  protected abstract fun updateIsReplaceCharacter(isReplaceCharacter: Boolean)
}