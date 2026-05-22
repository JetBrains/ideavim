/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.editor.impl.editorId
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.group.change.ChangeRemoteApi
import com.maddyhome.idea.vim.newapi.IjVimEditor

/**
 * Groups a sequence of document edits into a single undo step on the JBC backend.
 *
 * On JBC 2025.3+ speculative undo disables the platform's own command-boundary
 * grouping (`RdUndoCapabilities.isSupported = !isSpeculativeUndoEnabled`), so
 * without an explicit mark each atomic edit (insert, delete, caret move) lands
 * as its own undo entry — breaking vim's "one command = one `u`" semantics.
 *
 * Use [withVimUndoGroup] when the edits live in a single block;
 * use [startVimUndoGroup] / [finishVimUndoGroup] when start and finish must
 * span different methods (e.g. block-insert: start in `initBlockInsert`,
 * finish in `repeatInsert`).
 *
 * In non-split (monolith) mode the calls are no-ops because
 * [rpcSplitModeOnly] short-circuits.
 */
internal inline fun withVimUndoGroup(editor: VimEditor, name: String, block: () -> Unit) {
  startVimUndoGroup(editor, name)
  try {
    block()
  } finally {
    finishVimUndoGroup(editor)
  }
}

internal fun startVimUndoGroup(editor: VimEditor, name: String) {
  val ijEditor = (editor as IjVimEditor).editor
  val editorId = ijEditor.editorId()
  rpcSplitModeOnly(ijEditor.project) { ChangeRemoteApi.getInstance().startUndoMark(editorId, name) }
}

internal fun finishVimUndoGroup(editor: VimEditor) {
  val ijEditor = (editor as IjVimEditor).editor
  val editorId = ijEditor.editorId()
  rpcSplitModeOnly(ijEditor.project) { ChangeRemoteApi.getInstance().finishUndoMark(editorId) }
}
