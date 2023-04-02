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
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.engine
import com.maddyhome.idea.vim.command.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope

internal val VimStateMachine.Mode.hasVisualSelection
  get() = when (this) {
    VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT -> true
    VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.INSERT, VimStateMachine.Mode.OP_PENDING -> false
    VimStateMachine.Mode.INSERT_NORMAL -> false
    VimStateMachine.Mode.INSERT_VISUAL -> true
    VimStateMachine.Mode.INSERT_SELECT -> true
  }

internal val Editor.editorMode
  get() = this.vim.vimStateMachine.mode

/**
 * COMPATIBILITY-LAYER: New method
 * Please see: https://jb.gg/zo8n0r
 */
public val Editor.mode: CommandState.Mode
  get() = this.vim.vimStateMachine.mode.ij

/**
 * COMPATIBILITY-LAYER: New method
 * Please see: https://jb.gg/zo8n0r
 */
@Deprecated("Please migrate to VimEditor.isEndAllowed which can correctly access virtualedit at the right scope",
  replaceWith = ReplaceWith("VimEditor.isEndAllowed"))
public val CommandState.Mode.isEndAllowed: Boolean
  get() {
    fun possiblyUsesVirtualSpace(): Boolean {
      // virtualedit is GLOBAL_OR_LOCAL_TO_WINDOW. We should NOT be using the global value!
      return injector.optionGroup.hasValue(Options.virtualedit, OptionScope.GLOBAL, OptionConstants.virtualedit_onemore)
    }

    return when (this.engine) {
      VimStateMachine.Mode.INSERT, VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT -> true
      VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.OP_PENDING -> possiblyUsesVirtualSpace()
      VimStateMachine.Mode.INSERT_NORMAL, VimStateMachine.Mode.INSERT_VISUAL, VimStateMachine.Mode.INSERT_SELECT -> possiblyUsesVirtualSpace()
    }
  }

internal var Editor.subMode
  get() = this.vim.vimStateMachine.subMode
  set(value) {
    this.vim.vimStateMachine.subMode = value
  }

@get:JvmName("inNormalMode")
internal val Editor.inNormalMode
  get() = this.editorMode.inNormalMode

@get:JvmName("inNormalMode")
public val VimStateMachine.Mode.inNormalMode: Boolean
  get() = this == VimStateMachine.Mode.COMMAND || this == VimStateMachine.Mode.INSERT_NORMAL

@get:JvmName("inInsertMode")
public val Editor.inInsertMode: Boolean
  get() = this.editorMode == VimStateMachine.Mode.INSERT || this.editorMode == VimStateMachine.Mode.REPLACE

@get:JvmName("inVisualMode")
public val Editor.inVisualMode: Boolean
  get() = this.editorMode.inVisualMode

@get:JvmName("inSelectMode")
internal val Editor.inSelectMode
  get() = this.editorMode == VimStateMachine.Mode.SELECT || this.editorMode == VimStateMachine.Mode.INSERT_SELECT

@get:JvmName("inBlockSubMode")
internal val Editor.inBlockSubMode
  get() = this.subMode == VimStateMachine.SubMode.VISUAL_BLOCK
