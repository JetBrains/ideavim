/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.changelist

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.PlaceInfo
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.RecentPlacesListener
import com.intellij.openapi.project.Project
import com.intellij.platform.rpc.topics.broadcast

/**
 * Bridges IntelliJ's `RecentPlacesListener` into [CHANGE_LIST_REMOTE_TOPIC].
 *
 * `PlaceInfo.caretPosition` carries the *post-command* caret (one past `iX<Esc>`,
 * end of `rA`, etc.) but Vim's `g;` targets where the edit *began*, so we capture
 * `event.offset` from a `DocumentListener` and prefer it when available.
 *
 * Line/col are computed here on the backend (where the document lives) and sent
 * pre-resolved over the topic; the frontend then has no VirtualFile lookup to
 * race with editor loading in split mode.
 *
 * `recentPlaceRemoved` is intentionally NOT mirrored: IntelliJ's `putLastOrMerge`
 * fires "remove A, add B" across different lines (`canBeMergedWith(NAVIGATION)`),
 * which is far more aggressive than Vim's same-line/textwidth merge rule. The
 * frontend service does its own merging and capping, so platform eviction is moot.
 */
internal class ChangeListPlacesListener(private val project: Project) : RecentPlacesListener {

  private data class Pending(val document: Document, val offset: Int)

  private val pendingByPath = mutableMapOf<String, Pending>()

  init {
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(
      object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
          val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
          pendingByPath[file.path] = Pending(event.document, event.offset)
        }
      },
      project,
    )
  }

  @Suppress("OVERRIDE_DEPRECATION")
  override fun recentPlaceAdded(changePlace: PlaceInfo, isChanged: Boolean) {
    if (!isChanged) return
    val file = changePlace.file
    val path = file.path

    val pending = pendingByPath.remove(path)
    val (document, offset) = pending?.let { it.document to it.offset }
      ?: run {
        val doc = FileDocumentManager.getInstance().getDocument(file) ?: return
        val off = changePlace.caretPosition?.startOffset ?: return
        doc to off
      }

    val safeOffset = offset.coerceIn(0, document.textLength)
    val line = document.getLineNumber(safeOffset)
    val col = safeOffset - document.getLineStartOffset(line)

    CHANGE_LIST_REMOTE_TOPIC.broadcast(
      project,
      ChangeListInfo(
        line = line,
        col = col,
        filepath = path,
        protocol = file.fileSystem.protocol,
        timestamp = System.currentTimeMillis(),
      ),
    )
  }

  @Suppress("OVERRIDE_DEPRECATION")
  override fun recentPlaceRemoved(changePlace: PlaceInfo, isChanged: Boolean) {
    // Intentionally empty -- see class kdoc.
  }
}
