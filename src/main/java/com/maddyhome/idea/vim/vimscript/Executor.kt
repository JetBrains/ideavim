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
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimScriptExecutorBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.FinishException
import com.maddyhome.idea.vim.history.HistoryConstants
import com.maddyhome.idea.vim.newapi.vim
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

@Service
class Executor : VimScriptExecutorBase() {
  private val logger = logger<Executor>()
  override var executingVimscript = false

  @Throws(ExException::class)
  override fun execute(script: String, editor: VimEditor, context: ExecutionContext, skipHistory: Boolean, indicateErrors: Boolean, vimContext: VimLContext?): ExecutionResult {
    var finalResult: ExecutionResult = ExecutionResult.Success

    val myScript = VimscriptParser.parse(script)
    myScript.units.forEach { it.vimContext = vimContext ?: myScript }

    for (unit in myScript.units) {
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
        if (injector.application.isUnitTest()) {
          throw e
        }
      }
    }

    if (!skipHistory) {
      VimPlugin.getHistory().addEntry(HistoryConstants.COMMAND, script)
      if (myScript.units.size == 1 && myScript.units[0] is Command && myScript.units[0] !is RepeatCommand) {
        VimPlugin.getRegister().storeTextSpecial(LAST_COMMAND_REGISTER, script)
      }
    }
    return finalResult
  }

  override fun execute(script: String, skipHistory: Boolean) {
    val editor = TextComponentEditorImpl(null, JTextArea()).vim
    val context = DataContext.EMPTY_CONTEXT.vim
    execute(script, editor, context, skipHistory, indicateErrors = true, CommandLineVimLContext)
  }

  override fun executeFile(file: File, indicateErrors: Boolean) {
    val editor = TextComponentEditorImpl(null, JTextArea()).vim
    val context = DataContext.EMPTY_CONTEXT.vim
    try {
      execute(file.readText(), editor, context, skipHistory = true, indicateErrors)
    } catch (ignored: IOException) { }
  }

  @Throws(ExException::class)
  override fun executeLastCommand(editor: VimEditor, context: ExecutionContext): Boolean {
    val reg = VimPlugin.getRegister().getRegister(':') ?: return false
    val text = reg.text ?: return false
    execute(text, editor, context, skipHistory = false, indicateErrors = true, CommandLineVimLContext)
    return true
  }
}
