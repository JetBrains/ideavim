/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.testFramework.fixtures.EditorMouseFixture
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntilSelectionUpdated
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for selecting with the mouse, as governed by the 'mouse' option.
 *
 * When the mouse is enabled for the current mode, dragging creates a selection and IdeaVim switches to Visual mode.
 *
 * When it is not enabled, Vim makes a `modeless-selection` (`:help modeless-selection`): the text is still selected so
 * that it can be copied, but the mode is not changed, the caret does not move, and Vim's operators cannot act on the
 * selection, which goes away as soon as a command is typed. This is why people set `mouse=` in the first place - so
 * that selecting with the mouse keeps working the "old" way.
 *
 * The gestures are simulated with the platform [EditorMouseFixture], which dispatches real Swing mouse events to the
 * editor component. Note that [EditorMouseFixture.dragTo] requires a non-empty visible area.
 *
 * Only plain left-button gestures are covered here. [EditorMouseFixture] adds a modifier to the release event of every
 * other button (ALT for the middle button, META for the right one, plus a popup trigger for the right one), so the
 * platform reacts with alt-click / cmd-click / context-menu behaviour whose side effects leak into unrelated tests
 * sharing this JVM. The button and area filtering therefore has to be verified by hand - see
 * `ModelessSelection.appliesTo`.
 */
@TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
class MouseDragSelectionTest : VimTestCase() {
  private companion object {
    const val TEXT = "Lorem ipsum dolor sit amet,\nconsectetur adipiscing elit"

    /**
     * 'mouse' does not change *what* a drag selects, only what Vim does about it - a modeless selection covers the same
     * text as the Visual selection the same gesture would make.
     *
     * Note that the drag has to send more than one event to get this. On the very first drag event the caret is still a
     * block caret, and `EditorImpl.getTargetPosition` then subtracts a column when the mouse is in the first half of a
     * cell, so a single-event drag to column 5 selects only four characters. A real drag always sends many events, by
     * which point IdeaVim has forced a bar caret and the adjustment no longer applies.
     */
    const val SELECTED = "Lorem"
  }

