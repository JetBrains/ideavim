/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.change

import com.intellij.openapi.command.impl.FinishMarkAction
import com.intellij.openapi.command.impl.StartMarkAction
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.openapi.editor.impl.findEditorOrNull
import com.maddyhome.idea.vim.group.onEdt

/**
 * RPC handler for [ChangeRemoteApi].
 *
 * Registers StartMarkAction/FinishMarkAction on the backend's UndoManager
 * to group multiple document changes into a single undo step.
 */
internal class ChangeRemoteApiImpl : ChangeRemoteApi {
  private var currentStartMark: StartMarkAction? = null

  override suspend fun startUndoMark(editorId: EditorId, commandName: String) = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt
    val project = editor.project ?: return@onEdt
    currentStartMark = try {
      StartMarkAction.start(editor, project, commandName)
    } catch (_: StartMarkAction.AlreadyStartedException) {
      null
    }
  }

  override suspend fun finishUndoMark(editorId: EditorId) = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt
    val project = editor.project ?: return@onEdt
    val mark = currentStartMark
    currentStartMark = null
    if (mark != null) {
      FinishMarkAction.finish(project, editor, mark)
    }
  }
}
