/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.helper

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.newapi.IjVimEditorSnapshot
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals

/**
 * The snapshot is searched in the background instead of the editor, so it must answer the regex engine's questions
 * exactly like the editor does
 */
@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class IjVimEditorSnapshotTest : VimTestCase() {

  private val text = """
    |first line
    |	indented with a tab
    |  two	tabs	here
    |
    |${c}last line without a newline""".trimMargin()

  @Test
  fun `lines and offsets match the editor`() = readAction {
    val editor = fixture.editor.vim
    val snapshot = IjVimEditorSnapshot.capture(fixture.editor)

    assertEquals(editor.lineCount(), snapshot.lineCount())
    for (line in 0 until editor.lineCount()) {
      assertEquals(editor.getLineStartOffset(line), snapshot.getLineStartOffset(line), "Start of line $line")
      assertEquals(editor.getLineEndOffset(line), snapshot.getLineEndOffset(line), "End of line $line")
    }
    for (offset in 0..editor.text().length) {
      val expected = editor.offsetToBufferPosition(offset)
      val actual = snapshot.offsetToBufferPosition(offset)
      assertEquals(expected.line to expected.column, actual.line to actual.column, "Position of offset $offset")
      assertEquals(editor.bufferPositionToOffset(expected), snapshot.bufferPositionToOffset(actual), "Offset $offset")
    }
  }

  @Test
  fun `snapshot keeps the text and carets it was captured with`() {
    configureByText(text)
    val (snapshot, caretOffset) = ApplicationManager.getApplication().runReadAction<Pair<IjVimEditorSnapshot, Int>> {
      IjVimEditorSnapshot.capture(fixture.editor) to fixture.editor.caretModel.offset
    }

    typeText("ggdd")

    assertEquals(text.replace(c, ""), snapshot.text().toString())
    assertEquals(caretOffset, snapshot.carets().single().offset)
  }

  @Test
  fun `search in snapshot finds the same matches as in the editor`() = readAction {
    val editor = fixture.editor.vim
    val snapshot = IjVimEditorSnapshot.capture(fixture.editor)

    for (pattern in listOf("line", "\\<t\\w*", "\\ttabs", "^\\s*$", "\\%#last", "\\%2l\\S\\+", "\\%5v\\w")) {
      assertEquals(
        injector.searchHelper.findAll(editor, pattern, 0, -1, false),
        injector.searchHelper.findAll(snapshot, pattern, 0, -1, false),
        "Matches of $pattern",
      )
    }
  }

  @Test
  fun `search stops when cancelled`() = readAction {
    val snapshot = IjVimEditorSnapshot.capture(fixture.editor)

    assertThrows<CancellationException> {
      injector.searchHelper.findAll(snapshot, "line", 0, -1, false, Int.MAX_VALUE) { true }
    }
  }

  @Test
  fun `searches on different threads do not interfere`() {
    configureByText((0 until 2000).joinToString("\n") { "foo$it barbar ${it % 7} foobar" })
    val snapshot = ApplicationManager.getApplication().runReadAction<IjVimEditorSnapshot> {
      IjVimEditorSnapshot.capture(fixture.editor)
    }
    val pattern = "\\(foo\\|bar\\)\\+ \\d"
    val expected = injector.searchHelper.findAll(snapshot, pattern, 0, -1, false)

    val executor = Executors.newFixedThreadPool(4)
    try {
      val results = (1..8).map {
        executor.submit<List<TextRange>> { injector.searchHelper.findAll(snapshot, pattern, 0, -1, false) }
      }
      results.forEach { assertEquals(expected, it.get()) }
    } finally {
      executor.shutdownNow()
    }
  }

  @Test
  fun `buffer position past the end of the line is clamped to the line end`() = readAction {
    val snapshot = IjVimEditorSnapshot.capture(fixture.editor)

    assertEquals(snapshot.getLineEndOffset(0), snapshot.bufferPositionToOffset(BufferPosition(0, 100)))
  }

  private fun readAction(action: () -> Unit) {
    configureByText(text)
    ApplicationManager.getApplication().runReadAction(action)
  }
}
