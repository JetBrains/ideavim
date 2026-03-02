/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMarkServiceBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.mark.VimMark
import com.maddyhome.idea.vim.newapi.globalIjOptions
import kotlinx.coroutines.runBlocking

/**
 * Thin-client mark service for split (Remote Development) mode.
 *
 * Extends [VimMarkServiceBase] for all mark state management (local marks,
 * global marks, mark updates on text changes). The only operation that needs
 * the backend is IDE bookmark creation (`ideamarks` option), because
 * [com.intellij.ide.bookmark.BookmarksManager] has no bookmark groups on
 * the thin client.
 */
internal class VimMarkServiceSplitClient : VimMarkServiceBase() {

  override fun createGlobalMark(editor: VimEditor, char: Char, offset: Int): Mark? {
    if (!injector.globalIjOptions().ideamarks) {
      return super.createGlobalMark(editor, char, offset)
    }
    val lp = editor.offsetToBufferPosition(offset)
    val virtualFile = editor.getVirtualFile() ?: return super.createGlobalMark(editor, char, offset)
    val projectId = editor.projectId
    val info = rpc { createOrGetSystemMark(char, lp.line, virtualFile.path, projectId) }
      ?: return super.createGlobalMark(editor, char, offset)
    // Use the backend's protocol (from BookmarkInfo) so cross-file navigation via
    // selectEditor can find the file on the correct backend filesystem (file, jar, etc.).
    return VimMark(info.key, info.line, lp.column, virtualFile.path, info.protocol)
  }

  override fun getGlobalMark(char: Char): Mark? {
    if (!char.isGlobalMark()) return null

    if (!injector.globalIjOptions().ideamarks) {
      return super.getGlobalMark(char)
    }

    // When ideamarks is on, the backend's BookmarksManager is the source of truth.
    // Query the backend for the current bookmark state so that IDE-UI-created
    // bookmarks (which don't fire BookmarksListener on the thin client) are visible.
    val info = rpc { getBookmarkForMark(char) }
    if (info != null) {
      val mark = VimMark(info.key, info.line, info.col, info.filepath, info.protocol)
      globalMarks[char] = mark
      return mark
    }

    // No IDE bookmark exists for this mark — clear stale local state if any.
    globalMarks.remove(char)
    return null
  }

  override fun getAllGlobalMarks(): Set<Mark> {
    if (!injector.globalIjOptions().ideamarks) {
      return super.getAllGlobalMarks()
    }

    val bookmarks = rpc { getAllBookmarks() }
    globalMarks.clear()
    for (info in bookmarks) {
      globalMarks[info.key] = VimMark(info.key, info.line, info.col, info.filepath, info.protocol)
    }
    return globalMarks.values.toSet()
  }

  override fun removeGlobalMark(char: Char) {
    if (injector.globalIjOptions().ideamarks) {
      rpc { removeBookmark(char) }
    }
    super.removeGlobalMark(char)
  }

  private fun <T> rpc(block: suspend BookmarkRemoteApi.() -> T): T {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) { BookmarkRemoteApi.getInstance().block() }
  }
}
