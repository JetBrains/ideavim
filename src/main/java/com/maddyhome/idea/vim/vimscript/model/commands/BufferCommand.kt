/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * Handles buffer, buf, bu, b.
 *
 * @author John Weigel
 */
@ExCommand(command = "b[uffer]")
internal data class BufferCommand(val range: Range, val argument: String) : Command.SingleExecution(range) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val arg = argument.trim()
    val overrideModified = arg.startsWith('!')
    val buffer = if (overrideModified) arg.replace(Regex("^!\\s*"), "") else arg
    var result = true

    if (buffer.isNotEmpty()) {
      if (buffer.matches(Regex("^\\d+$"))) {
        val bufNum = buffer.toInt() - 1

        if (!VimPlugin.getFile().selectFile(bufNum, context)) {
          VimPlugin.showMessage(MessageHelper.message("buffer.0.does.not.exist", bufNum))
          result = false
        }
      } else if (buffer == "#") {
        VimPlugin.getFile().selectPreviousTab(context)
      } else {
        val editors = findPartialMatch(context, buffer)

        when (editors.size) {
          0 -> {
            VimPlugin.showMessage(MessageHelper.message("no.matching.buffer.for.0", buffer))
            result = false
          }
          1 -> {
            if (EditorHelper.hasUnsavedChanges(editor.ij) && !overrideModified) {
              VimPlugin.showMessage(MessageHelper.message("no.write.since.last.change.add.to.override"))
              result = false
            } else {
              VimPlugin.getFile().openFile(EditorHelper.getVirtualFile(editors[0].ij)!!.name, context)
            }
          }
          else -> {
            VimPlugin.showMessage(MessageHelper.message("more.than.one.match.for.0", buffer))
            result = false
          }
        }
      }
    }

    return if (result) ExecutionResult.Success else ExecutionResult.Error
  }

  private fun findPartialMatch(context: ExecutionContext, fileName: String): List<VimEditor> {
    val matchedFiles = mutableListOf<VimEditor>()
    val project = PlatformDataKeys.PROJECT.getData(context.ij) ?: return matchedFiles

    for (file in FileEditorManager.getInstance(project).openFiles) {
      if (file.name.contains(fileName)) {
        val editor = EditorHelper.getEditor(file) ?: continue
        matchedFiles.add(editor)
      }
    }

    return matchedFiles
  }
}