  @Test
  fun `mouse=a -- drag selects text and enters Visual mode`() {
    configureByText(TEXT)
    enterCommand("set mouse=a")

    dragOverFirstWord()

    assertSelection(SELECTED)
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `mouse= (empty) -- drag still selects text, but the mode does not change`() {
    configureByText(TEXT)
    enterCommand("set mouse=")

    dragOverFirstWord()

    assertSelection(SELECTED)
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `mouse= (empty) -- drag does not move the caret`() {
    configureByText(TEXT)
    enterCommand("set mouse=")
    typeText("w") // Move off offset 0, so that "the caret did not move" is meaningful

    val before = caretOffset()
    dragOverFirstWord()

    assertSelection(SELECTED)
    assertEquals(before, caretOffset(), "A modeless selection must not move the caret")
  }

  @Test
  fun `mouse= (empty) -- a Vim command removes the modeless selection`() {
    configureByText(TEXT)
    enterCommand("set mouse=")

    dragOverFirstWord()
    assertSelection(SELECTED)

    // Vim drops a modeless selection as soon as a command is typed. It must not survive as an operator target
    typeText("l")

    assertSelection(null)
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `mouse= (empty) -- click keeps a Visual selection made with the keyboard`() {
    configureByText(TEXT)
    enterCommand("set mouse=")
    typeText("ve")

    ApplicationManager.getApplication().invokeAndWait {
      EditorMouseFixture(fixture.editor as EditorImpl).clickAt(1, 3)
    }
    waitUntilSelectionUpdated(fixture.editor)

    // Vim ignores the click entirely, so neither the selection nor the mode may change
    assertSelection("Lorem")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `mouse=n -- drag in Visual mode does not replace the Visual selection`() {
    configureByText(TEXT)
    enterCommand("set mouse=n") // No 'v', so the mouse is disabled in Visual mode
    typeText("ve")

    // Drag over a different part of the file. Vim would show a modeless selection on top of the Visual area; IdeaVim
    // has one selection per caret, and the Visual area has to win - operators act on the native selection
    ApplicationManager.getApplication().invokeAndWait {
      EditorMouseFixture(fixture.editor as EditorImpl).pressAt(1, 0).dragTo(1, 8).release()
    }
    waitUntilSelectionUpdated(fixture.editor)

    assertSelection("Lorem")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `mouse= (empty) -- a special key removes the modeless selection`() {
    configureByText(TEXT)
    enterCommand("set mouse=")
    typeText("w")

    dragOverFirstWord()

    // Special keys take a different route into the key handler than typed characters do (VimShortcutKeyAction rather
    // than VimTypedActionHandler), and the selection has to be dropped on both
    typeText("<Left>")

    assertSelection(null)
  }

  @Test
  fun `mouse= (empty) -- an operator does not act on the modeless selection`() {
    configureByText(TEXT)
    enterCommand("set mouse=")
    typeText("w") // Caret on "ipsum", away from the dragged range

    dragOverFirstWord()
    typeText("yiw")

    // Vim's operators cannot see a modeless selection, so this yanks the word under the caret
    assertRegister('"', "ipsum")
  }

  @Test
  fun `mouse= (empty) -- typing in Insert mode does not replace the selected text`() {
    configureByText(TEXT)
    enterCommand("set mouse=n") // Mouse disabled in Insert mode too, since 'i' is absent
    typeText("i")

    dragOverFirstWord()
    typeText("X")

    // The platform's typed action replaces a native selection, so the modeless selection has to be gone by then
    assertState("X${TEXT}")
  }

  @Test
  fun `mouse=nvi -- drag with the command line open keeps the Visual selection`() {
    configureByText(TEXT)
    typeText("ve") // Visual, "Lorem"
    typeText(":") // Command-line mode. The default 'mouse' has no 'c', so the mouse is disabled here

    // IdeaVim keeps the Visual selection on screen while the command line is open, so a drag must not replace it
    dragOverSecondLine()

    assertSelection("Lorem")

    // Leave the command line closed. VimTestCase.tearDown deactivates the panel but does not reset the Vim mode, so an
    // open command line here would make the next test in this JVM run its `:` command outside Normal mode.
    // <C-C> is what VimExTestCase.deactivateExEntry uses
    typeText("<C-C>")
  }

  @Test
  fun `mouse= (empty) -- dragging back to the anchor collapses the selection`() {
    configureByText(TEXT)
    enterCommand("set mouse=")

    ApplicationManager.getApplication().invokeAndWait {
      EditorMouseFixture(fixture.editor as EditorImpl)
        .pressAt(0, 0)
        .dragTo(0, 3)
        .dragTo(0, 5)
        .dragTo(0, 0)
        .release()
    }
    waitUntilSelectionUpdated(fixture.editor)

    assertSelection(null)
  }

  @Test
  fun `mouse= (empty) -- a second drag replaces the first modeless selection`() {
    configureByText(TEXT)
    enterCommand("set mouse=")

    dragOverFirstWord()
    dragOverSecondLine()

    assertSelection("consec")
  }

  private fun dragOverSecondLine() {
    ApplicationManager.getApplication().invokeAndWait {
      EditorMouseFixture(fixture.editor as EditorImpl)
        .pressAt(1, 0)
        .dragTo(1, 3)
        .dragTo(1, 6)
        .release()
    }
    waitUntilSelectionUpdated(fixture.editor)
  }

  private fun dragOverFirstWord() {
    ApplicationManager.getApplication().invokeAndWait {
      EditorMouseFixture(fixture.editor as EditorImpl)
        .pressAt(0, 0)
        .dragTo(0, 3)
        .dragTo(0, 5)
        .release()
    }
    waitUntilSelectionUpdated(fixture.editor)
  }

  private fun caretOffset(): Int {
    var offset = 0
    ApplicationManager.getApplication().invokeAndWait {
      offset = fixture.editor.caretModel.primaryCaret.offset
    }
    return offset
  }
}
