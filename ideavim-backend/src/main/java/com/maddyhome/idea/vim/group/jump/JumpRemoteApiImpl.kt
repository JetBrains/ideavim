/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.openapi.editor.impl.findEditorOrNull
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import com.maddyhome.idea.vim.group.diff.DiffJumps
import com.maddyhome.idea.vim.group.onEdt

/**
 * RPC handler for [JumpRemoteApi].
 * Delegates to [IdeDocumentHistory] and to the diff tabs on the backend, where they are available.
 *
 * Note: Must NOT use [com.maddyhome.idea.vim.api.injector] — it is not initialized on the backend
 * in split mode. Uses platform [ProjectId.findProjectOrNull] instead.
 */
internal class JumpRemoteApiImpl : JumpRemoteApi {
  override suspend fun includeCurrentCommandAsNavigation(projectId: ProjectId?) {
    if (projectId == null) return
    val project = projectId.findProjectOrNull() ?: return
    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
  }

  override suspend fun diffJumpAt(editorId: EditorId, line: Int, column: Int): DiffJump? = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt null
    DiffJumps.jumpAt(editor, LogicalPosition(line, column))?.let(DiffJump::of)
  }

  override suspend fun showDiffJump(projectId: ProjectId, jump: DiffJump): Boolean = onEdt {
    val project = projectId.findProjectOrNull() ?: return@onEdt false
    DiffJumps.show(project, jump.toJump())
  }
}
