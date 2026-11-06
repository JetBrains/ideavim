/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.motion

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertTrue

/**
 * Moving the caret within a line doesn't scroll, wherever the sticky lines panel is (VIM-4372)
 */
@TestWithoutNeovim(SkipNeovimReason.OPTION)
class ScrolloffStickyLinesShakeTest : StickyLinesTestCase() {
  @ParameterizedTest
  @ValueSource(ints = [3, 5, 10, 15, 18, 19, 20, 21, 22, 23, 24, 25, 999])
  fun `test moving left and right does not scroll with sticky lines`(scrollOffset: Int) {
    withStickyLines(scrollOffset) { editor -> checkSideways(editor, scrollOffset) }
  }

  // Nested C# scopes in a window of 41 rows, where the panel pushes its last line out as a scope ends (VIM-4372)
  @ParameterizedTest
  @ValueSource(ints = [10, 15, 18, 19, 20, 21, 22, 999])
  fun `test moving left and right does not scroll with sticky lines in nested blocks`(scrollOffset: Int) {
    withNestedBlocks(scrollOffset) { editor -> checkSideways(editor, scrollOffset) }
  }

  private fun checkSideways(editor: Editor, scrollOffset: Int) {
      val failures = mutableListOf<String>()
      for (motion in listOf("j", "k")) {
        repeat(150) {
          typeText(editor, motion)
          for (sideways in listOf("l", "h", "l", "h")) {
            val before = state(editor)
            typeText(editor, sideways)
            val after = state(editor)
            if (before.offset != after.offset) failures += "'$sideways' after '$motion': $before -> $after"
          }
        }
      }
      assertTrue(failures.isEmpty(), "so=$scrollOffset scrolled ${failures.size} times:\n" + failures.take(10).joinToString("\n"))
  }

  private fun withNestedBlocks(scrollOffset: Int, action: (Editor) -> Unit) {
    val lines = mutableListOf("namespace ConsoleApp", "{")
    repeat(6) { c ->
      lines += listOf("    public class Worker$c", "    {")
      repeat(4) { m ->
        lines += listOf(
          "        public int Run$m(int value)", "        {",
          "            var total = 0;",
          "            for (var i = 0; i < value; i++)", "            {",
          "                if (i % 2 == 0)", "                {",
          "                    while (total < 100)", "                    {",
        )
        lines += List(12) { k -> "                        total += i * $k + value; // some line of code $k" }
        lines += listOf("                    }", "                }", "            }", "            return total;", "        }", "")
      }
      lines += listOf("    }", "")
    }
    lines += "}"
    configureByText(lines.joinToString("\n"))
    enterCommand("set scrolloff=$scrollOffset")

    withMainEditor(41) { editor ->
      // The header of a C# scope is the line above its `{`
      addStickyLines(editor) { add ->
        val open = ArrayDeque<Int>()
        lines.forEachIndexed { index, line ->
          when (line.trim()) {
            "{" -> open.addLast(index - 1)
            "}" -> add(open.removeLast(), index)
          }
        }
      }
      typeText(editor, "gg")
      action(editor)
    }
  }

  private data class State(val line: Int, val offset: Int, val stickyHeight: Int)

  private fun state(editor: Editor): State {
    var state: State? = null
    ApplicationManager.getApplication().invokeAndWait {
      state = State(
        editor.caretModel.logicalPosition.line,
        editor.scrollingModel.verticalScrollOffset,
        (editor as EditorImpl).stickyLinesPanelHeight,
      )
    }
    return state!!
  }

  private fun withStickyLines(scrollOffset: Int, action: (Editor) -> Unit) {
    configureByLines(1000, "    I found it in a legendary land")
    enterCommand("set scrolloff=$scrollOffset")

    withMainEditor(50) { editor ->
      addNestedScopes(editor)
      typeText(editor, "gg")
      action(editor)
    }
  }

  // Scopes nested up to 6 deep, so the panel grows and shrinks a line at a time, up to its limit
  private fun addNestedScopes(editor: Editor) {
    addStickyLines(editor) { add ->
      for (start in 0 until 900 step 60) {
        for (depth in 0 until 6) add(start + depth * 3, start + 55 - depth)
      }
    }
  }
}
