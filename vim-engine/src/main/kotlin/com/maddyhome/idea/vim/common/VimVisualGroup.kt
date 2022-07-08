package com.maddyhome.idea.vim.group.visual

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimLogicalPosition
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

fun charToNativeSelection(editor: VimEditor, start: Int, end: Int, mode: VimStateMachine.Mode): Pair<Int, Int> {
  val (nativeStart, nativeEnd) = sort(start, end)
  val lineEnd = editor.lineEndForOffset(nativeEnd)
  val adj =
    if (isExclusiveSelection() || nativeEnd == lineEnd || mode == VimStateMachine.Mode.SELECT) 0 else 1
  val adjEnd = (nativeEnd + adj).coerceAtMost(editor.fileSize().toInt())
  return nativeStart to adjEnd
}

/**
 * Convert vim's selection start and end to corresponding native selection.
 *
 * Adds caret adjustment or extends to line start / end in case of linewise selection
 */
fun lineToNativeSelection(editor: VimEditor, start: Int, end: Int): Pair<Int, Int> {
  val (nativeStart, nativeEnd) = sort(start, end)
  val lineStart = editor.lineStartForOffset(nativeStart)
  // Extend to \n char of line to fill full line with selection
  val lineEnd = (editor.lineEndForOffset(nativeEnd) + 1).coerceAtMost(editor.fileSize().toInt())
  return lineStart to lineEnd
}

fun <T : Comparable<T>> sort(a: T, b: T) = if (a > b) b to a else a to b

private fun isExclusiveSelection(): Boolean {
  return (
    injector.optionService.getOptionValue(
      OptionScope.GLOBAL,
      OptionConstants.selectionName
    ) as VimString
    ).value == "exclusive"
}

fun blockToNativeSelection(
  editor: VimEditor,
  start: Int,
  end: Int,
  mode: VimStateMachine.Mode,
): Pair<VimLogicalPosition, VimLogicalPosition> {
  var blockStart = editor.offsetToLogicalPosition(start)
  var blockEnd = editor.offsetToLogicalPosition(end)
  if (!isExclusiveSelection() && mode != VimStateMachine.Mode.SELECT) {
    if (blockStart.column > blockEnd.column) {
      if (blockStart.column < editor.lineLength(blockStart.line)) {
        blockStart = VimLogicalPosition(blockStart.line, blockStart.column + 1)
      }
    } else {
      if (blockEnd.column < editor.lineLength(blockEnd.line)) {
        blockEnd = VimLogicalPosition(blockEnd.line, blockEnd.column + 1)
      }
    }
  }
  return blockStart to blockEnd
}
