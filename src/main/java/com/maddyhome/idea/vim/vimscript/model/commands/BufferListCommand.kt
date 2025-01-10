/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.vimLine
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import org.jetbrains.annotations.NonNls

/**
 * Handles buffers, files, ls command. Supports +, =, a, %, # filters.
 *
 * @author John Weigel
 */
@ExCommand(command = "ls,files,buffers")
internal data class BufferListCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier) {

  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  companion object {
    const val FILE_NAME_PAD = 30
    val SUPPORTED_FILTERS = setOf('+', '=', 'a', '%', '#')
  }

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val arg = argument.trim()
    val filter = pruneUnsupportedFilters(arg)
    val bufferList = getBufferList(context, filter)

    val outputPanel = injector.outputPanel.getOrCreate(editor, context)
    outputPanel.addText(bufferList.joinToString(separator = "\n"))
    outputPanel.show()

    return ExecutionResult.Success
  }

  private fun pruneUnsupportedFilters(filter: String) = filter.filter { it in SUPPORTED_FILTERS }

  private fun getBufferList(context: ExecutionContext, filter: String): List<String> {
    val bufferList = mutableListOf<String>()
    val project = PlatformDataKeys.PROJECT.getData(context.ij) ?: return emptyList()

    val fem = FileEditorManager.getInstance(project)
    val openFiles = fem.openFiles
    val bufNumPad = openFiles.size.toString().length
    val currentFile = fem.selectedFiles[0]
    val previousFile = VimPlugin.getFile().getPreviousTab(context.ij)
    val virtualFileDisplayMap = buildVirtualFileDisplayMap(project)

    var index = 1
    for ((file, displayFileName) in virtualFileDisplayMap) {
      val editor = EditorHelper.getEditor(file) ?: continue

      val bufStatus = getBufferStatus(editor, file, currentFile, previousFile)

      if (bufStatusMatchesFilter(filter, bufStatus)) {
        val lineNum = editor.ij.vimLine
        val lineNumPad =
          if (displayFileName.length < FILE_NAME_PAD) (FILE_NAME_PAD - displayFileName.length).toString() else ""

        bufferList.add(
          String.format(
            "   %${bufNumPad}s %s %s%${lineNumPad}s line: %d",
            index,
            bufStatus,
            displayFileName,
            "",
            lineNum,
          ),
        )
      }
      index++
    }

    return bufferList
  }

  private fun buildVirtualFileDisplayMap(project: Project): Map<VirtualFile, String> {
    val openFiles = FileEditorManager.getInstance(project).openFiles
    val basePath = if (project.basePath != null) project.basePath + "/" else ""
    val filePaths = mutableMapOf<VirtualFile, String>()

    for (file in openFiles) {
      val filePath = file.path

      // If the file is under the project path, then remove the project base path from the file.
      val displayFilePath = if (basePath.isNotEmpty() && filePath.startsWith(basePath)) {
        filePath.replace(basePath, "")
      } else {
        // File is not under the project base path so add the full path.
        filePath
      }

      filePaths[file] = '"' + displayFilePath + '"'
    }

    return filePaths
  }

  private fun bufStatusMatchesFilter(filter: String, bufStatus: String) = filter.all { it in bufStatus }
}

private fun getBufferStatus(
  editor: VimEditor,
  file: VirtualFile,
  currentFile: VirtualFile,
  previousFile: VirtualFile?,
): String {
  @NonNls val bufStatus = StringBuilder()

  when (file) {
    currentFile -> bufStatus.append("%a  ")
    previousFile -> bufStatus.append("#   ")
    else -> bufStatus.append("    ")
  }

  if (!file.isWritable) {
    bufStatus.setCharAt(2, '=')
  }

  if (isDocumentDirty(editor.ij.document)) {
    bufStatus.setCharAt(3, '+')
  }

  return bufStatus.toString()
}

private fun isDocumentDirty(document: Document): Boolean {
  var line = 0

  while (line < document.lineCount) {
    if (document.isLineModified(line)) {
      return true
    }
    line++
  }

  return false
}
