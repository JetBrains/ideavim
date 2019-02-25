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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.handler.CaretOrder
import com.maddyhome.idea.vim.handler.ExecuteMethodNotOverriddenException
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.Msg

import java.util.EnumSet

/**
 * Base class for all Ex command handlers.
 */
abstract class CommandHandler {

  val names: Array<CommandName>?
  val argFlags: EnumSet<Flag>
  private val optFlags: EnumSet<CommandFlags>

  private val runForEachCaret: Boolean
  private val caretOrder: CaretOrder

  enum class Flag {
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
    ARGUMENT_FORBIDDEN,
    /**
     * Indicates that the command takes a count, not a range - effects default
     */
    RANGE_IS_COUNT,

    DONT_REOPEN,

    /**
     * Indicates that this is a command that modifies the editor
     */
    WRITABLE,
    /**
     * Indicates that this command does not modify the editor
     */
    READ_ONLY,
    DONT_SAVE_LAST
  }

  /**
   * Create the handler
   *
   * [names] A list of names this command answers to
   * [argFlags] - Range and Arguments commands
   */
  constructor(
          names: Array<CommandName>?,
          argFlags: EnumSet<Flag>,
          runForEachCaret: Boolean = false,
          caretOrder: CaretOrder = CaretOrder.NATIVE,
          optFlags: EnumSet<CommandFlags> = EnumSet.noneOf<CommandFlags>(CommandFlags::class.java)
  ) {
    this.names = names
    this.argFlags = argFlags
    this.optFlags = optFlags

    this.runForEachCaret = runForEachCaret
    this.caretOrder = caretOrder

    @Suppress("LeakingThis")
    CommandParser.getInstance().addHandler(this)
  }

  constructor(argFlags: EnumSet<Flag>, optFlags: EnumSet<CommandFlags>, runForEachCaret: Boolean, caretOrder: CaretOrder) {
    this.names = null
    this.argFlags = argFlags
    this.optFlags = optFlags

    this.runForEachCaret = runForEachCaret
    this.caretOrder = caretOrder
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
    if (Flag.RANGE_FORBIDDEN in argFlags && cmd.ranges.size() != 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_norange))
      throw NoRangeAllowedException()
    }

    if (Flag.RANGE_REQUIRED in argFlags && cmd.ranges.size() == 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_rangereq))
      throw MissingRangeException()
    }

    // Argument required
    if (Flag.ARGUMENT_REQUIRED in argFlags && cmd.argument.isEmpty()) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_argreq))
      throw MissingArgumentException()
    }

    if (Flag.RANGE_IS_COUNT in argFlags) {
      cmd.ranges.setDefaultLine(1)
    }

    CommandState.getInstance(editor).flags = optFlags

    var res = true
    try {
      if (runForEachCaret) {
        EditorHelper.getOrderedCaretsList(editor, caretOrder).forEach { caret ->
          var i = 0
          while (i < count && res) {
            try {
              res = execute(editor, caret, context, cmd)
            } catch (e: ExecuteMethodNotOverriddenException) {
              return false
            }

            i++
          }
        }
      } else {
        var i = 0
        while (i < count && res) {
          try {
            res = execute(editor, context, cmd)
          } catch (e: ExecuteMethodNotOverriddenException) {
            return false
          }

          i++
        }
      }

      if (!res) {
        VimPlugin.indicateError()
      }
      return res
    } catch (e: ExException) {
      VimPlugin.showMessage(e.message)
      VimPlugin.indicateError()
      return false
    }
  }

  /**
   * Performs the action of the handler.
   *
   * @param editor  The editor to perform the action in.
   * @param context The data context
   * @param cmd     The complete Ex command including range, command, and arguments
   * @return True if able to perform the command, false if not
   * @throws ExException if the range or arguments are invalid for the command
   */
  @Throws(ExException::class, ExecuteMethodNotOverriddenException::class)
  open fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    if (!runForEachCaret) throw ExecuteMethodNotOverriddenException(this.javaClass)
    return execute(editor, editor.caretModel.primaryCaret, context, cmd)
  }

  @Throws(ExException::class, ExecuteMethodNotOverriddenException::class)
  open fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: ExCommand): Boolean {
    if (runForEachCaret) throw ExecuteMethodNotOverriddenException(this.javaClass)
    return execute(editor, context, cmd)
  }
}
