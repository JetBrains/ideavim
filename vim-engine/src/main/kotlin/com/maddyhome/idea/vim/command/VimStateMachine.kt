/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimActionsInitiator
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.DigraphResult
import com.maddyhome.idea.vim.common.DigraphSequence
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.key.CommandPartNode
import org.jetbrains.annotations.Contract
import java.util.*
import javax.swing.KeyStroke

/**
 * Used to maintain state before and while entering a Vim command (operator, motion, text object, etc.)
 *
 * // TODO: 21.02.2022 This constructor should be empty
 */
public class VimStateMachine(private val editor: VimEditor?) {
  public val commandBuilder: CommandBuilder = CommandBuilder(getKeyRootNode(MappingMode.NORMAL))
  private val modeStates = Stack<ModeState>()
  public val mappingState: MappingState = MappingState()
  public val digraphSequence: DigraphSequence = DigraphSequence()
  public var isRecording: Boolean = false
    set(value) {
      field = value
      doShowMode()
    }
  public var isDotRepeatInProgress: Boolean = false
  public var isRegisterPending: Boolean = false
  public var isReplaceCharacter: Boolean = false
    set(value) {
      field = value
      onModeChanged()
    }

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
  public var executingCommand: Command? = null
    private set

  public val isOperatorPending: Boolean
    get() = mappingState.mappingMode == MappingMode.OP_PENDING && !commandBuilder.isEmpty

  init {
    pushModes(defaultModeState.mode, defaultModeState.subMode)
  }

  public fun isDuplicateOperatorKeyStroke(key: KeyStroke?): Boolean {
    return isOperatorPending && commandBuilder.isDuplicateOperatorKeyStroke(key!!)
  }

  public fun setExecutingCommand(cmd: Command) {
    executingCommand = cmd
  }

  public val executingCommandFlags: EnumSet<CommandFlags>
    get() = executingCommand?.flags ?: noneOfEnum()

  public fun pushModes(mode: Mode, submode: SubMode) {
    val newModeState = ModeState(mode, submode)

    logger.debug("Push new mode state: ${newModeState.toSimpleString()}")
    logger.debug { "Stack of mode states before push: ${toSimpleString()}" }

    val previousMode = currentModeState()
    modeStates.push(newModeState)
    setMappingMode()

    if (previousMode != newModeState) {
      onModeChanged()
    }
  }

  public fun popModes() {
    val popped = modeStates.pop()
    setMappingMode()
    if (popped != currentModeState()) {
      onModeChanged()
    }

    logger.debug("Popped mode state: ${popped.toSimpleString()}")
    logger.debug { "Stack of mode states after pop: ${toSimpleString()}" }
  }

  public fun resetOpPending() {
    if (mode == Mode.OP_PENDING) {
      popModes()
    }
  }

  public fun resetReplaceCharacter() {
    if (isReplaceCharacter) {
      isReplaceCharacter = false
    }
  }

  public fun resetRegisterPending() {
    if (isRegisterPending) {
      isRegisterPending = false
    }
  }

  private fun resetModes() {
    modeStates.clear()
    pushModes(defaultModeState.mode, defaultModeState.subMode)
    onModeChanged()
    setMappingMode()
  }

  private fun onModeChanged() {
    if (editor != null) {
      editor.updateCaretsVisualAttributes()
      editor.updateCaretsVisualPosition()
    } else {
      injector.application.localEditors().forEach { editor ->
        editor.updateCaretsVisualAttributes()
        editor.updateCaretsVisualPosition()
      }
    }
    doShowMode()
  }

  private fun setMappingMode() {
    mappingState.mappingMode = modeToMappingMode(mode)
  }

  public val mode: Mode
    get() = currentModeState().mode

  public var subMode: SubMode
    get() = currentModeState().subMode
    set(submode) {
      val modeState = currentModeState()
      popModes()
      pushModes(modeState.mode, submode)
    }

  public fun startDigraphSequence() {
    digraphSequence.startDigraphSequence()
  }

  public fun startLiteralSequence() {
    digraphSequence.startLiteralSequence()
  }

  public fun processDigraphKey(key: KeyStroke, editor: VimEditor): DigraphResult {
    return digraphSequence.processKey(key, editor)
  }

  public fun resetDigraph() {
    digraphSequence.reset()
  }

