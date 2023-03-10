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
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.engine
import com.maddyhome.idea.vim.command.ij
import com.maddyhome.idea.vim.newapi.vim

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
public val CommandState.Mode.isEndAllowed: Boolean
  get() = this.engine.isEndAllowed

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
