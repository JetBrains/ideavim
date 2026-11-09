/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.ide.impl.UndoRemoteBehaviorService
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.editorId
import com.intellij.openapi.util.Key
import com.intellij.util.PlatformUtils
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.group.change.ChangeRemoteApi
import com.maddyhome.idea.vim.newapi.ij

/**
 * True on a frontend without speculative undo (e.g. Rider): undo and redo run on the backend, and the frontend's
 * changes get there as document patches
 */
internal fun isUndoStackOnBackend(): Boolean =
  PlatformUtils.isJetBrainsClient() && !UndoRemoteBehaviorService.isSpeculativeUndoEnabled()

/**
 * Makes the changes of the previous IdeaVim command a separate undo step on the backend, see
 * [ChangeRemoteApi.closeUndoStep].
 *
 * Called before a command rather than after it: the changes of the previous command reach the backend as document
 * patches, and by the time of the next key press they are there.
 */
internal fun closeBackendUndoStep(editor: VimEditor) {
  if (!isUndoStackOnBackend()) return
  val ijEditor = editor.ij
  // Most commands, e.g. motions, change nothing, and need no round trip to the backend
  if (!ijEditor.document.changedSinceUndoStepClosed()) return
  ijEditor.document.rememberUndoStepClosed()
  val editorId = ijEditor.editorId()
  rpc(ijEditor.project) { ChangeRemoteApi.getInstance().closeUndoStep(editorId) }
}

private val UNDO_STEP_CLOSED_AT = Key.create<Long>("ideavim.undo.step.closed.at")

private fun Document.changedSinceUndoStepClosed(): Boolean = getUserData(UNDO_STEP_CLOSED_AT) != modificationStamp

private fun Document.rememberUndoStepClosed() = putUserData(UNDO_STEP_CLOSED_AT, modificationStamp)
