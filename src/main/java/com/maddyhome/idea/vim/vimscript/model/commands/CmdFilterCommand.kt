/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.toTextRange
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :!"
 */
@ExCommand(command = "!")
internal data class CmdFilterCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier) {

  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    logger.debug("execute")
    val command = buildString {
      var inBackslash = false
      argument.forEach { c ->
        when {
          !inBackslash && c == '!' -> {
            val last = lastCommand
            if (last.isNullOrEmpty()) {
              VimPlugin.showMessage(EngineMessageHelper.message("e_noprev"))
              return ExecutionResult.Error
            }
            append(last)
          }

          !inBackslash && c == '%' -> {
            val virtualFile = EditorHelper.getVirtualFile(editor.ij)
            if (virtualFile == null) {
              // Note that we use a slightly different error message to Vim, because we don't support alternate files or file
              // name modifiers. (I also don't know what the :p:h means)
              // (Vim) E499: Empty file name for '%' or '#', only works with ":p:h"
              // (IdeaVim) E499: Empty file name for '%'
              VimPlugin.showMessage(EngineMessageHelper.message("E499"))
              return ExecutionResult.Error
            }
            append(virtualFile.path)
          }

          else -> append(c)
        }

        inBackslash = c == '\\'
      }
    }

    if (command.isEmpty()) {
      return ExecutionResult.Error
    }

    val workingDirectory = editor.ij.project?.basePath
    return try {
      if (range.size() == 0) {
        // Show command output in a window
        VimPlugin.getProcess().executeCommand(editor, command, null, workingDirectory)?.let {
          val outputPanel = injector.outputPanel.getOrCreate(editor, context)
          outputPanel.addText(it)
          outputPanel.show()
        }
        lastCommand = command
        ExecutionResult.Success
      } else {
        // Filter
        val range = getLineRange(editor).toTextRange(editor)
        val input = editor.ij.document.charsSequence.subSequence(range.startOffset, range.endOffset)
        VimPlugin.getProcess().executeCommand(editor, command, input, workingDirectory)?.let {
          ApplicationManager.getApplication().runWriteAction {
            val start = editor.offsetToBufferPosition(range.startOffset)
            val end = editor.offsetToBufferPosition(range.endOffset)
            editor.ij.document.replaceString(range.startOffset, range.endOffset, it)
            val linesFiltered = end.line - start.line
            if (linesFiltered > 2) {
              VimPlugin.showMessage("$linesFiltered lines filtered")
            }
          }
        }
        lastCommand = command
        ExecutionResult.Success
      }
    } catch (_: ProcessCanceledException) {
      throw ExException("Command terminated")
    } catch (e: Exception) {
      throw ExException(e.message)
    }
  }

  companion object {
    private val logger = Logger.getInstance(CmdFilterCommand::class.java.name)
    private var lastCommand: String? = null
  }
}
