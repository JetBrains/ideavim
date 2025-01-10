/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import java.util.*

/**
 * @author Alex Selesse
 * see "h :sort"
 */
// todo make it multicaret
@ExCommand(command = "sor[t]")
data class SortCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  @Throws(ExException::class)
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val sortOption = parseSortOption(argument)
    val lineComparator = LineComparator(sortOption.ignoreCase, sortOption.numeric, sortOption.reverse)
    var worked = true
    for (caret in editor.carets()) {
      val range = getSortLineRange(editor, caret)
      if (!injector.changeGroup.sortRange(editor, caret, range, lineComparator, sortOption)) {
        worked = false
      }
      caret.moveToInlayAwareOffset(injector.motion.moveCaretToLineStartSkipLeading(editor, range.startLine))
    }

    return if (worked) ExecutionResult.Success else ExecutionResult.Error
  }

  private fun getSortLineRange(editor: VimEditor, caret: VimCaret): LineRange {
    val range = getLineRange(editor, caret)

    // If we don't have a range, we either have "sort", a selection, or a block
    if (range.size == 1) {
      // If we have a selection.
      val selectionModel = editor.getSelectionModel()
      return if (selectionModel.hasSelection()) {
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd

        val startLine = editor.offsetToBufferPosition(start).line
        val endLine = editor.offsetToBufferPosition(end).line

        LineRange(startLine, endLine)
      } else {
        LineRange(0, editor.lineCount() - 1)
      } // If we have a generic selection, i.e. "sort" entire document
    }

    return range
  }

  private fun parseSortOption(arg: String): SortOption {
    val patternRange = extractPattern(arg)
    val pattern = patternRange?.let { arg.substring(it) }
    val flags = patternRange?.let { arg.removeRange(patternRange) } ?: arg
    return SortOption(
      reverse = modifier == CommandModifier.BANG,
      ignoreCase = "i" in flags,
      numeric = "n" in flags,
      unique = "u" in flags,
      sortOnPattern = "r" in flags,
      pattern = pattern
    )
  }

  private fun extractPattern(arg: String): IntRange? {
    val startIndex = arg.indexOf('/')
    val endIndex = arg.indexOf('/', startIndex + 2)
    if (startIndex >= 0 && endIndex >= 0) {
      return IntRange(startIndex + 1, endIndex - 1)
    }
    return null
  }

  private class LineComparator(
    private val ignoreCase: Boolean,
    private val numeric: Boolean,
    private val reverse: Boolean,
  ) : Comparator<String> {

    override fun compare(o1: String, o2: String): Int {
      var o1ToCompare = o1
      var o2ToCompare = o2
      if (reverse) {
        val tmp = o2ToCompare
        o2ToCompare = o1ToCompare
        o1ToCompare = tmp
      }
      if (ignoreCase) {
        o1ToCompare = o1ToCompare.uppercase(Locale.getDefault())
        o2ToCompare = o2ToCompare.uppercase(Locale.getDefault())
      }
      return if (numeric) {
        // About natural sort order - https://blog.codinghorror.com/sorting-for-humans-natural-sort-order/
        val n1 = injector.searchGroup.findDecimalNumber(o1ToCompare)
        val n2 = injector.searchGroup.findDecimalNumber(o2ToCompare)
        if (n1 == null) {
          if (n2 == null) {
            // no number, fallback to default
            o1ToCompare.compareTo(o2ToCompare)
          } else -1
        } else {
          if (n2 == null) 1 else n1.compareTo(n2) // what if tied?
        }
      } else {
        o1ToCompare.compareTo(o2ToCompare)
      }
    }
  }
}

data class SortOption(
  val ignoreCase: Boolean,
  val numeric: Boolean,
  val reverse: Boolean,
  val unique: Boolean,
  val sortOnPattern: Boolean,
  val pattern: String? = null,
)
