/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.removeUserData
import com.maddyhome.idea.vim.api.coerceOffset
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.hasVisualSelection
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.event.InputEvent
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.min

/**
 * Implements Vim's `modeless-selection` for mouse gestures that Vim itself does not handle.
 *
 * When `'mouse'` does not include the current mode, Vim does not use the mouse for cursor positioning or for Visual
 * mode. It still lets the mouse select text, so that the selection can be copied - Vim calls this a modeless selection
 * (`:help modeless-selection`). Such a selection does not change the mode, does not move the caret, and Vim's operators
 * cannot act on it. It goes away as soon as a command is typed or another selection is started.
 *
 * We get the "caret does not move" part by consuming the mouse press, which is the only way the platform offers - the
 * caret is placed inline in `EditorImpl.processMousePressed`, with nothing between it and the single `isConsumed` check
 * in `runMousePressedCommand`. A consumed press also makes the platform ignore the rest of the gesture
 * (`runMouseDraggedCommand` bails out on it), so we make the selection ourselves in [extendSelection].
 *
 * We consume as little as possible: only a plain left-button press over text. The context menu, middle-click paste,
 * gutter clicks, folding arrows, ctrl+click navigation and alt+click carets are IDE features that `'mouse'` has no
 * business disabling, so those gestures are left to the platform untouched. See [appliesTo].
 *
 * That narrowing has a deliberate consequence: "the caret does not move" only holds for the plain left button. The
 * platform's caret placement depends on the mouse event's area, not on its button, so a right, middle, ctrl+ or shift+
 * click still moves the caret even when `'mouse'` is empty, and a shift+click additionally creates a native selection
 * that IdeaVim turns into Visual mode. We accept this: those gestures are IDE navigation rather than Vim's use of the
 * mouse, and disabling them (as consuming every press used to) breaks the context menu, breakpoints and go-to-declaration
 * for no benefit Vim asks for.
 */
internal object ModelessSelection {
  /**
   * The offset a gesture started at, i.e. the anchor of the modeless selection being made, if any.
   *
   * Also acts as the "we consumed this press" flag. Stored per editor rather than globally: a gesture belongs to one
   * editor, and several editors (splits, diff panes) can be in play.
   */
  private val anchorKey = Key.create<Int>("IdeaVim.mouse.modelessSelectionAnchor")

  /**
   * The range of a modeless selection that we made.
   *
   * We only ever remove a selection that still matches this range, so we cannot destroy a selection that something else
   * has taken ownership of in the meantime.
   *
   * A [RangeMarker] rather than plain offsets, because the selection is itself document-tracking: an edit that doesn't
   * come from a Vim key (a menu-invoked Reformat Code, say) shifts the selection, and raw offsets would then stop
   * matching, leaving the selection on screen with nothing able to remove it.
   */
  private val ownedRangeKey = Key.create<RangeMarker>("IdeaVim.mouse.modelessSelectionRange")

  /**
   * Is the mouse enabled for the current mode, as described by the `'mouse'` option?
   *
   * Note that IdeaVim's mode is global rather than per-editor ([com.maddyhome.idea.vim.api.VimEditorBase.mode]
   * delegates to the state machine), so [editor] does not narrow the mode - it is only used for the option scope.
   */
  fun isMouseEnabled(editor: Editor): Boolean {
    val mouseOption = injector.globalOptions().mouse
    if (mouseOption.contains("a")) return true
    return when (editor.vim.mode) {
      is Mode.INSERT, is Mode.REPLACE -> mouseOption.contains("i")
      // Vim has no flag for operator-pending; `:help 'mouse'` treats it as Normal
      is Mode.NORMAL, is Mode.OP_PENDING -> mouseOption.contains("n")
      is Mode.VISUAL, is Mode.SELECT -> mouseOption.contains("v")
      is Mode.CMD_LINE -> mouseOption.contains("c")
      // Vim's 'r' flag (hit-enter and more-prompt) is not modelled
      else -> true
    }
  }

