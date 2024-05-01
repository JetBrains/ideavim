/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.state.mode

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.helper.vimStateMachine

public val VimEditor.mode: Mode
  get() = this.vimStateMachine.mode

public val VimEditor.inVisualMode: Boolean
  get() = this.vimStateMachine.mode is Mode.VISUAL

public val VimEditor.inBlockSelection: Boolean
  get() = this.mode.selectionType == SelectionType.BLOCK_WISE

public val VimEditor.inNormalMode: Boolean
  get() = this.mode is Mode.NORMAL

public val VimEditor.inSelectMode: Boolean
  get() = this.mode is Mode.SELECT

public val VimEditor.inCommandLineMode: Boolean
  get() = this.mode is Mode.CMD_LINE

public val VimEditor.singleModeActive: Boolean
  get() = this.mode.isSingleModeActive

/**
 * Check if text insertion is allowed. It's true in [Mode.INSERT] or [Mode.REPLACE].
 */
public val VimEditor.isInsertionAllowed: Boolean
  get() = this.mode == Mode.INSERT || this.mode == Mode.REPLACE
