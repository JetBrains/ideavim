/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.state.mode

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector

val VimEditor.inVisualMode: Boolean
  get() = injector.vimState.mode is Mode.VISUAL

val VimEditor.inBlockSelection: Boolean
  get() = this.mode.selectionType == SelectionType.BLOCK_WISE

val VimEditor.inNormalMode: Boolean
  get() = this.mode is Mode.NORMAL

val VimEditor.inSelectMode: Boolean
  get() = this.mode is Mode.SELECT

val VimEditor.inCommandLineMode: Boolean
  get() = this.mode is Mode.CMD_LINE

val VimEditor.singleModeActive: Boolean
  get() = this.mode.isSingleModeActive

/**
 * Check if text insertion is allowed. It's true in [Mode.INSERT] or [Mode.REPLACE].
 */
val VimEditor.isInsertionAllowed: Boolean
  get() = this.mode == Mode.INSERT || this.mode == Mode.REPLACE
