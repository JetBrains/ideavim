/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Undo behaviour of *keystroke* replay (`@a`) on the JBC backend, as a counterpart to
 * [RepeatUndoSplitTest], which covers *stroke* replay (`.`).
 *
 * The two replay mechanisms have opposite requirements around the platform's command boundaries, and
 * split mode is where that shows: JBC 2025.3+ speculative undo turns off the platform's own
 * command-boundary grouping, so every atomic edit becomes its own undo entry unless something marks
 * a group (see `VimUndoGroup`).
 *
 *  - `.` keeps the outer command (`executesNestedCommands` is false) and adds an explicit undo mark,
 *    so one `u` reverts the whole repeat. That is asserted by
 *    [RepeatUndoSplitTest.change word repeat with dot then undo].
 *  - `@` drops the outer command (`PlaybackRegisterAction.executesNestedCommands = true`) so that a
 *    `u` replayed *inside* the macro can undo an earlier change made by the same macro.
 *
 * These tests pin down whether keystroke replay also satisfies vim's "one change, one `u`" rule on
 * JBC. The answer decides whether `.` could ever be reimplemented on top of keystroke replay without
 * regressing VIM-3918 / VIM-4245.
 */
class MacroUndoSplitTest : IdeaVimStarterTestBase() {

  /**
   * The decisive one. Same single logical change as
   * [RepeatUndoSplitTest.change word repeat with dot then undo], replayed with `@a` instead of `.`.
   * In vim one change is one undo step, so one `u` must take the whole replayed `cw` back - not just
   * the last keystroke of it.
   */
  @Test
  fun `one undo reverts a single-change macro replay`() {
    openFile(createFile("src/MacroUndo1.txt", "Lorem ipsum dolor\nLorem ipsum dolor\n"))

    // Record a macro that performs exactly one change
    typeVimAndEscape("qa0cwSectetur")
    typeVim("q")
    pause()
    assertEquals(1, countSectetur()) { "Recording should have changed one word. Actual: ${editorText()}" }

    typeVim("j")
    pause()
    typeVim("@a")
    val replayed = waitUntil { countSectetur() >= 2 }
    assertTrue(replayed) { "Macro replay should have changed the second line too. Actual: ${editorText()}" }

    // One `u` must revert the whole replayed change, leaving the original one intact
    typeVim("u")
    pause(1000)
    assertEquals(1, countSectetur()) {
      "A single `u` should revert the whole macro replay, leaving exactly one change. Actual: ${editorText()}"
    }
  }

  /**
   * A macro whose body contains `u` must be able to undo a change made earlier in the same macro.
   * Before `executesNestedCommands`, holding an outer command here threw `UnexpectedUndoException`
   * and disposed the editor, so the tail of this test checks the editor is still usable.
   */
  @Test
  fun `undo replayed inside a macro does not break the editor`() {
    openFile(createFile("src/MacroUndo2.txt", "Lorem ipsum dolor sit amet\n"))

    // `dw` deletes a word, the `u` inside the macro puts it back: the macro is a no-op overall
    typeVim("qa0dwuq")
    pause(1000)
    assertEditorContains("Lorem ipsum", "Recording dw+u should leave the line unchanged")

    typeVim("@a")
    pause(1000)
    assertEditorContains("Lorem ipsum", "Replaying dw+u should leave the line unchanged")

    // If the replayed `u` had disposed the editor, no further vim key would land
    typeVim("x")
    assertEditorContains("orem ipsum", "Editor should still accept vim keys after the macro")
  }

  private fun countSectetur(): Int = editorText().split("Sectetur").size - 1
}
