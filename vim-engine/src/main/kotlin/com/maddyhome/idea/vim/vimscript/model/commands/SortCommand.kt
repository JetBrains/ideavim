/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import java.util.*

/**
 * @author Alex Selesse
 * see "h :sort"
 */
data class SortCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  @Throws(ExException::class)
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val arg = argument
    val nonEmptyArg = arg.trim().isNotEmpty()

    val reverse = nonEmptyArg && "!" in arg
    val ignoreCase = nonEmptyArg && "i" in arg
    val number = nonEmptyArg && "n" in arg

    val lineComparator = LineComparator(ignoreCase, number, reverse)
    if (editor.inBlockSubMode) {
      val primaryCaret = editor.primaryCaret()
      val range = getSortLineRange(editor, primaryCaret)
      val worked = injector.changeGroup.sortRange(editor, range, lineComparator)
      primaryCaret.moveToInlayAwareOffset(
        injector.motion.moveCaretToLineStartSkipLeading(editor, range.startLine)
      )
      return if (worked) ExecutionResult.Success else ExecutionResult.Error
    }

    var worked = true
    for (caret in editor.nativeCarets()) {
      val range = getSortLineRange(editor, caret)
      if (!injector.changeGroup.sortRange(editor, range, lineComparator)) {
        worked = false
      }
      caret.moveToInlayAwareOffset(injector.motion.moveCaretToLineStartSkipLeading(editor, range.startLine))
    }

    return if (worked) ExecutionResult.Success else ExecutionResult.Error
  }

  private fun getSortLineRange(editor: VimEditor, caret: VimCaret): LineRange {
    val range = getLineRange(editor, caret)

    // Something like "30,20sort" gets converted to "20,30sort"
    val normalizedRange = if (range.endLine < range.startLine) LineRange(range.endLine, range.startLine) else range

    // If we don't have a range, we either have "sort", a selection, or a block
    if (normalizedRange.endLine - normalizedRange.startLine == 0) {
      // If we have a selection.
      val selectionModel = editor.getSelectionModel()
      return if (selectionModel.hasSelection()) {
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd

        val startLine = editor.offsetToLogicalPosition(start).line
        val endLine = editor.offsetToLogicalPosition(end).line

        LineRange(startLine, endLine)
      } else {
        LineRange(0, editor.lineCount() - 1)
      } // If we have a generic selection, i.e. "sort" entire document
    }

    return normalizedRange
  }

  private class LineComparator(
    private val myIgnoreCase: Boolean,
    private val myNumber: Boolean,
    private val myReverse: Boolean,
  ) : Comparator<String> {

    override fun compare(o1: String, o2: String): Int {
      var o1ToCompare = o1
      var o2ToCompare = o2
      if (myReverse) {
        val tmp = o2ToCompare
        o2ToCompare = o1ToCompare
        o1ToCompare = tmp
      }
      if (myIgnoreCase) {
        o1ToCompare = o1ToCompare.uppercase(Locale.getDefault())
        o2ToCompare = o2ToCompare.uppercase(Locale.getDefault())
      }
      return if (myNumber) {
        // About natural sort order - http://www.codinghorror.com/blog/2007/12/sorting-for-humans-natural-sort-order.html
        val n1 = injector.searchGroup.findDecimalNumber(o1ToCompare)
        val n2 = injector.searchGroup.findDecimalNumber(o2ToCompare)
        if (n1 == null) {
          if (n2 == null) 0 else -1
        } else {
          if (n2 == null) 1 else n1.compareTo(n2)
        }
      } else o1ToCompare.compareTo(o2ToCompare)
    }
  }
}
