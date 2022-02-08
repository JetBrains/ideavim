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
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.newapi.VimEditor
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.services.OptionConstants
import com.maddyhome.idea.vim.vimscript.services.OptionService

@get:VimNlsSafe
val usesVirtualSpace
  get() = (VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, OptionConstants.virtualeditName) as VimString).value == "onemore"

/**
 * Please use `isEndAllowed` based on `Editor` (another extension function)
 * It takes "single command" into account.
 */
val CommandState.Mode.isEndAllowed: Boolean
  get() = when (this) {
    CommandState.Mode.INSERT, CommandState.Mode.VISUAL, CommandState.Mode.SELECT -> true
    CommandState.Mode.COMMAND, CommandState.Mode.CMD_LINE, CommandState.Mode.REPLACE, CommandState.Mode.OP_PENDING -> usesVirtualSpace
    CommandState.Mode.INSERT_NORMAL -> usesVirtualSpace
    CommandState.Mode.INSERT_VISUAL -> usesVirtualSpace
    CommandState.Mode.INSERT_SELECT -> usesVirtualSpace
  }

val Editor.isEndAllowed: Boolean
  get() = when (this.mode) {
    CommandState.Mode.INSERT, CommandState.Mode.VISUAL, CommandState.Mode.SELECT, CommandState.Mode.INSERT_VISUAL, CommandState.Mode.INSERT_SELECT -> true
    CommandState.Mode.COMMAND, CommandState.Mode.CMD_LINE, CommandState.Mode.REPLACE, CommandState.Mode.OP_PENDING, CommandState.Mode.INSERT_NORMAL -> {
      // One day we'll use a proper insert_normal mode
      if (this.mode.inSingleMode) true else usesVirtualSpace
    }
  }

val CommandState.Mode.isEndAllowedIgnoringOnemore: Boolean
  get() = when (this) {
    CommandState.Mode.INSERT, CommandState.Mode.VISUAL, CommandState.Mode.SELECT -> true
    CommandState.Mode.COMMAND, CommandState.Mode.CMD_LINE, CommandState.Mode.REPLACE, CommandState.Mode.OP_PENDING -> false
    CommandState.Mode.INSERT_NORMAL -> false
    CommandState.Mode.INSERT_VISUAL -> true
    CommandState.Mode.INSERT_SELECT -> true
  }

val CommandState.Mode.hasVisualSelection
  get() = when (this) {
    CommandState.Mode.VISUAL, CommandState.Mode.SELECT -> true
    CommandState.Mode.REPLACE, CommandState.Mode.CMD_LINE, CommandState.Mode.COMMAND, CommandState.Mode.INSERT, CommandState.Mode.OP_PENDING -> false
    CommandState.Mode.INSERT_NORMAL -> false
    CommandState.Mode.INSERT_VISUAL -> true
    CommandState.Mode.INSERT_SELECT -> true
  }

val Editor.mode
  get() = this.commandState.mode

val VimEditor.mode
  get() = this.commandState.mode

var Editor.subMode
  get() = this.commandState.subMode
  set(value) {
    this.commandState.subMode = value
  }

@get:JvmName("inNormalMode")
val Editor.inNormalMode
  get() = this.mode.inNormalMode

@get:JvmName("inNormalMode")
val CommandState.Mode.inNormalMode
  get() = this == CommandState.Mode.COMMAND || this == CommandState.Mode.INSERT_NORMAL

@get:JvmName("inInsertMode")
val Editor.inInsertMode
  get() = this.mode == CommandState.Mode.INSERT || this.mode == CommandState.Mode.REPLACE

@get:JvmName("inRepeatMode")
val Editor.inRepeatMode
  get() = this.commandState.isDotRepeatInProgress

@get:JvmName("inVisualMode")
val Editor.inVisualMode
  get() = this.mode.inVisualMode

@get:JvmName("inVisualMode")
val CommandState.Mode.inVisualMode
  get() = this == CommandState.Mode.VISUAL || this == CommandState.Mode.INSERT_VISUAL

@get:JvmName("inSelectMode")
val Editor.inSelectMode
  get() = this.mode == CommandState.Mode.SELECT || this.mode == CommandState.Mode.INSERT_SELECT

val VimEditor.inSelectMode
  get() = this.mode == CommandState.Mode.SELECT || this.mode == CommandState.Mode.INSERT_SELECT

@get:JvmName("inBlockSubMode")
val Editor.inBlockSubMode
  get() = this.subMode == CommandState.SubMode.VISUAL_BLOCK

@get:JvmName("inSingleCommandMode")
val Editor.inSingleCommandMode: Boolean
  get() = this.mode.inSingleMode

@get:JvmName("inSingleMode")
val CommandState.Mode.inSingleMode: Boolean
  get() = when (this) {
    CommandState.Mode.INSERT_NORMAL, CommandState.Mode.INSERT_SELECT, CommandState.Mode.INSERT_VISUAL -> true
    else -> false
  }

@get:JvmName("inSingleNormalMode")
val CommandState.Mode.inSingleNormalMode: Boolean
  get() = when (this) {
    CommandState.Mode.INSERT_NORMAL -> true
    else -> false
  }

@get:JvmName("commandState")
val Editor.commandState
  get() = CommandState.getInstance(this)

val VimEditor.commandState
  get() = CommandState.getInstance(this)

fun CommandState.pushVisualMode(subMode: CommandState.SubMode, prevMode: CommandState.Mode = this.mode) {
  if (prevMode.inSingleMode) {
    popModes()
    pushModes(CommandState.Mode.INSERT_VISUAL, subMode)
  } else {
    pushModes(CommandState.Mode.VISUAL, subMode)
  }
}

fun CommandState.pushSelectMode(subMode: CommandState.SubMode, prevMode: CommandState.Mode = this.mode) {
  if (prevMode.inSingleMode) {
    popModes()
    pushModes(CommandState.Mode.INSERT_SELECT, subMode)
  } else {
    pushModes(CommandState.Mode.SELECT, subMode)
  }
}
