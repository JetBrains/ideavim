/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.vimscript

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.FinishException
import com.maddyhome.idea.vim.group.HistoryGroup
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_COMMAND_REGISTER
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.RepeatCommand
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import java.io.File
import java.io.IOException
import javax.swing.JTextArea

object Executor {
  private val logger = logger<Executor>()
  var executingVimScript = false

  @Throws(ExException::class)
  fun execute(scriptString: String, editor: Editor, context: DataContext, skipHistory: Boolean, indicateErrors: Boolean = true, vimContext: VimLContext? = null): ExecutionResult {
    var finalResult: ExecutionResult = ExecutionResult.Success

    val script = VimscriptParser.parse(scriptString)
    script.units.forEach { it.vimContext = vimContext ?: script }

    for (unit in script.units) {
      try {
        val result = unit.execute(editor, context)
        if (result is ExecutionResult.Error) {
          finalResult = ExecutionResult.Error
          if (indicateErrors) {
            VimPlugin.indicateError()
          }
        }
      } catch (e: ExException) {
        if (e is FinishException) {
          break
        }
        finalResult = ExecutionResult.Error
        if (indicateErrors) {
          VimPlugin.showMessage(e.message)
          VimPlugin.indicateError()
        } else {
          logger.warn("Failed while executing $unit. " + e.message)
        }
      } catch (e: NotImplementedError) {
        if (indicateErrors) {
          VimPlugin.showMessage("Not implemented yet :(")
          VimPlugin.indicateError()
        }
      } catch (e: Exception) {
        logger.warn("Caught: ${e.message}")
        logger.warn(e.stackTrace.toString())
      }
    }

    if (!skipHistory) {
      VimPlugin.getHistory().addEntry(HistoryGroup.COMMAND, scriptString)
      if (script.units.size == 1 && script.units[0] is Command && script.units[0] !is RepeatCommand) {
        VimPlugin.getRegister().storeTextSpecial(LAST_COMMAND_REGISTER, scriptString)
      }
    }
    return finalResult
  }

  fun execute(scriptString: String, skipHistory: Boolean = true) {
    val editor = TextComponentEditorImpl(null, JTextArea())
    val context = DataContext.EMPTY_CONTEXT
    execute(scriptString, editor, context, skipHistory, indicateErrors = true, CommandLineVimLContext)
  }

  @JvmStatic
  fun executeFile(file: File, indicateErrors: Boolean = false) {
    val editor = TextComponentEditorImpl(null, JTextArea())
    val context = DataContext.EMPTY_CONTEXT
    try {
      execute(file.readText(), editor, context, skipHistory = true, indicateErrors)
    } catch (ignored: IOException) { }
  }

  @Throws(ExException::class)
  fun executeLastCommand(editor: Editor, context: DataContext): Boolean {
    val reg = VimPlugin.getRegister().getRegister(':') ?: return false
    val text = reg.text ?: return false
    execute(text, editor, context, skipHistory = false, indicateErrors = true, CommandLineVimLContext)
    return true
  }
}
