/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector

/**
 * Per-editor state that mirrors Vim's `arrow_used` flag (see `:help abbreviations` and `mapping.c:echeck_abbr`).
 *
 * The flag suppresses abbreviation expansion when the caret moved during the current insert session
 * by something other than an edit (arrow keys, mouse click, IDE jump, etc.). Unlike Vim — which clears
 * the flag on the next text edit via `stop_arrow()` — IdeaVim holds it until insert mode is freshly entered.
 *
 * The `lastSeenDocStamp` is the analog of "we know this caret move came from an edit": when a caret
 * event arrives, if the document's modification stamp changed since the last event, the move was
 * edit-driven and must not invalidate; if the stamp is unchanged, the caret was moved on its own.
 */
private val INVALIDATED: Key<Boolean> = Key("abbreviation.session.invalidated")
private val LAST_DOC_STAMP: Key<Long> = Key("abbreviation.session.lastDocStamp")

/** Call when insert mode is freshly entered. */
fun resetAbbreviationSession(editor: VimEditor, docStamp: Long) {
  injector.vimStorageService.putDataToWindow(editor, INVALIDATED, false)
  injector.vimStorageService.putDataToWindow(editor, LAST_DOC_STAMP, docStamp)
}

/** Call from the caret listener whenever the caret moves while in insert mode. */
fun noteCaretMoveInInsertSession(editor: VimEditor, currentDocStamp: Long) {
  val lastSeenDocStamp = injector.vimStorageService.getDataFromWindow(editor, LAST_DOC_STAMP)
  val caretMovedWithoutEdit = lastSeenDocStamp == currentDocStamp
  if (caretMovedWithoutEdit) {
    injector.vimStorageService.putDataToWindow(editor, INVALIDATED, true)
  }
  injector.vimStorageService.putDataToWindow(editor, LAST_DOC_STAMP, currentDocStamp)
}

/** True iff a pure cursor move has invalidated the current insert session's pending expansion. */
fun isAbbreviationSessionInvalidated(editor: VimEditor): Boolean =
  injector.vimStorageService.getDataFromWindow(editor, INVALIDATED) == true
