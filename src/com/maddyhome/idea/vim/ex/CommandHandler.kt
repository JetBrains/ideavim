/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.noneOfEnum
import java.util.*

/**
 * Base class for all Ex command handlers.
 */
sealed class CommandHandler {

  abstract val argFlags: CommandHandlerFlags
  protected open val optFlags: EnumSet<CommandFlags> = noneOfEnum()

  abstract class ForEachCaret : CommandHandler() {
    abstract fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: ExCommand): Boolean
  }

  abstract class SingleExecution : CommandHandler() {
    abstract fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean
  }

  enum class RangeFlag {
    /**
     * Indicates that a range must be specified with this command
     */
    RANGE_REQUIRED,
    /**
     * Indicates that a range is optional for this command
     */
    RANGE_OPTIONAL,
    /**
     * Indicates that a range can't be specified for this command
     */
    RANGE_FORBIDDEN,
    /**
     * Indicates that the command takes a count, not a range - effects default
     * Works like RANGE_OPTIONAL
     */
    RANGE_IS_COUNT
  }

  enum class ArgumentFlag {
    /**
     * Indicates that an argument must be specified with this command
     */
    ARGUMENT_REQUIRED,
    /**
     * Indicates that an argument is optional for this command
     */
    ARGUMENT_OPTIONAL,
    /**
     * Indicates that an argument can't be specified for this command
     */
    ARGUMENT_FORBIDDEN
  }

  enum class Access {
    /**
     * Indicates that this is a command that modifies the editor
     */
    WRITABLE,
    /**
     * Indicates that this command does not modify the editor
     */
    READ_ONLY,
    /**
     * Indicates that this command handles writability by itself
     */
    SELF_SYNCHRONIZED
  }

  enum class Flag {
    DONT_SAVE_LAST,

    /**
     * This command should not exit visual mode.
     *
     * Vim exits visual mode before command execution, but in this case :action will work incorrect.
     *   With this flag visual mode will not be exited while command execution.
     */
    SAVE_VISUAL
  }

  /**
   * Executes a command. The range and arguments are validated first.
   *
   * @param editor  The editor to run the command in
   * @param context The data context
   * @param cmd     The command as entered by the user
   * @param count   The count entered by the user prior to the command
   * @throws ExException if the range or argument is invalid or unable to run the command
   */
  @Throws(ExException::class)
  fun process(editor: Editor, context: DataContext, cmd: ExCommand, count: Int): Boolean {

    // No range allowed
    if (RangeFlag.RANGE_FORBIDDEN == argFlags.rangeFlag && cmd.ranges.size() != 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_norange))
      throw NoRangeAllowedException()
    }

    if (RangeFlag.RANGE_REQUIRED == argFlags.rangeFlag && cmd.ranges.size() == 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_rangereq))
      throw MissingRangeException()
    }

    if (RangeFlag.RANGE_IS_COUNT == argFlags.rangeFlag) {
      cmd.ranges.setDefaultLine(1)
    }

    // Argument required
    if (ArgumentFlag.ARGUMENT_REQUIRED == argFlags.argumentFlag && cmd.argument.isEmpty()) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_argreq))
      throw MissingArgumentException()
    }

    if (ArgumentFlag.ARGUMENT_FORBIDDEN == argFlags.argumentFlag && cmd.argument.isNotEmpty()) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_argforb))
      throw NoArgumentAllowedException()
    }
    editor.commandState.flags = optFlags
    if (editor.inVisualMode && Flag.SAVE_VISUAL !in argFlags.flags) {
      editor.exitVisualMode()
    }

    val res = Ref.create(true)
    try {
      when (this) {
        is ForEachCaret -> {
          editor.caretModel.runForEachCaret({ caret ->
            var i = 0
            while (i++ < count && res.get()) {
              res.set(execute(editor, caret, context, cmd))
            }
          }, true)
        }
        is SingleExecution -> {
          var i = 0
          while (i++ < count && res.get()) {
            res.set(execute(editor, context, cmd))
          }
        }
      }

      if (!res.get()) {
        VimPlugin.indicateError()
      }
      return res.get()
    } catch (e: ExException) {
      VimPlugin.showMessage(e.message)
      VimPlugin.indicateError()
      return false
    }
  }
}

data class CommandHandlerFlags(
  val rangeFlag: CommandHandler.RangeFlag,
  val argumentFlag: CommandHandler.ArgumentFlag,
  val access: CommandHandler.Access,
  val flags: Set<CommandHandler.Flag>
)
