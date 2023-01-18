/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.editorMode
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.services.IjOptionConstants
import kotlin.test.fail

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

inline fun waitAndAssert(timeInMillis: Int = 1000, condition: () -> Boolean) {
  val end = System.currentTimeMillis() + timeInMillis
  while (end > System.currentTimeMillis()) {
    Thread.sleep(10)
    IdeEventQueue.getInstance().flushQueue()
    if (condition()) return
  }
  fail()
}

fun waitAndAssertMode(
  fixture: CodeInsightTestFixture,
  mode: VimStateMachine.Mode,
  timeInMillis: Int = (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, IjOptionConstants.visualdelay) as VimInt).value + 1000,
) {
  waitAndAssert(timeInMillis) { fixture.editor.editorMode == mode }
}

fun assertDoesntChange(timeInMillis: Int = 1000, condition: () -> Boolean) {
  val end = System.currentTimeMillis() + timeInMillis
  while (end > System.currentTimeMillis()) {
    if (!condition()) {
      fail()
    }

    Thread.sleep(10)
    IdeEventQueue.getInstance().flushQueue()
  }
}

fun assertHappened(timeInMillis: Int = 1000, precision: Int, condition: () -> Boolean) {
  assertDoesntChange(timeInMillis - precision) { !condition() }

  waitAndAssert(precision * 2) { condition() }
}

@Suppress("unused")
fun waitCondition(
  durationMillis: Long,
  interval: Long = 500,
  condition: () -> Boolean,
): Boolean {
  val endTime = System.currentTimeMillis() + durationMillis
  while (System.currentTimeMillis() < endTime) {
    if (condition())
      return true
    else {
      Thread.sleep(interval)
    }
  }
  return false
}
