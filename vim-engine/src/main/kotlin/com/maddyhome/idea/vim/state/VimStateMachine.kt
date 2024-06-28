/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.state

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.impl.state.VimStateMachineImpl
import com.maddyhome.idea.vim.state.mode.Mode
import java.util.*

/**
 * Used to maintain state before and while entering a Vim command (operator, motion, text object, etc.)
 */
public interface VimStateMachine {
  public val mode: Mode
  public var isDotRepeatInProgress: Boolean
  public var isRegisterPending: Boolean
  public val isReplaceCharacter: Boolean

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
  public var executingCommand: Command?
  public val executingCommandFlags: EnumSet<CommandFlags>

  public fun resetRegisterPending()

  /**
   * Toggles the insert/overwrite state. If currently insert, goto replace mode. If currently replace, goto insert
   * mode.
   */
  public fun toggleInsertOverwrite()

  public companion object {
    private val globalState = VimStateMachineImpl()

    // TODO do we really need this method? Can't we use editor.vimStateMachine?
    public fun getInstance(editor: Any?): VimStateMachine {
      return if (editor == null || injector.globalOptions().ideaglobalmode) {
        globalState
      } else {
        injector.commandStateFor(editor)
      }
    }
  }
}
