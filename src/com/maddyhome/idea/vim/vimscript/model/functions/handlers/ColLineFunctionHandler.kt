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

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.vimLine
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

// TODO: 03.08.2021 Support second parameter
object LineFunctionHandler : FunctionHandler() {

  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 2

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
  ): VimInt {
    if (editor == null) return VimInt.ZERO
    val argument = argumentValues[0].evaluate(editor, context, vimContext)

    if (argument is VimList) {
      val (line, _) = getLineAndCol(argument, editor) ?: return VimInt.ZERO
      return line
    }

    if (argument !is VimString) return VimInt.ZERO
    val stringValue = argument.value
    return getLine(stringValue, editor)
  }
}

object ColFunctionHandler : FunctionHandler() {

  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 1

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
  ): VimDataType {
    val argument = argumentValues[0].evaluate(editor, context, vimContext)
    if (editor == null) return VimInt.ZERO

    if (argument is VimList) {
      val (_, col) = getLineAndCol(argument, editor) ?: return VimInt.ZERO
      return col
    }

    if (argument !is VimString) return VimInt.ZERO
    val argumentValue = argument.value
    return getColumn(argumentValue, editor)
  }
}

private fun getLineAndCol(
  argument: VimList,
  editor: Editor,
): Pair<VimInt, VimInt>? {
  if (argument.values.size < 2) return null

  val firstArgument = argument.values[0]
  val line: VimInt
  if (firstArgument is VimString) {
    // Try to get line for '.', '$', etc.
    val calculatedLine = getLine(firstArgument.value, editor)
    if (calculatedLine == VimInt.ZERO) {
      // Try to get line as number
      val parsedInt = firstArgument.toVimNumber()
      if (parsedInt == VimInt.ZERO) {
        return null
      } else {
        line = parsedInt
      }
    } else {
      line = calculatedLine
    }
  } else {
    line = firstArgument.toVimNumber()
  }
  if (line.value - 1 < 0 || line.value - 1 >= editor.document.lineCount) return null
  val lineLength = lineLength(editor, line.value - 1)

  val columnArgument = argument.values[1]

  val col: VimInt = if (columnArgument is VimString) {
    val calculatedColumn = getColumn(columnArgument.value, editor)
    if (calculatedColumn == VimInt.ZERO) {
      val parsedInt = columnArgument.toVimNumber()
      if (parsedInt == VimInt.ZERO) {
        return null
      } else {
        parsedInt
      }
    } else {
      calculatedColumn
    }
  } else {
    columnArgument.toVimNumber()
  }

  if (col.value - 1 < 0 || col.value - 1 >= lineLength) return null
  return line to col
}

private fun lineLength(editor: Editor, line: Int): Int {
  return editor.document.getLineEndOffset(line) - editor.document.getLineStartOffset(line)
}

private fun getColumn(argumentValue: String, editor: Editor): VimInt {
  return when {
    argumentValue.isEmpty() -> VimInt.ZERO

    // Current line
    argumentValue == "." -> currentCol(editor)

    // Last column of current line
    argumentValue == "$" -> lastCol(editor)

    // Mark line
    argumentValue.length == 2 && argumentValue[0] == '\'' -> {
      val markLogicalLine = VimPlugin.getMark().getMark(editor, argumentValue[1])?.col ?: return VimInt.ZERO
      (markLogicalLine + 1).asVimInt()
    }

    // Start of the visual position
    argumentValue == "v" -> {
      if (editor.mode != CommandState.Mode.VISUAL) return currentCol(editor)
      val vimStart = editor.caretModel.currentCaret.vimSelectionStart
      (editor.offsetToLogicalPosition(vimStart).column + 1).asVimInt()
    }

    else -> VimInt.ZERO
  }
}

private fun currentCol(editor: Editor): VimInt {
  val logicalPosition = editor.caretModel.currentCaret.logicalPosition
  var lineLength = lineLength(editor, logicalPosition.line)

  // If virtualedit is set, the col is one more
  // XXX Should we also check the current mode?
  if (OptionsManager.virtualedit.value.isNotEmpty()) {
    lineLength += 1
  }

  return (logicalPosition.column + 1).coerceAtMost(lineLength).asVimInt()
}

private fun lastCol(editor: Editor): VimInt {
  val logicalPosition = editor.caretModel.currentCaret.logicalPosition
  val lineLength = lineLength(editor, logicalPosition.line)
  return lineLength.asVimInt()
}

fun getLine(stringValue: String, editor: Editor): VimInt {
  return when {
    stringValue.isEmpty() -> VimInt.ZERO

    // Current caret line
    stringValue == "." -> editor.vimLine.asVimInt()

    // Last line in document
    // Line count because vim counts lines 1-based
    stringValue == "$" -> editor.document.lineCount.asVimInt()

    // Mark line
    stringValue.length == 2 && stringValue[0] == '\'' -> {
      val markLogicalLine = VimPlugin.getMark().getMark(editor, stringValue[1])?.logicalLine ?: return VimInt.ZERO
      (markLogicalLine + 1).asVimInt()
    }

    // First visible line
    stringValue == "w0" -> {
      val actualVisualTop = EditorHelper.getVisualLineAtTopOfScreen(editor)
      val actualLogicalTop = EditorHelper.visualLineToLogicalLine(editor, actualVisualTop)
      (actualLogicalTop + 1).asVimInt()
    }

    // Last visible line
    stringValue == "w$" -> {
      val actualVisualBottom = EditorHelper.getVisualLineAtBottomOfScreen(editor)
      val actualLogicalBottom = EditorHelper.visualLineToLogicalLine(editor, actualVisualBottom)
      (actualLogicalBottom + 1).asVimInt()
    }

    // Start of the visual position
    stringValue == "v" -> {
      if (editor.mode != CommandState.Mode.VISUAL) return editor.vimLine.asVimInt()
      val vimStart = editor.caretModel.currentCaret.vimSelectionStart
      (editor.offsetToLogicalPosition(vimStart).line + 1).asVimInt()
    }

    else -> VimInt.ZERO
  }
}
