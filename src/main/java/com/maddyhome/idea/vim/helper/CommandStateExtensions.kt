/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:JvmName("CommandStateHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.engine
import com.maddyhome.idea.vim.command.ij
import com.maddyhome.idea.vim.newapi.vim

val Editor.isEndAllowed: Boolean
  get() = when (this.editorMode) {
    VimStateMachine.Mode.INSERT, VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT, VimStateMachine.Mode.INSERT_VISUAL, VimStateMachine.Mode.INSERT_SELECT -> true
    VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.OP_PENDING, VimStateMachine.Mode.INSERT_NORMAL -> {
      // One day we'll use a proper insert_normal mode
      if (this.editorMode.inSingleMode) true else usesVirtualSpace
    }
  }

val VimStateMachine.Mode.isEndAllowedIgnoringOnemore: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT, VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT -> true
    VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.OP_PENDING -> false
    VimStateMachine.Mode.INSERT_NORMAL -> false
    VimStateMachine.Mode.INSERT_VISUAL -> true
    VimStateMachine.Mode.INSERT_SELECT -> true
  }

val VimStateMachine.Mode.hasVisualSelection
  get() = when (this) {
    VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT -> true
    VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.INSERT, VimStateMachine.Mode.OP_PENDING -> false
    VimStateMachine.Mode.INSERT_NORMAL -> false
    VimStateMachine.Mode.INSERT_VISUAL -> true
    VimStateMachine.Mode.INSERT_SELECT -> true
  }

val Editor.editorMode
  get() = this.vim.vimStateMachine.mode

/**
 * COMPATIBILITY-LAYER: New method
 * Please see: https://jb.gg/zo8n0r
 */
val Editor.mode
  get() = this.vim.vimStateMachine.mode.ij

/**
 * COMPATIBILITY-LAYER: New method
 * Please see: https://jb.gg/zo8n0r
 */
val CommandState.Mode.isEndAllowed: Boolean
  get() = this.engine.isEndAllowed

var Editor.subMode
  get() = this.vim.vimStateMachine.subMode
  set(value) {
    this.vim.vimStateMachine.subMode = value
  }

@get:JvmName("inNormalMode")
val Editor.inNormalMode
  get() = this.editorMode.inNormalMode

@get:JvmName("inNormalMode")
val VimStateMachine.Mode.inNormalMode
  get() = this == VimStateMachine.Mode.COMMAND || this == VimStateMachine.Mode.INSERT_NORMAL

@get:JvmName("inInsertMode")
val Editor.inInsertMode
  get() = this.editorMode == VimStateMachine.Mode.INSERT || this.editorMode == VimStateMachine.Mode.REPLACE

@get:JvmName("inRepeatMode")
val Editor.inRepeatMode
  get() = this.vim.vimStateMachine.isDotRepeatInProgress

@get:JvmName("inVisualMode")
val Editor.inVisualMode
  get() = this.editorMode.inVisualMode

@get:JvmName("inSelectMode")
val Editor.inSelectMode
  get() = this.editorMode == VimStateMachine.Mode.SELECT || this.editorMode == VimStateMachine.Mode.INSERT_SELECT

val VimEditor.inSelectMode
  get() = this.mode == VimStateMachine.Mode.SELECT || this.mode == VimStateMachine.Mode.INSERT_SELECT

@get:JvmName("inBlockSubMode")
val Editor.inBlockSubMode
  get() = this.subMode == VimStateMachine.SubMode.VISUAL_BLOCK

@get:JvmName("inSingleCommandMode")
val Editor.inSingleCommandMode: Boolean
  get() = this.editorMode.inSingleMode

@get:JvmName("inSingleMode")
val VimStateMachine.Mode.inSingleMode: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT_NORMAL, VimStateMachine.Mode.INSERT_SELECT, VimStateMachine.Mode.INSERT_VISUAL -> true
    else -> false
  }

@get:JvmName("inSingleNormalMode")
val VimStateMachine.Mode.inSingleNormalMode: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT_NORMAL -> true
    else -> false
  }
