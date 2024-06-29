/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.impl.state

import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.annotations.Contract
import java.util.*

/**
 * Used to maintain state before and while entering a Vim command (operator, motion, text object, etc.)
 */
class VimStateMachineImpl : VimStateMachine {
  override var mode: Mode = Mode.NORMAL()
  override var isDotRepeatInProgress: Boolean = false
  override var isRegisterPending: Boolean = false
  override var isReplaceCharacter: Boolean = false

  /**
   * The currently executing command
   *
   * This is a complete command, e.g. operator + motion. Some actions/helpers require additional context from flags in
   * the command/argument. Ideally, we would pass the command through KeyHandler#executeVimAction and
   * EditorActionHandlerBase#execute, but we also need to know the command type in MarkGroup#updateMarkFromDelete,
   * which is called via a document change event.
   *
   * This field is reset after the command has been executed.
   */
  override var executingCommand: Command? = null

  override val executingCommandFlags: EnumSet<CommandFlags>
    get() = executingCommand?.flags ?: noneOfEnum()


  override fun resetRegisterPending() {
    if (isRegisterPending) {
      isRegisterPending = false
    }
  }

  override fun reset() {
    mode = Mode.NORMAL()
    isDotRepeatInProgress = false
    isRegisterPending = false
    isReplaceCharacter = false
    executingCommand = null
  }

  companion object {
    @Contract(pure = true)
    fun modeToMappingMode(mode: Mode): MappingMode {
      return when (mode) {
        is Mode.NORMAL -> MappingMode.NORMAL
        Mode.INSERT, Mode.REPLACE -> MappingMode.INSERT
        is Mode.VISUAL -> MappingMode.VISUAL
        is Mode.SELECT -> MappingMode.SELECT
        is Mode.CMD_LINE -> MappingMode.CMD_LINE
        is Mode.OP_PENDING -> MappingMode.OP_PENDING
      }
    }
  }
}

fun Mode.toMappingMode(): MappingMode {
  return when (this) {
    is Mode.NORMAL -> MappingMode.NORMAL
    Mode.INSERT, Mode.REPLACE -> MappingMode.INSERT
    is Mode.VISUAL -> MappingMode.VISUAL
    is Mode.SELECT -> MappingMode.SELECT
    is Mode.CMD_LINE -> MappingMode.CMD_LINE
    is Mode.OP_PENDING -> MappingMode.OP_PENDING
  }
}
