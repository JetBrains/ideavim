@file:JvmName("CommandStateHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.command.CommandState

/**
 * @author Alex Plate
 */

private val modesWithEndAllowed = setOf(CommandState.Mode.INSERT, CommandState.Mode.REPEAT, CommandState.Mode.VISUAL, CommandState.Mode.SELECT)
val CommandState.Mode.isEndAllowed
  get() = this in modesWithEndAllowed

val Editor.mode
  get() = CommandState.getInstance(this).mode

var Editor.subMode
  get() = CommandState.getInstance(this).subMode
  set(value) {
    CommandState.getInstance(this).subMode = value
  }

@get:JvmName("inInsertMode")
val Editor.inInsertMode
  get() = this.mode == CommandState.Mode.INSERT || this.mode == CommandState.Mode.REPLACE

@get:JvmName("inRepeatMode")
val Editor.inRepeatMode
  get() = this.mode == CommandState.Mode.REPEAT

@get:JvmName("inVisualMode")
val Editor.inVisualMode
  get() = this.mode == CommandState.Mode.VISUAL

@get:JvmName("inSelectMode")
val Editor.inSelectMode
  get() = this.mode == CommandState.Mode.SELECT

@get:JvmName("inBlockSubMode")
val Editor.inBlockSubMode
  get() = this.subMode == CommandState.SubMode.VISUAL_BLOCK

@get:JvmName("inSingleCommandMode")
val Editor.inSingleCommandMode
  get() = this.subMode == CommandState.SubMode.SINGLE_COMMAND && this.mode == CommandState.Mode.COMMAND