  /**
   * Is this a gesture that `'mouse'` governs, i.e. a plain left-button gesture over text?
   */
  fun appliesTo(event: EditorMouseEvent): Boolean {
    if (event.area != EditorMouseEventArea.EDITING_AREA) return false
    if (!SwingUtilities.isLeftMouseButton(event.mouseEvent)) return false
    val modifiers = InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK or
      InputEvent.ALT_DOWN_MASK or InputEvent.META_DOWN_MASK
    return event.mouseEvent.modifiersEx and modifiers == 0
  }

  /**
   * Start a gesture that Vim does not handle. Any modeless selection left from the previous gesture goes away, as it
   * does in Vim.
   */
  fun beginGesture(editor: Editor, offset: Int) {
    clearIfOwned(editor)
    editor.putUserData(anchorKey, offset)
  }

  /**
   * Finish the gesture. The modeless selection stays on screen until a command is typed or another gesture starts.
   */
  fun endGesture(editor: Editor) {
    editor.removeUserData(anchorKey)
  }

  /**
   * Is a gesture that Vim does not handle in progress in this editor, i.e. did we consume its press?
   */
  fun isGestureInProgress(editor: Editor): Boolean = editor.getUserData(anchorKey) != null

  /**
   * Extend the modeless selection to [offset], the position the mouse has been dragged to.
   *
   * Does nothing when the current mode owns a selection. Vim draws a modeless selection on top of the Visual area and
   * keeps both; IdeaVim has one selection per caret, and the Visual area has to win, because operators and Insert mode
   * act on the native selection - letting the drag win would make `d` delete the dragged text instead.
   */
  fun extendSelection(editor: Editor, offset: Int) {
    val anchor = editor.getUserData(anchorKey) ?: return
    // Note that Command-line mode keeps the Visual selection on screen, so its `returnTo` has to be checked as well -
    // `hasVisualSelection` alone is false for CMD_LINE. See IdeaSelectionControl, which makes the same distinction
    if (editor.vim.mode.hasVisualSelection || editor.vim.mode.returnTo.hasVisualSelection) return

    val vimEditor = IjVimEditor(editor)
    val start = vimEditor.coerceOffset(min(anchor, offset))
    val end = vimEditor.coerceOffset(max(anchor, offset))
    if (start == end) {
      // Dragged back onto the anchor. Every other drag-select in the IDE collapses here, rather than leaving the last
      // non-empty range on screen
      clearIfOwned(editor)
      return
    }

    // The suppressor stops IdeaSelectionControl from turning this into a Visual selection
    SelectionVimListenerSuppressor.lock {
      editor.caretModel.primaryCaret.setSelection(start, end)
    }
    setOwnedRange(editor, start, end)
  }

  private fun setOwnedRange(editor: Editor, start: Int, end: Int) {
    disposeOwnedRange(editor)
    editor.putUserData(ownedRangeKey, editor.document.createRangeMarker(start, end))
  }

  private fun disposeOwnedRange(editor: Editor) {
    editor.getUserData(ownedRangeKey)?.dispose()
    editor.removeUserData(ownedRangeKey)
  }

  /**
   * Drop a modeless selection that we made. Called when a Vim command is about to run, matching Vim, where any command
   * removes the modeless selection.
   *
   * Does nothing unless the selection still is exactly the one we made, so a Visual selection, an IDE selection or a
   * template selection is never touched.
   */
  fun clearIfOwned(editor: Editor) {
    val owned = editor.getUserData(ownedRangeKey) ?: return
    val caret = editor.caretModel.primaryCaret
    val isOurs = owned.isValid &&
      caret.hasSelection() &&
      caret.selectionStart == owned.startOffset &&
      caret.selectionEnd == owned.endOffset

    if (isOurs) {
      SelectionVimListenerSuppressor.lock {
        caret.removeSelection()
      }
    }
    // Only drop ownership once we know the selection is no longer ours to remove, so that a selection we made can never
    // be orphaned on screen with nothing able to clear it
    disposeOwnedRange(editor)
  }
}
