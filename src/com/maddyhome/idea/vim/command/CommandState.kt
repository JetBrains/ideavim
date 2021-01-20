/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
package com.maddyhome.idea.vim.command

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.DigraphResult
import com.maddyhome.idea.vim.helper.DigraphSequence
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.helper.vimCommandState
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.option.OptionsManager.showmode
import org.jetbrains.annotations.Contract
import java.util.*
import javax.swing.KeyStroke

/**
 * Used to maintain state while entering a Vim command (operator, motion, text object, etc.)
 */
class CommandState private constructor() {
  val commandBuilder = CommandBuilder(getKeyRootNode(MappingMode.NORMAL))
  private val modeStates = Stack<ModeState>()
  val mappingState = MappingState()
  private val digraphSequence = DigraphSequence()
  var isRecording = false
    set(value) {
      field = value
      updateStatus()
    }
  var isDotRepeatInProgress = false

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
  var executingCommand: Command? = null
    private set

  val isOperatorPending: Boolean
    get() = mappingState.mappingMode == MappingMode.OP_PENDING && !commandBuilder.isEmpty

  fun isDuplicateOperatorKeyStroke(key: KeyStroke?): Boolean {
    return isOperatorPending && commandBuilder.isDuplicateOperatorKeyStroke(key!!)
  }

  fun setExecutingCommand(cmd: Command) {
    executingCommand = cmd
  }

  val executingCommandFlags: EnumSet<CommandFlags>
    get() = executingCommand?.flags ?: noneOfEnum()

  fun pushModes(mode: Mode, submode: SubMode) {
    val newModeState = ModeState(mode, submode)
    logger.debug("Push new mode state: ${newModeState.toSimpleString()}")
    logger.debug { "Stack of mode states before push: ${toSimpleString()}" }
    modeStates.push(newModeState)
    setMappingMode()
    updateStatus()
  }

  fun popModes() {
    val popped = modeStates.pop()
    setMappingMode()
    updateStatus()
    logger.debug("Popped mode state: ${popped.toSimpleString()}")
    logger.debug { "Stack of mode states after pop: ${toSimpleString()}" }
  }

  fun resetOpPending() {
    if (mode == Mode.OP_PENDING) {
      popModes()
    }
  }

  fun resetRegisterPending() {
    if (subMode == SubMode.REGISTER_PENDING) {
      popModes()
    }
  }

  private fun resetModes() {
    modeStates.clear()
    setMappingMode()
  }

  private fun setMappingMode() {
    val modeState = currentModeState()
    mappingState.mappingMode = if (modeState.mode == Mode.OP_PENDING) MappingMode.OP_PENDING else modeToMappingMode(mode)
  }

  @Contract(pure = true)
  private fun modeToMappingMode(mode: Mode): MappingMode {
    return when (mode) {
      Mode.COMMAND -> MappingMode.NORMAL
      Mode.INSERT, Mode.REPLACE -> MappingMode.INSERT
      Mode.VISUAL -> MappingMode.VISUAL
      Mode.SELECT -> MappingMode.SELECT
      Mode.CMD_LINE -> MappingMode.CMD_LINE
      else -> error("Unexpected mode: $mode")
    }
  }

  val mode: Mode
    get() = currentModeState().mode

  var subMode: SubMode
    get() = currentModeState().subMode
    set(submode) {
      val modeState = currentModeState()
      popModes()
      pushModes(modeState.mode, submode)
      updateStatus()
    }

  fun startDigraphSequence() {
    digraphSequence.startDigraphSequence()
  }

  fun startLiteralSequence() {
    digraphSequence.startLiteralSequence()
  }

  fun processDigraphKey(key: KeyStroke, editor: Editor): DigraphResult {
    return digraphSequence.processKey(key, editor)
  }

  fun resetDigraph() {
    digraphSequence.reset()
  }

  /**
   * Toggles the insert/overwrite state. If currently insert, goto replace mode. If currently replace, goto insert
   * mode.
   */
  fun toggleInsertOverwrite() {
    val oldMode = mode
    var newMode = oldMode
    if (oldMode == Mode.INSERT) {
      newMode = Mode.REPLACE
    } else if (oldMode == Mode.REPLACE) {
      newMode = Mode.INSERT
    }
    if (oldMode != newMode) {
      val modeState = currentModeState()
      popModes()
      pushModes(newMode, modeState.subMode)
    }
  }

  /**
   * Resets the command, mode, visual mode, and mapping mode to initial values.
   */
  fun reset() {
    executingCommand = null
    resetModes()
    commandBuilder.resetInProgressCommandPart(getKeyRootNode(mappingState.mappingMode))
    digraphSequence.reset()
    updateStatus()
  }

  fun toSimpleString(): String = modeStates.joinToString { it.toSimpleString() }

