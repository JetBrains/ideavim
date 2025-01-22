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

/**
 * Returns true if the current mode is Command-line and there is a pending Visual selection
 *
 * Returns false if the current mode isn't Command-line. For example, `v/foo` will return true, as the editor is in
 * Command-line mode for the search, but was originally in Visual mode, and will return to Visual when the search is
 * completed.
 */
val VimEditor.inCommandLineModeWithVisual: Boolean
  get() = (this.mode as? Mode.CMD_LINE)?.isVisualPending == true

val VimEditor.singleModeActive: Boolean
  get() = this.mode.isSingleModeActive

/**
 * Check if text insertion is allowed. It's true in [Mode.INSERT] or [Mode.REPLACE].
 */
val VimEditor.isInsertionAllowed: Boolean
  get() = this.mode == Mode.INSERT || this.mode == Mode.REPLACE
