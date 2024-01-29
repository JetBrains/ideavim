/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.impl.state

import com.maddyhome.idea.vim.action.change.LazyVimCommand
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandBuilder
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MappingState
import com.maddyhome.idea.vim.common.DigraphResult
import com.maddyhome.idea.vim.common.DigraphSequence
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.ReturnTo
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.returnTo
import org.jetbrains.annotations.Contract
import java.util.*
import javax.swing.KeyStroke

/**
 * Used to maintain state before and while entering a Vim command (operator, motion, text object, etc.)
 *
 * // TODO: 21.02.2022 This constructor should be empty
 */
public class VimStateMachineImpl(private val editor: VimEditor?) : VimStateMachine {
  override val commandBuilder: CommandBuilder = CommandBuilder(getKeyRootNode(MappingMode.NORMAL))
  override var mode: Mode = Mode.NORMAL()
    set(value) {
      if (field == value) return

      val oldValue = field
      field = value
      setMappingMode()
      if (editor != null) {
        injector.listenersNotifier.notifyModeChanged(editor, oldValue)
      }
      onModeChanged()
    }
  override val mappingState: MappingState = MappingState()
  override val digraphSequence: DigraphSequence = DigraphSequence()
  override var isRecording: Boolean = false
    set(value) {
      field = value
      doShowMode()
    }
  override var isDotRepeatInProgress: Boolean = false
  override var isRegisterPending: Boolean = false
  override var isReplaceCharacter: Boolean = false
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
  override var executingCommand: Command? = null

  override val isOperatorPending: Boolean
    get() = mappingState.mappingMode == MappingMode.OP_PENDING && !commandBuilder.isEmpty

  override fun isDuplicateOperatorKeyStroke(key: KeyStroke?): Boolean {
    return isOperatorPending && commandBuilder.isDuplicateOperatorKeyStroke(key!!)
  }

  override val executingCommandFlags: EnumSet<CommandFlags>
    get() = executingCommand?.flags ?: noneOfEnum()

  override fun resetOpPending() {
    if (this.mode is Mode.OP_PENDING) {
      val returnTo = this.mode.returnTo
      mode = when (returnTo) {
        ReturnTo.INSERT -> Mode.INSERT
        ReturnTo.REPLACE -> Mode.INSERT
        null -> Mode.NORMAL()
      }
    }
  }

  override fun resetReplaceCharacter() {
    if (isReplaceCharacter) {
      isReplaceCharacter = false
    }
  }

  override fun resetRegisterPending() {
    if (isRegisterPending) {
      isRegisterPending = false
    }
  }

  private fun resetModes() {
//    modeStates.clear()
    mode = Mode.NORMAL()
    onModeChanged()
    setMappingMode()
  }

  private fun onModeChanged() {
    if (editor != null) {
      editor.updateCaretsVisualAttributes()
      editor.updateCaretsVisualPosition()
    } else {
      injector.editorGroup.localEditors().forEach { editor ->
        editor.updateCaretsVisualAttributes()
        editor.updateCaretsVisualPosition()
      }
    }
    doShowMode()
  }

  private fun setMappingMode() {
    mappingState.mappingMode = modeToMappingMode(this.mode)
  }

  override fun startDigraphSequence() {
    digraphSequence.startDigraphSequence()
  }

  override fun startLiteralSequence() {
    digraphSequence.startLiteralSequence()
  }

  override fun processDigraphKey(key: KeyStroke, editor: VimEditor): DigraphResult {
    return digraphSequence.processKey(key, editor)
  }

  override fun resetDigraph() {
    digraphSequence.reset()
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

  /**
   * Resets the command, mode, visual mode, and mapping mode to initial values.
   */
  override fun reset() {
    executingCommand = null
    resetModes()
    commandBuilder.resetInProgressCommandPart(getKeyRootNode(mappingState.mappingMode))
    digraphSequence.reset()
  }

  private fun doShowMode() {
    val msg = StringBuilder()
    if (injector.globalOptions().showmode) {
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

  override fun getStatusString(): String {
    val modeState = this.mode
    return buildString {
      when (modeState) {
        is Mode.NORMAL -> {
          if (modeState.returnTo != null) append("-- (insert) --")
        }

        Mode.INSERT -> append("-- INSERT --")
        Mode.REPLACE -> append("-- REPLACE --")
        is Mode.VISUAL -> {
          val inInsert = if (modeState.returnTo != null) "(insert) " else ""
          append("-- ${inInsert}VISUAL")
          when (modeState.selectionType) {
            SelectionType.LINE_WISE -> append(" LINE")
            SelectionType.BLOCK_WISE -> append(" BLOCK")
            else -> Unit
          }
          append(" --")
        }

        is Mode.SELECT -> {
          val inInsert = if (modeState.returnTo != null) "(insert) " else ""
          append("-- ${inInsert}SELECT")
          when (modeState.selectionType) {
            SelectionType.LINE_WISE -> append(" LINE")
            SelectionType.BLOCK_WISE -> append(" BLOCK")
            else -> Unit
          }
          append(" --")
        }

        else -> Unit
      }
    }
  }

  public companion object {
    private val logger = vimLogger<VimStateMachine>()

    private fun getKeyRootNode(mappingMode: MappingMode): CommandPartNode<LazyVimCommand> {
      return injector.keyGroup.getKeyRoot(mappingMode)
    }

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
