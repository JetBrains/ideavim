/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.cursorFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

// TODO: 03.08.2021 Support second parameter
@VimscriptFunction(name = "line")
internal class LineFunctionHandler : BuiltinFunctionHandler<VimInt>(minArity = 1, maxArity = 2) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    return variableToPosition(editor, arguments[0], true)?.first ?: VimInt.ZERO
  }
}

@VimscriptFunction(name = "col")
internal class ColFunctionHandler : UnaryFunctionHandler<VimInt>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    return variableToPosition(editor, arguments[0], false)?.second ?: VimInt.ZERO
  }
}

private fun currentCol(editor: VimEditor): VimInt {
  val logicalPosition = editor.currentCaret().getBufferPosition()
  var lineLength = editor.lineLength(logicalPosition.line)

  // If virtualedit is set, the col is one more
  // XXX Should we also check the current mode?
  if (injector.options(editor).virtualedit.isNotEmpty()) {
    lineLength += 1
  }

  return (logicalPosition.column + 1).coerceAtMost(lineLength).asVimInt()
}

// Analog of var2fpos function
// Translate variable to position
internal fun variableToPosition(
  editor: VimEditor,
  variable: VimDataType,
  dollarForLine: Boolean,
): Pair<VimInt, VimInt>? {
  if (variable is VimList) {
    if (variable.values.size < 2) return null

    val line = indexAsNumber(variable, 0) ?: return null
    if (line <= 0 || line > editor.lineCount()) {
      return null
    }

    var column = indexAsNumber(variable, 1) ?: return null
    val lineLength = editor.lineLength(line.value - 1)

    if (variable[1].toVimString().value == "$") {
      column = (lineLength + 1).asVimInt()
    }

    if (column.value == 0 || column > lineLength + 1) {
      return null
    }

    return line to column
  }

  val name = variable.toVimString().value
  if (name.isEmpty()) return null

  // Current caret line
  if (name[0] == '.') return editor.currentCaret().vimLine.asVimInt() to currentCol(editor)

  // The opposite end of the Visual selection, i.e., the start. If there is no selection, return the current line
  if (name == "v") {
    if (!editor.inVisualMode) {
      return editor.currentCaret().vimLine.asVimInt() to currentCol(editor)
    }

    val vimStart = editor.currentCaret().vimSelectionStart
    val bufferPosition = editor.offsetToBufferPosition(vimStart)
    val line = (bufferPosition.line + 1).asVimInt()
    val col = (bufferPosition.column + 1).asVimInt()

    return line to col
  }

  // Mark
  if (name.length >= 2 && name[0] == '\'') {
    // todo make it multicaret
    val mark = injector.markService.getMark(editor.primaryCaret(), name[1]) ?: return null
    val markLogicalLine = (mark.line + 1).asVimInt()
    val markLogicalCol = (mark.col + 1).asVimInt()
    return markLogicalLine to markLogicalCol
  }

  // First visual line
  if (name.length >= 2 && name[0] == 'w' && name[1] == '0') {
    if (!dollarForLine) return null
    val actualVisualTop = injector.engineEditorHelper.getVisualLineAtTopOfScreen(editor)
    val actualLogicalTop = editor.visualLineToBufferLine(actualVisualTop)
    return (actualLogicalTop + 1).asVimInt() to currentCol(editor)
  }

  // Last visual line
  if (name.length >= 2 && name[0] == 'w' && name[1] == '$') {
    if (!dollarForLine) return null
    val actualVisualBottom = injector.engineEditorHelper.getVisualLineAtBottomOfScreen(editor)
    val actualLogicalBottom = editor.visualLineToBufferLine(actualVisualBottom)
    return (actualLogicalBottom + 1).asVimInt() to currentCol(editor)
  }

  // Last column or line
  if (name[0] == '$') {
    return if (dollarForLine) {
      editor.lineCount().asVimInt() to VimInt.ZERO
    } else {
      val line = editor.currentCaret().getBufferPosition().line
      val lineLength = editor.lineLength(line)
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
