/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.option.OptionsManager
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

fun waitAndAssertMode(fixture: CodeInsightTestFixture, mode: CommandState.Mode, timeInMillis: Int = OptionsManager.visualEnterDelay.value() + 1000) {
  waitAndAssert(timeInMillis) { fixture.editor.mode == mode }
}

fun assertDoesntChange(timeInMillis: Int = 1000, condition: () -> Boolean) {
  val end = System.currentTimeMillis() + timeInMillis
  while (end > System.currentTimeMillis()) {
    if (!condition()) fail()

    Thread.sleep(10)
    IdeEventQueue.getInstance().flushQueue()
  }
}
