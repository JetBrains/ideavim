/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.impl.state

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandBuilder
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MappingState
import com.maddyhome.idea.vim.common.DigraphResult
import com.maddyhome.idea.vim.common.DigraphSequence
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.annotations.Contract
import java.util.*
import javax.swing.KeyStroke

/**
 * Used to maintain state before and while entering a Vim command (operator, motion, text object, etc.)
 */
public class VimStateMachineImpl : VimStateMachine {
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

  @Deprecated("Please use KeyHandler instead")
  override fun isOperatorPending(mode: Mode): Boolean {
    val keyHandler = KeyHandler.getInstance()
    return keyHandler.isOperatorPending(mode, keyHandler.keyHandlerState)
  }

  override fun isDuplicateOperatorKeyStroke(key: KeyStroke, mode: Mode): Boolean {
    val keyHandler = KeyHandler.getInstance()
    return keyHandler.isDuplicateOperatorKeyStroke(key, mode, keyHandler.keyHandlerState)
  }

  override val executingCommandFlags: EnumSet<CommandFlags>
    get() = executingCommand?.flags ?: noneOfEnum()


  override fun resetRegisterPending() {
    if (isRegisterPending) {
      isRegisterPending = false
    }
  }

  override fun startDigraphSequence() {
    val keyHandler = KeyHandler.getInstance()
    keyHandler.keyHandlerState.digraphSequence.startDigraphSequence()
  }

  override fun startLiteralSequence() {
    val keyHandler = KeyHandler.getInstance()
    keyHandler.keyHandlerState.digraphSequence.startLiteralSequence()
  }

  override fun processDigraphKey(key: KeyStroke, editor: VimEditor): DigraphResult {
    val keyHandler = KeyHandler.getInstance()
    return keyHandler.keyHandlerState.digraphSequence.processKey(key, editor)
  }

  /**
   * Toggles the insert/overwrite state. If currently insert, goto replace mode. If currently replace, goto insert
   * mode.
   */
  override fun toggleInsertOverwrite() {
    val oldMode = this.mode
    var newMode = oldMode
    if (oldMode == Mode.INSERT) {
      newMode = Mode.REPLACE
    } else if (oldMode == Mode.REPLACE) {
      newMode = Mode.INSERT
    }
    if (oldMode != newMode) {
      mode = newMode
    }
  }

  public companion object {
    @Contract(pure = true)
    public fun modeToMappingMode(mode: Mode): MappingMode {
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

public fun Mode.toMappingMode(): MappingMode {
  return when (this) {
    is Mode.NORMAL -> MappingMode.NORMAL
    Mode.INSERT, Mode.REPLACE -> MappingMode.INSERT
    is Mode.VISUAL -> MappingMode.VISUAL
    is Mode.SELECT -> MappingMode.SELECT
    is Mode.CMD_LINE -> MappingMode.CMD_LINE
    is Mode.OP_PENDING -> MappingMode.OP_PENDING
  }
}
