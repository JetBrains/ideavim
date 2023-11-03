/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimScriptExecutorBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.FinishException
import com.maddyhome.idea.vim.extension.VimExtensionRegistrar
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

@Service
internal class Executor : VimScriptExecutorBase() {
  private val logger = logger<Executor>()
  override var executingVimscript = false
  override var executingIdeaVimRcConfiguration = false

  @Throws(ExException::class)
  override fun execute(script: String, editor: VimEditor, context: ExecutionContext, skipHistory: Boolean, indicateErrors: Boolean, vimContext: VimLContext?): ExecutionResult {
    try {
      injector.vimscriptExecutor.executingVimscript = true
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
    } finally {
      injector.vimscriptExecutor.executingVimscript = false

      // Initialize any extensions that were enabled during execution of this vimscript
      // See the doc of this function for details
      VimExtensionRegistrar.enableDelayedExtensions()
    }
  }

  override fun executeFile(file: File, editor: VimEditor, fileIsIdeaVimRcConfig: Boolean, indicateErrors: Boolean) {
    val context = DataContext.EMPTY_CONTEXT.vim
    try {
      if (fileIsIdeaVimRcConfig) {
        injector.vimscriptExecutor.executingIdeaVimRcConfiguration = true
      }
      ensureFileIsSaved(file)
      execute(file.readText(), editor, context, skipHistory = true, indicateErrors)
    } catch (ignored: IOException) {
      LOG.error(ignored)
    } finally {
      if (fileIsIdeaVimRcConfig) {
        injector.vimrcFileState.saveFileState(file.absolutePath)
        injector.vimscriptExecutor.executingIdeaVimRcConfiguration = false
      }
    }
  }

  private fun ensureFileIsSaved(file: File) {
    val documentManager = FileDocumentManager.getInstance()

    VirtualFileManager.getInstance().findFileByNioPath(file.toPath())
      ?.let(documentManager::getCachedDocument)
      ?.takeIf(documentManager::isDocumentUnsaved)
      ?.let(documentManager::saveDocumentAsIs)
  }

  @Throws(ExException::class)
  override fun executeLastCommand(editor: VimEditor, context: ExecutionContext): Boolean {
    val reg = VimPlugin.getRegister().getRegister(':') ?: return false
    val text = reg.text ?: return false
    execute(text, editor, context, skipHistory = false, indicateErrors = true, CommandLineVimLContext)
    return true
  }

  companion object {
    val LOG = logger<Executor>()
  }
}
