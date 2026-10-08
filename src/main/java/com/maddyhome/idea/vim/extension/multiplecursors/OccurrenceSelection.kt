/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * Selects the occurrence of [text] at [offset], with the caret at its end
 */
internal fun Caret.selectOccurrence(offset: Int, text: String): TextRange =
  selectRange(TextRange(offset, offset + text.length))

/**
 * Selects [range], with the caret at its end
 */
internal fun Caret.selectRange(range: TextRange): TextRange {
  vim.vimSetSelection(range.startOffset, range.endOffset - 1, true)
  injector.scroll.scrollCaretIntoView(editor.vim)
  return selectedRange
}

/**
 * Enters Visual mode and selects the word under the caret. Returns `null` if the caret is not on a word.
 */
internal fun Caret.selectWordUnderCaret(): TextRange? {
  val word = injector.searchHelper.findWordAtOrFollowingCursor(editor.vim, vim, isBigWord = false) ?: return null
  if (word.startOffset > offset) return null

  enterCharacterwiseVisualMode(editor.vim)
  vim.vimSetSelection(word.startOffset, word.endOffsetInclusive, true)
  return selectedRange
}

internal fun Caret.wordUnderCaret(): String? {
  val word = injector.searchHelper.findWordAtOrFollowingCursor(editor.vim, vim, isBigWord = false) ?: return null
  if (word.startOffset > offset) return null
  return editor.vim.getText(word)
}

/**
 * IntelliJ can't change the primary caret, so [caret] is added again as the primary one, with its selection
 */
internal fun Editor.makePrimary(caret: Caret): Caret {
  val anchor = caret.vim.vimSelectionStart
  val head = caret.offset
  val hasSelection = caret.hasSelection()

  caretModel.removeCaret(caret)
  val primary = caretModel.addCaret(offsetToVisualPosition(head), true) ?: return caretModel.primaryCaret
  if (hasSelection) {
    primary.vim.vimSetSelection(anchor, head, true)
  }
  updateCaretsVisualAttributes()
  injector.scroll.scrollCaretIntoView(vim)
  return primary
}

internal val Caret.selectedRange: TextRange
  get() = TextRange(selectionStart, selectionEnd)

internal fun Editor.isSelectedByAnyCaret(offset: Int): Boolean =
  caretModel.allCarets.any { it.selectionStart == offset }

internal fun enterCharacterwiseVisualMode(editor: VimEditor) {
  VimPlugin.getVisualMotion().enterVisualMode(editor, SelectionType.CHARACTER_WISE)
  // The key handler has to know about the mode for the next keys
  KeyHandler.getInstance().reset(editor)
}

internal fun leaveVisualMode(editor: VimEditor) {
  editor.exitVisualMode()
  KeyHandler.getInstance().reset(editor)
}

internal fun showNoMoreMatches() {
  VimPlugin.showMessage(MessageHelper.message("multiple-cursors.message.no.more.matches"))
}
