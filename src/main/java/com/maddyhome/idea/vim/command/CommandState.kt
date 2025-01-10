/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.VimStateMachine
import org.jetbrains.annotations.ApiStatus

/**
 * COMPATIBILITY-LAYER: Additional class
 * Please see: https://jb.gg/zo8n0r
 */
@Deprecated("Use `injector.vimState`")
@ApiStatus.ScheduledForRemoval
class CommandState(private val machine: VimStateMachine) {

  val mode: Mode
    get() {
      val myMode = machine.mode
      return when (myMode) {
        is com.maddyhome.idea.vim.state.mode.Mode.CMD_LINE -> Mode.CMD_LINE
        com.maddyhome.idea.vim.state.mode.Mode.INSERT -> Mode.INSERT
        is com.maddyhome.idea.vim.state.mode.Mode.NORMAL -> Mode.COMMAND
        is com.maddyhome.idea.vim.state.mode.Mode.OP_PENDING -> Mode.OP_PENDING
        com.maddyhome.idea.vim.state.mode.Mode.REPLACE -> Mode.REPLACE
        is com.maddyhome.idea.vim.state.mode.Mode.SELECT -> Mode.SELECT
        is com.maddyhome.idea.vim.state.mode.Mode.VISUAL -> Mode.VISUAL
      }
    }

  @get:Deprecated("Use `KeyHandler.keyHandlerState.commandBuilder", ReplaceWith(
    "KeyHandler.getInstance().keyHandlerState.commandBuilder",
    "com.maddyhome.idea.vim.KeyHandler"
  )
  )
  @get:ApiStatus.ScheduledForRemoval
  val commandBuilder: CommandBuilder
    get() = KeyHandler.getInstance().keyHandlerState.commandBuilder

  @Deprecated("Use `KeyHandler.keyHandlerState.mappingState", ReplaceWith(
    "KeyHandler.getInstance().keyHandlerState.mappingState",
    "com.maddyhome.idea.vim.KeyHandler"
  )
  )
  val mappingState: MappingState
    get() = KeyHandler.getInstance().keyHandlerState.mappingState

  enum class Mode {
    // Basic modes
    COMMAND, VISUAL, SELECT, INSERT, CMD_LINE, /*EX*/

    // Additional modes
    OP_PENDING, REPLACE /*, VISUAL_REPLACE*/, INSERT_NORMAL, INSERT_VISUAL, INSERT_SELECT
  }

  enum class SubMode {
    NONE, VISUAL_CHARACTER, VISUAL_LINE, VISUAL_BLOCK
  }

  companion object {
    @JvmStatic
    @Deprecated("Use `injector.vimState`")
    @ApiStatus.ScheduledForRemoval
    fun getInstance(editor: Editor): CommandState {
      return CommandState(injector.vimState)
    }
  }
}
