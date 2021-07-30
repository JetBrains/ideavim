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

package com.maddyhome.idea.vim.ex

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.ThrowableComputable
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.GoalCommand
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExCommand.Constants.MAX_RECURSION
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.group.HistoryGroup
import com.maddyhome.idea.vim.group.RegisterGroup
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.vimscript.Executor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import javax.swing.JTextArea

data class ExCommand(val ranges: Ranges, var command: String, var argument: String, val originalString: String) :
  Executable {

  private val logger = logger<ExCommand>()

  private object Constants {
    const val MAX_RECURSION = 100
  }

  override fun execute(
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
    skipHistory: Boolean,
  ): ExecutionResult {
    processCommand(editor, context, MAX_RECURSION, skipHistory)
    return ExecutionResult.Success
  }

  @kotlin.jvm.Throws(ExException::class)
  private fun processCommand(
    editor: Editor?,
    context: DataContext?,
    aliasCountdown: Int,
    skipHistory: Boolean,
  ) {

    if (aliasCountdown == MAX_RECURSION && !skipHistory) {
      VimPlugin.getHistory().addEntry(HistoryGroup.COMMAND, originalString)
    }

    if (command.isEmpty()) {
      logger.warn("CMD is empty")
      return
    }

    // If there is a command alias for the entered text, then process the alias and return that
    // instead of the original command.

    if (VimPlugin.getCommand().isAlias(command)) {
      // ugly piece of s to support nullable editor and data context
      val nonNullEditor: Editor = editor ?: TextComponentEditorImpl(null, JTextArea())
      val nonNullContext: DataContext = context ?: DataContext.EMPTY_CONTEXT
      if (aliasCountdown > 0) {
        val commandAlias = VimPlugin.getCommand().getAliasCommand(originalString, 1)
        when (commandAlias) {
          is GoalCommand.Ex -> {
            if (commandAlias.command.isEmpty()) {
              logger.warn("Command alias is empty")
              return
            }
            Executor.execute(commandAlias.command, editor, context, skipHistory)
          }
          // todo nullable editor & context

          is GoalCommand.Call -> commandAlias.handler.execute(nonNullEditor, nonNullContext)
        }.let { }
      } else {
        VimPlugin.showMessage(MessageHelper.message("recursion.detected.maximum.alias.depth.reached"))
        VimPlugin.indicateError()
        logger.warn("Recursion detected, maximum alias depth reached. ")
      }
      return
    }

    // Parse the command
    val handler = getCommandHandler()
    if (handler == null) {
      val message = MessageHelper.message(Msg.NOT_EX_CMD, command)
      throw InvalidCommandException(message, null)
    }
    if (editor != null) {
      if (handler.argFlags.access === CommandHandler.Access.WRITABLE && !editor.document.isWritable) {
        VimPlugin.indicateError()
        logger.info("Trying to modify readonly document")
        return
      }
    }

    // ugly piece of s to support nullable editor and data context
    val nonNullEditor: Editor = editor ?: TextComponentEditorImpl(null, JTextArea())
    val nonNullContext: DataContext = context ?: DataContext.EMPTY_CONTEXT

    // Run the command
    val runCommand = ThrowableComputable<Any?, ExException> {
      // todo nullable editor & context
      val ok = handler.process(nonNullEditor, nonNullContext, this)
      if (ok && !handler.argFlags.flags.contains(CommandHandler.Flag.DONT_SAVE_LAST)) {
        val commandAsString = command + if (argument.isNotBlank()) " $argument" else ""
        VimPlugin.getRegister().storeTextSpecial(RegisterGroup.LAST_COMMAND_REGISTER, commandAsString)
      }
      null
    }
    when (handler.argFlags.access) {
      CommandHandler.Access.WRITABLE -> ApplicationManager.getApplication().runWriteAction(runCommand)
      CommandHandler.Access.READ_ONLY -> ApplicationManager.getApplication().runReadAction(runCommand)
      CommandHandler.Access.SELF_SYNCHRONIZED -> runCommand.compute()
    }
  }

  fun getCommandHandler(): CommandHandler? {
    // See if the user entered a supported command by checking each character entered
    var node: CommandNode = root
    for (char in command) {
      node = node.getChild(char) ?: return null
    }
    val handlerHolder = node.commandHandler
    return handlerHolder?.instance
  }

  companion object CommandHandlersTree {
    val EX_COMMAND_EP = ExtensionPointName.create<ExBeanClass>("IdeaVIM.vimExCommand")
    private val root = CommandNode()

    fun unregisterHandlers() {
      root.clear()
    }

    /**
     * Registers all the supported Ex commands
     */
    fun registerHandlers() {
      EX_COMMAND_EP.extensions().forEach(ExBeanClass::register)
      registerEpListener()
    }

    private fun registerEpListener() {
      // IdeaVim doesn't support contribution to ex_command_ep extension point, so technically we can skip this update,
      //   but let's support dynamic plugins in a more classic way and reload handlers on every EP change.
      EX_COMMAND_EP.addChangeListener(
        {
          unregisterHandlers()
          registerHandlers()
        },
        VimPlugin.getInstance()
      )
    }

    /** Adds a command handler to the parser  */
    fun addHandler(handlerHolder: ExBeanClass) {
      // Iterator through each command name alias
      val names: Array<CommandName> = when {
        handlerHolder.names != null -> {
          commands(*handlerHolder.names!!.split(",").toTypedArray())
        }
        handlerHolder.instance is ComplicatedNameExCommand -> {
          (handlerHolder.instance as ComplicatedNameExCommand).names
        }
        else -> throw RuntimeException("Cannot create an ex command: $handlerHolder")
      }
      for (name in names) {
        var node = root
        var text = name.required
        // Build a tree for each character in the required portion of the command name
        for (i in 0 until text.length - 1) {
          var cn = node.getChild(text[i])
          if (cn == null) {
            cn = node.addChild(text[i], null)
          }
          node = cn
        }

        // For the last character we need to add the actual handler
        var cn = node.getChild(text[text.length - 1])
        if (cn == null) {
          cn = node.addChild(text[text.length - 1], handlerHolder)
        } else {
          cn.commandHandler = handlerHolder
        }
        node = cn

        // Now add the handler for each character in the optional portion of the command name
        text = name.optional
        for (i in text.indices) {
          cn = node.getChild(text[i])
          if (cn == null) {
            cn = node.addChild(text[i], handlerHolder)
          } else if (cn.commandHandler == null) {
            cn.commandHandler = handlerHolder
          }
          node = cn
        }
      }
    }
  }
  fun getLine(editor: Editor): Int = ranges.getLine(editor)

  fun getLine(editor: Editor, caret: Caret): Int = ranges.getLine(editor, caret)

  fun getCount(editor: Editor, defaultCount: Int, checkCount: Boolean): Int {
    val count = if (checkCount) countArgument else -1

    val res = ranges.getCount(editor, count)
    return if (res == -1) defaultCount else res
  }

  fun getCount(editor: Editor, caret: Caret, defaultCount: Int, checkCount: Boolean): Int {
    val count = ranges.getCount(editor, caret, if (checkCount) countArgument else -1)
    return if (count == -1) defaultCount else count
  }

  fun getLineRange(editor: Editor): LineRange = ranges.getLineRange(editor, -1)

  fun getLineRange(editor: Editor, caret: Caret, checkCount: Boolean = false): LineRange {
    return ranges.getLineRange(editor, caret, if (checkCount) countArgument else -1)
  }

  fun getTextRange(editor: Editor, checkCount: Boolean): TextRange {
    val count = if (checkCount) countArgument else -1
    return ranges.getTextRange(editor, count)
  }

  fun getTextRange(editor: Editor, caret: Caret, checkCount: Boolean): TextRange {
    return ranges.getTextRange(editor, caret, if (checkCount) countArgument else -1)
  }

  private val countArgument: Int
    get() = argument.toIntOrNull() ?: -1
}