  /**
   * Toggles the insert/overwrite state. If currently insert, goto replace mode. If currently replace, goto insert
   * mode.
   */
  public fun toggleInsertOverwrite() {
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
  public fun reset() {
    executingCommand = null
    resetModes()
    commandBuilder.resetInProgressCommandPart(getKeyRootNode(mappingState.mappingMode))
    digraphSequence.reset()
  }

  public fun toSimpleString(): String = modeStates.joinToString { it.toSimpleString() }

  /**
   * It's a bit more complicated
   *
   *  Neovim
   * :h mode()
   *
   * - mode(expr)          Return a string that indicates the current mode.
   *
   *   If "expr" is supplied and it evaluates to a non-zero Number or
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
  public fun toVimNotation(): String {
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
      Mode.INSERT_VISUAL -> when (subMode) {
        SubMode.VISUAL_CHARACTER -> "v"
        SubMode.VISUAL_LINE -> "V"
        SubMode.VISUAL_BLOCK -> "\u0016"
        else -> error("Unexpected state")
      }
      else -> error("Unexpected state")
    }
  }

  private fun currentModeState(): ModeState {
    return if (modeStates.size > 0) modeStates.peek() else defaultModeState
  }

  private fun doShowMode() {
    val msg = StringBuilder()
    if (injector.globalOptions().isSet(Options.showmode)) {
      msg.append(getStatusString())
    }
    if (isRecording) {
      if (msg.isNotEmpty()) {
        msg.append(" - ")
      }
      msg.append(injector.messages.message("show.mode.recording"))
    }
    injector.messages.showMode(editor, msg.toString())
  }

  public fun getStatusString(): String {
    val pos = modeStates.size - 1
    val modeState = if (pos >= 0) {
      modeStates[pos]
    } else {
      defaultModeState
    }
    return buildString {
      when (modeState.mode) {
        Mode.INSERT_NORMAL -> append("-- (insert) --")
        Mode.INSERT -> append("-- INSERT --")
        Mode.REPLACE -> append("-- REPLACE --")
        Mode.VISUAL -> {
          append("-- VISUAL")
          when (modeState.subMode) {
            SubMode.VISUAL_LINE -> append(" LINE")
            SubMode.VISUAL_BLOCK -> append(" BLOCK")
            else -> Unit
          }
          append(" --")
        }
        Mode.SELECT -> {
          append("-- SELECT")
          when (modeState.subMode) {
            SubMode.VISUAL_LINE -> append(" LINE")
            SubMode.VISUAL_BLOCK -> append(" BLOCK")
            else -> Unit
          }
          append(" --")
        }
        Mode.INSERT_VISUAL -> {
          append("-- (insert) VISUAL")
          when (modeState.subMode) {
            SubMode.VISUAL_LINE -> append(" LINE")
            SubMode.VISUAL_BLOCK -> append(" BLOCK")
            else -> Unit
          }
          append(" --")
        }
        Mode.INSERT_SELECT -> {
          append("-- (insert) SELECT")
          when (modeState.subMode) {
            SubMode.VISUAL_LINE -> append(" LINE")
            SubMode.VISUAL_BLOCK -> append(" BLOCK")
            else -> Unit
          }
          append(" --")
        }
        else -> Unit
      }
    }
  }

  public enum class Mode {
    // Basic modes
    COMMAND, VISUAL, SELECT, INSERT, CMD_LINE, /*EX*/

    // Additional modes
    OP_PENDING, REPLACE /*, VISUAL_REPLACE*/, INSERT_NORMAL, INSERT_VISUAL, INSERT_SELECT
  }

  public enum class SubMode {
    NONE, VISUAL_CHARACTER, VISUAL_LINE, VISUAL_BLOCK
  }

  private data class ModeState(val mode: Mode, val subMode: SubMode) {
    fun toSimpleString(): String = "$mode:$subMode"
  }

  public companion object {
    private val logger = vimLogger<VimStateMachine>()
    private val defaultModeState = ModeState(Mode.COMMAND, SubMode.NONE)
    private val globalState = VimStateMachine(null)

    /**
     * COMPATIBILITY-LAYER: Method switched to Any (was VimEditor)
     * Please see: https://jb.gg/zo8n0r
     */
    @JvmStatic
    public fun getInstance(editor: Any?): VimStateMachine {
      return if (editor == null || injector.globalOptions().isSet(Options.ideaglobalmode)) {
        globalState
      } else {
        injector.commandStateFor(editor)
      }
    }

    private fun getKeyRootNode(mappingMode: MappingMode): CommandPartNode<VimActionsInitiator> {
      return injector.keyGroup.getKeyRoot(mappingMode)
    }

    @Contract(pure = true)
    public fun modeToMappingMode(mode: Mode): MappingMode {
      return when (mode) {
        Mode.COMMAND -> MappingMode.NORMAL
        Mode.INSERT, Mode.REPLACE -> MappingMode.INSERT
        Mode.VISUAL -> MappingMode.VISUAL
        Mode.SELECT -> MappingMode.SELECT
        Mode.CMD_LINE -> MappingMode.CMD_LINE
        Mode.OP_PENDING -> MappingMode.OP_PENDING
        Mode.INSERT_NORMAL -> MappingMode.NORMAL
        Mode.INSERT_VISUAL -> MappingMode.VISUAL
        Mode.INSERT_SELECT -> MappingMode.SELECT
      }
    }
  }
}
