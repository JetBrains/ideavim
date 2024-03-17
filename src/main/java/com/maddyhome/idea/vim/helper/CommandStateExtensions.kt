/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("CommandStateHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.hasValue
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inVisualMode

internal val Mode.hasVisualSelection
  get() = when (this) {
    is Mode.VISUAL, is Mode.SELECT -> true
    else -> false
  }

/**
 * COMPATIBILITY-LAYER: New method
 * Please see: https://jb.gg/zo8n0r
 */
public val Editor.mode: CommandState.Mode
  get() {
    val mode = this.vim.vimStateMachine.mode
    return when (mode) {
      is Mode.CMD_LINE -> CommandState.Mode.CMD_LINE
      Mode.INSERT -> CommandState.Mode.INSERT
      is Mode.NORMAL -> CommandState.Mode.COMMAND
      is Mode.OP_PENDING -> CommandState.Mode.OP_PENDING
      Mode.REPLACE -> CommandState.Mode.REPLACE
      is Mode.SELECT -> CommandState.Mode.SELECT
      is Mode.VISUAL -> CommandState.Mode.VISUAL
    }
  }

/**
 * COMPATIBILITY-LAYER: New method
 * Please see: https://jb.gg/zo8n0r
 */
@Deprecated("Please migrate to VimEditor.isEndAllowed which can correctly access virtualedit at the right scope",
  replaceWith = ReplaceWith("VimEditor.isEndAllowed"))
public val CommandState.Mode.isEndAllowed: Boolean
  get() {
    fun possiblyUsesVirtualSpace(): Boolean {
      // virtualedit is GLOBAL_OR_LOCAL_TO_WINDOW. We should be using EFFECTIVE, but we don't have a valid editor (which
      // is why this property is deprecated). Fetch the global value, passing in the fallback window to avoid asserts
      // DO NOT COPY THIS APPROACH - ALWAYS USE A REAL WINDOW FOR NON-GLOBAL OPTIONS!
      return injector.optionGroup.hasValue(Options.virtualedit, OptionAccessScope.GLOBAL(injector.fallbackWindow), OptionConstants.virtualedit_onemore)
    }

    return when (this) {
      CommandState.Mode.INSERT, CommandState.Mode.VISUAL, CommandState.Mode.SELECT -> true
      CommandState.Mode.COMMAND, CommandState.Mode.CMD_LINE, CommandState.Mode.REPLACE, CommandState.Mode.OP_PENDING -> possiblyUsesVirtualSpace()
      CommandState.Mode.INSERT_NORMAL, CommandState.Mode.INSERT_VISUAL, CommandState.Mode.INSERT_SELECT -> possiblyUsesVirtualSpace()
    }
  }

public val Mode.inNormalMode: Boolean
  get() = this is Mode.NORMAL

@get:JvmName("inInsertMode")
public val Editor.inInsertMode: Boolean
  get() = this.vim.mode == Mode.INSERT || this.vim.mode == Mode.REPLACE

@get:JvmName("inVisualMode")
public val Editor.inVisualMode: Boolean
  get() = this.vim.inVisualMode

@get:JvmName("inExMode")
internal val Editor.inExMode
  get() = this.vim.vimStateMachine.mode is Mode.CMD_LINE
