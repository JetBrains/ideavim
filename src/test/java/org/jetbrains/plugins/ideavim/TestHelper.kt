/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.testFramework.EditorTestUtil
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.key.MappingOwner

/**
 * @author Alex Plate
 */

infix fun String.rangeOf(str: String): TextRange {
  val clearString = this.replace(EditorTestUtil.CARET_TAG, "")
  val indexOf = clearString.indexOf(str)
  if (indexOf == -1) throw RuntimeException("$str was not found in $clearString")

  return TextRange(indexOf, indexOf + str.length)
}

fun Editor.rangeOf(first: String, nLinesDown: Int): TextRange {
  val starts = ArrayList<Int>()
  val ends = ArrayList<Int>()

  val indexOf = document.text.replace(EditorTestUtil.CARET_TAG, "").indexOf(first)
  if (indexOf == -1) throw RuntimeException("$first was not found in $this")

  val position = offsetToLogicalPosition(indexOf)
  if (position.line + nLinesDown > document.lineCount) throw RuntimeException("To much lines")

  starts += indexOf
  ends += indexOf + first.length

  for (i in 1..nLinesDown) {
    val nextOffset = logicalPositionToOffset(LogicalPosition(position.line + i, position.column))
    starts += nextOffset
    ends += nextOffset + first.length
  }
  return TextRange(starts.toIntArray(), ends.toIntArray())
}

@Suppress("unused")
fun waitCondition(
  durationMillis: Long,
  interval: Long = 500,
  condition: () -> Boolean,
): Boolean {
  val endTime = System.currentTimeMillis() + durationMillis
  while (System.currentTimeMillis() < endTime) {
    if (condition()) {
      return true
    } else {
      Thread.sleep(interval)
    }
  }
  return false
}

internal class ExceptionHandler : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    error(exceptionMessage)
  }

  companion object {
    internal const val exceptionMessage = "Exception here"
  }
}

internal val exceptionMappingOwner = MappingOwner.Plugin.get("Exception mapping owner")