  /**
   * It's a bit more complicated
   *
   *  Neovim
   * :h mode()
   *
   * - mode([expr])          Return a string that indicates the current mode.
   *
   *   If [expr] is supplied and it evaluates to a non-zero Number or
   *   a non-empty String (|non-zero-arg|), then the full mode is
   *   returned, otherwise only the first letter is returned.
   *
   *   n          Normal
   *   no         Operator-pending
   *   nov        Operator-pending (forced characterwise |o_v|)
   *   noV        Operator-pending (forced linewise |o_V|)
   *   noCTRL-V   Operator-pending (forced blockwise |o_CTRL-V|)
   *   niI        Normal using |i_CTRL-O| in |Insert-mode|
   *   niR        Normal using |i_CTRL-O| in |Replace-mode|
   *   niV        Normal using |i_CTRL-O| in |Virtual-Replace-mode|
   *   v          Visual by character
   *   V          Visual by line
   *   CTRL-V     Visual blockwise
   *   s          Select by character
   *   S          Select by line
   *   CTRL-S     Select blockwise
   *   i          Insert
   *   ic         Insert mode completion |compl-generic|
   *   ix         Insert mode |i_CTRL-X| completion
   *   R          Replace |R|
   *   Rc         Replace mode completion |compl-generic|
   *   Rv         Virtual Replace |gR|
   *   Rx         Replace mode |i_CTRL-X| completion
   *   c          Command-line editing
   *   cv         Vim Ex mode |gQ|
   *   ce         Normal Ex mode |Q|
   *   r          Hit-enter prompt
   *   rm         The -- more -- prompt
   *   r?         |:confirm| query of some sort
   *   !          Shell or external command is executing
   *   t          Terminal mode: keys go to the job
   *   This is useful in the 'statusline' option or when used
   *   with |remote_expr()| In most other places it always returns
   *   "c" or "n".
   *   Note that in the future more modes and more specific modes may
   *   be added. It's better not to compare the whole string but only
   *   the leading character(s).
   */
  @VimNlsSafe
  fun toVimNotation(): String {
    return when (mode) {
      Mode.COMMAND -> "n"
      Mode.VISUAL -> when (subMode) {
        SubMode.VISUAL_CHARACTER -> "v"
        SubMode.VISUAL_LINE -> "V"
        SubMode.VISUAL_BLOCK -> "\u0016"
        else -> error("Unexpected state")
      }
      Mode.INSERT -> "i"
      Mode.SELECT -> when (subMode) {
        SubMode.VISUAL_CHARACTER -> "s"
        SubMode.VISUAL_LINE -> "S"
        SubMode.VISUAL_BLOCK -> "\u0013"
        else -> error("Unexpected state")
      }
      Mode.REPLACE -> "R"
      else -> error("Unexpected state")
    }
  }

  private fun currentModeState(): ModeState {
    return if (modeStates.size > 0) modeStates.peek() else defaultModeState
  }

  private fun updateStatus() {
    val msg = StringBuilder()
    if (showmode.isSet) {
      msg.append(getStatusString(modeStates.size - 1))
    }
    if (isRecording) {
      if (msg.isNotEmpty()) {
        msg.append(" - ")
      }
      msg.append(MessageHelper.message("show.mode.recording"))
    }
    VimPlugin.showMode(msg.toString())
  }

  private fun getStatusString(pos: Int): String {
    val modeState = if (pos >= 0 && pos < modeStates.size) {
      modeStates[pos]
    } else if (pos < 0) {
      defaultModeState
    } else {
      return ""
    }
    return buildString {
      when (modeState.mode) {
        Mode.COMMAND -> if (modeState.subMode == SubMode.SINGLE_COMMAND) {
          append('(').append(getStatusString(pos - 1).toLowerCase()).append(')')
        }
        Mode.INSERT -> append("INSERT")
        Mode.REPLACE -> append("REPLACE")
        Mode.VISUAL, Mode.SELECT -> {
          if (pos > 0) {
            val tmp = modeStates[pos - 1]
            if (tmp.mode == Mode.COMMAND && tmp.subMode == SubMode.SINGLE_COMMAND) {
              append(getStatusString(pos - 1))
              append(" - ")
            }
          }
          when (modeState.subMode) {
            SubMode.VISUAL_LINE -> append(modeState.mode).append(" LINE")
            SubMode.VISUAL_BLOCK -> append(modeState.mode).append(" BLOCK")
            else -> append(modeState.mode)
          }
        }
        else -> Unit
      }
    }
  }

  enum class Mode {
    // Basic modes
    COMMAND, VISUAL, SELECT, INSERT, CMD_LINE,  /*EX*/

    // Additional modes
    OP_PENDING, REPLACE /*, VISUAL_REPLACE, INSERT_NORMAL, INSERT_VISUAL, INSERT_SELECT */
  }

  enum class SubMode {
    NONE, SINGLE_COMMAND, REGISTER_PENDING, VISUAL_CHARACTER, VISUAL_LINE, VISUAL_BLOCK
  }

  private class ModeState(val mode: Mode, val subMode: SubMode) {
    fun toSimpleString(): String = "$mode:$subMode"
  }

  companion object {
    private val logger = Logger.getInstance(CommandState::class.java.name)
    private val defaultModeState = ModeState(Mode.COMMAND, SubMode.NONE)

    @JvmStatic
    @Contract("null -> new")
    fun getInstance(editor: Editor?): CommandState {
      if (editor == null) return CommandState()

      var res = editor.vimCommandState
      if (res == null) {
        res = CommandState()
        editor.vimCommandState = res
      }
      return res
    }

    private fun getKeyRootNode(mappingMode: MappingMode): CommandPartNode = VimPlugin.getKey().getKeyRoot(mappingMode)
  }

  init {
    pushModes(defaultModeState.mode, defaultModeState.subMode)
  }
}
