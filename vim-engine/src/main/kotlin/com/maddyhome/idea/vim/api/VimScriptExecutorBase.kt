/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.FinishException
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_COMMAND_REGISTER
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.RepeatCommand
import java.io.File
import java.io.IOException

abstract class VimScriptExecutorBase : VimscriptExecutor {
  private val logger = vimLogger<VimScriptExecutorBase>()
  override var executingVimscript = false
  override var executingIdeaVimRcConfiguration = false

  @Throws(ExException::class)
  override fun execute(
    script: String,
    editor: VimEditor,
    context: ExecutionContext,
    skipHistory: Boolean,
    indicateErrors: Boolean,
    vimContext: VimLContext?,
  ): ExecutionResult {
    try {
      injector.vimscriptExecutor.executingVimscript = true
      var finalResult: ExecutionResult = ExecutionResult.Success

      val myScript = injector.vimscriptParser.parse(script)
      myScript.units.forEach { it.vimContext = vimContext ?: myScript }

      // Record the command in the history before executing it
      if (!skipHistory) {
        injector.historyGroup.addEntry(VimHistory.Type.Command, script)
      }

      for (unit in myScript.units) {
        try {
          val result = unit.execute(editor, context)
          if (result is ExecutionResult.Error) {
            finalResult = ExecutionResult.Error
            if (indicateErrors) {
              injector.messages.indicateError()
            }
          }
        } catch (e: ExException) {
          if (e is FinishException) {
            break
          }
          finalResult = ExecutionResult.Error
          if (indicateErrors) {
            injector.messages.showErrorMessage(editor, e.message)
          } else {
            logger.warn("Failed while executing $unit. " + e.message)
          }
        } catch (e: NotImplementedError) {
          if (indicateErrors) {
            injector.messages.showErrorMessage(editor, "Not implemented yet :(")
          }
        } catch (e: Exception) {
          logger.warn(e.toString())
          if (injector.application.isUnitTest()) {
            throw e
          }
        }
      }

      if (!skipHistory && myScript.units.size == 1 && myScript.units[0] is Command && myScript.units[0] !is RepeatCommand) {
        injector.registerGroup.storeTextSpecial(LAST_COMMAND_REGISTER, script)
      }
      return finalResult
    } finally {
      injector.vimscriptExecutor.executingVimscript = false

      // Initialize any extensions that were enabled during execution of this vimscript
      // See the doc of this function for details
      enableDelayedExtensions()
    }
  }

  protected abstract fun enableDelayedExtensions()

  override fun executeFile(file: File, editor: VimEditor, fileIsIdeaVimRcConfig: Boolean, indicateErrors: Boolean) {
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    try {
      if (fileIsIdeaVimRcConfig) {
        injector.vimscriptExecutor.executingIdeaVimRcConfiguration = true
      }
      ensureFileIsSaved(file)
      execute(file.readText(), editor, context, skipHistory = true, indicateErrors)
    } catch (e: IOException) {
      if (indicateErrors) {
        injector.messages.showErrorMessage(editor, "Cannot read file \"${file.path}\": ${e.message}")
      } else {
        logger.warn("Failed to read file ${file.path}: ${e.message}")
      }
    } finally {
      if (fileIsIdeaVimRcConfig) {
        injector.vimrcFileState.saveFileState(file.absolutePath)
        injector.vimscriptExecutor.executingIdeaVimRcConfiguration = false
      }
    }
  }

  protected abstract fun ensureFileIsSaved(file: File)

  @Throws(ExException::class)
  override fun executeLastCommand(editor: VimEditor, context: ExecutionContext): Boolean {
    val reg = injector.registerGroup.getRegister(editor, context, ':') ?: return false
    val text = reg.text ?: return false
    execute(text, editor, context, skipHistory = false, indicateErrors = true, CommandLineVimLContext)
    return true
  }
}
