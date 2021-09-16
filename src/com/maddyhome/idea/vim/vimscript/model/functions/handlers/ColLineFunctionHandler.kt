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
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.vimLine
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

// TODO: 03.08.2021 Support second parameter
object LineFunctionHandler : FunctionHandler() {

  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 2

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: Editor,
    context: DataContext,
    parent: Executable,
  ): VimInt {
    if (editor == null) return VimInt.ZERO
    val argument = argumentValues[0].evaluate(editor, context, Script(listOf()))

    return variableToPosition(editor, argument, true)?.first ?: VimInt.ZERO
  }
}

object ColFunctionHandler : FunctionHandler() {

  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 1

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: Editor,
    context: DataContext,
    parent: Executable,
  ): VimDataType {
    val argument = argumentValues[0].evaluate(editor, context, Script(listOf()))
    if (editor == null) return VimInt.ZERO

    return variableToPosition(editor, argument, false)?.second ?: VimInt.ZERO
  }
}

private fun currentCol(editor: Editor): VimInt {
  val logicalPosition = editor.caretModel.currentCaret.logicalPosition
  var lineLength = EditorHelper.getLineLength(editor, logicalPosition.line)

  // If virtualedit is set, the col is one more
  // XXX Should we also check the current mode?
  if (OptionsManager.virtualedit.value.isNotEmpty()) {
    lineLength += 1
  }

  return (logicalPosition.column + 1).coerceAtMost(lineLength).asVimInt()
}

// Analog of var2fpos function
// Translate variable to position
private fun variableToPosition(editor: Editor, variable: VimDataType, dollarForLine: Boolean): Pair<VimInt, VimInt>? {
  if (variable is VimList) {
    if (variable.values.size < 2) return null

    val line = indexAsNumber(variable, 0) ?: return null
    if (line <= 0 || line > editor.document.lineCount) {
      return null
    }

    var column = indexAsNumber(variable, 1) ?: return null
    val lineLength = EditorHelper.getLineLength(editor, line.value - 1)

    if (variable[1].asString() == "$") {
      column = (lineLength + 1).asVimInt()
    }

    if (column.value == 0 || column > lineLength + 1) {
      return null
    }

    return line to column
  }

  val name = variable.asString()
  if (name.isEmpty()) return null

  // Current caret line
  if (name[0] == '.') return editor.vimLine.asVimInt() to currentCol(editor)

  // Visual start
  if (name == "v") {
    if (editor.inVisualMode) {
      return editor.vimLine.asVimInt() to currentCol(editor)
    }

    val vimStart = editor.caretModel.currentCaret.vimSelectionStart
    val visualLine = (editor.offsetToLogicalPosition(vimStart).line + 1).asVimInt()
    val visualCol = (editor.offsetToLogicalPosition(vimStart).column + 1).asVimInt()

    return visualLine to visualCol
  }

  // Mark
  if (name.length >= 2 && name[0] == '\'') {
    val mark = VimPlugin.getMark().getMark(editor, name[1]) ?: return null
    val markLogicalLine = (mark.logicalLine + 1).asVimInt()
    val markLogicalCol = (mark.col + 1).asVimInt()
    return markLogicalLine to markLogicalCol
  }

  // First visual line
  if (name.length >= 2 && name[0] == 'w' && name[1] == '0') {
    if (!dollarForLine) return null
    val actualVisualTop = EditorHelper.getVisualLineAtTopOfScreen(editor)
    val actualLogicalTop = EditorHelper.visualLineToLogicalLine(editor, actualVisualTop)
    return (actualLogicalTop + 1).asVimInt() to currentCol(editor)
  }

  // Last visual line
  if (name.length >= 2 && name[0] == 'w' && name[1] == '$') {
    if (!dollarForLine) return null
    val actualVisualBottom = EditorHelper.getVisualLineAtBottomOfScreen(editor)
    val actualLogicalBottom = EditorHelper.visualLineToLogicalLine(editor, actualVisualBottom)
    return (actualLogicalBottom + 1).asVimInt() to currentCol(editor)
  }

  // Last column or line
  if (name[0] == '$') {
    return if (dollarForLine) {
      editor.document.lineCount.asVimInt() to VimInt.ZERO
    } else {
      val line = editor.caretModel.currentCaret.logicalPosition.line
      val lineLength = EditorHelper.getLineLength(editor, line)
      (line + 1).asVimInt() to lineLength.asVimInt()
    }
  }

  return null
}

// Analog of tv_list_find_nr
// Get value as number by index
private fun indexAsNumber(list: VimList, index: Int): VimInt? {
  val value = list.values.getOrNull(index) ?: return null
  return value.toVimNumber()
}
