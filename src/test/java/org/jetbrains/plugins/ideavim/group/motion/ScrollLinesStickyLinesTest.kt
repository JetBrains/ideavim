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
import com.intellij.openapi.editor.ex.EditorEx
import com.maddyhome.idea.vim.helper.EditorWindowLayout
import com.maddyhome.idea.vim.helper.stickyLinesHeightAt
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.awt.Rectangle
import kotlin.test.assertTrue

@TestWithoutNeovim(SkipNeovimReason.OPTION)
class ScrollLinesStickyLinesTest : StickyLinesTestCase() {
  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2])
  fun `test CTRL-E scrolls down on every press with sticky lines`(headerLines: Int) {
    withNestedBlocks(headerLines) { editor ->
      val failures = mutableListOf<String>()
      repeat(150) { press ->
        val before = state(editor)
        typeText(editor, "<C-E>")
        val after = state(editor)
        if (after.offset <= before.offset || after.topLine <= before.topLine) {
          failures += "press ${press + 1}: $before -> $after"
        }
      }
      assertTrue(failures.isEmpty(), "<C-E> did not scroll down ${failures.size} times:\n" + failures.take(10).joinToString("\n"))
    }
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2])
  fun `test CTRL-Y scrolls up on every press with sticky lines`(headerLines: Int) {
    withNestedBlocks(headerLines) { editor ->
      typeText(editor, "150<C-E>")
      val failures = mutableListOf<String>()
      while (state(editor).offset > 0) {
        val before = state(editor)
        typeText(editor, "<C-Y>")
        val after = state(editor)
        if (after.offset >= before.offset || after.topLine >= before.topLine) {
          failures += "$before -> $after"
          break
        }
      }
      assertTrue(failures.isEmpty(), "<C-Y> did not scroll up:\n" + failures.joinToString("\n"))
    }
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2])
  fun `test zt scrolls to the closest offset that shows the line below the sticky lines panel`(headerLines: Int) {
    withNestedBlocks(headerLines) { editor ->
      val failures = mutableListOf<String>()
      for (line in 0 until editor.document.lineCount) {
        typeText(editor, "${line + 1}Gzt")
        val expected = closestOffsetShowing(editor, line)
        val actual = state(editor).offset
        if (actual != expected) failures += "line $line: expected offset $expected, was $actual"
      }
      assertTrue(failures.isEmpty(), "zt scrolled to the wrong offset:\n" + failures.take(10).joinToString("\n"))
    }
  }

  private fun closestOffsetShowing(editor: Editor, line: Int): Int {
    var offset = 0
    ApplicationManager.getApplication().invokeAndWait {
      val area = editor.scrollingModel.visibleArea
      val maxOffset = (editor as EditorEx).contentComponent.preferredSize.height - area.height
      val top = editor.visualLineToY(line)
      // Without a panel, a row cut at the top of the window isn't shown
      offset = (minOf(top, maxOffset) downTo 0).first {
        val panel = editor.stickyLinesHeightAt(Rectangle(area.x, it, area.width, area.height))
        it + panel <= top && (panel > 0 || (top - it) % editor.lineHeight == 0)
      }
    }
    return offset
  }

  private fun withNestedBlocks(headerLines: Int, action: (Editor) -> Unit) {
    val lines = MutableList(headerLines) { "<!-- header $it -->" }
    lines += "<root>"
    repeat(10) { s ->
      lines += "  <section id=\"$s\">"
      repeat(3) { g ->
        lines += "    <group id=\"$g\">"
        repeat(4) { i -> lines += "      <item id=\"$i\">value $s.$g.$i</item>" }
        lines += "    </group>"
      }
      lines += "  </section>"
    }
    lines += "</root>"
    configureByText(lines.joinToString("\n"))

    withMainEditor(20) { editor ->
      addStickyLines(editor) { add ->
        val open = ArrayDeque<Int>()
        lines.forEachIndexed { index, line ->
          val trimmed = line.trim()
          when {
            trimmed.startsWith("</") -> add(open.removeLast(), index)
            !trimmed.contains("</") && !trimmed.startsWith("<!--") -> open.addLast(index)
          }
        }
      }
      typeText(editor, "gg")
      action(editor)
    }
  }

  private data class State(val topLine: Int, val offset: Int)

  private fun state(editor: Editor): State {
    var state: State? = null
    ApplicationManager.getApplication().invokeAndWait {
      val layout = EditorWindowLayout(editor)
      state = State(layout.view.topLine, layout.offset)
    }
    return state!!
  }
}
