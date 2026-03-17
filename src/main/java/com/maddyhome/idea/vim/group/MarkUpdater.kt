/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.IjVimEditor

/**
 * Listens to editor document changes and updates marks accordingly.
 *
 * Extracted from `VimMarkServiceImpl` so it can be used in all product modes
 * (monolith, backend, and thin-client) without depending on the backend-only service.
 */
object MarkUpdater : DocumentListener {
  private val logger = Logger.getInstance(MarkUpdater::class.java.name)

  /**
   * This event indicates that a document is about to be changed. We use this event to update all the
   * editor's marks if text is about to be deleted.
   *
   * Note that the event is fired for both local changes and changes from remote guests in Code With Me scenarios (in
   * which case [com.intellij.codeWithMe.ClientId.current] will be the remote client). We don't care who caused it,
   * we just need to update the stored marks.
   *
   * @param event The change event
   */
  override fun beforeDocumentChange(event: DocumentEvent) {
    if (VimPlugin.isNotEnabled()) return
    logger.debug { "MarkUpdater before, event = $event" }
    if (event.oldLength == 0) return
    val doc = event.document
    val anEditor = getAnyEditorForDocument(doc) ?: return
    injector.markService.updateMarksFromDelete(anEditor, event.offset, event.oldLength, event.newLength)
  }

  /**
   * This event indicates that a document was just changed. We use this event to update all the editor's
   * marks if text was just added.
   *
   * Note that the event is fired for both local changes and changes from remote guests in Code With Me scenarios (in
   * which case [com.intellij.codeWithMe.ClientId.current] will be the remote client). We don't care who caused it,
   * we just need to update the stored marks.
   *
   * @param event The change event
   */
  override fun documentChanged(event: DocumentEvent) {
    if (VimPlugin.isNotEnabled()) return
    logger.debug { "MarkUpdater after, event = $event" }
    if (event.newLength == 0 || event.newLength == 1 && event.newFragment[0] != '\n') return
    val doc = event.document
    val anEditor = getAnyEditorForDocument(doc) ?: return
    injector.markService.updateMarksFromInsert(anEditor, event.offset, event.newLength)
  }

  /**
   * Get any editor for the given document
   *
   * We need an editor to help calculate offsets for marks, and it doesn't matter which one we use, because they would
   * all return the same results. However, we cannot use [com.maddyhome.idea.vim.api.VimEditorGroup.getEditors] because
   * the change might have come from a remote guest and there might not be an open local editor.
   */
  private fun getAnyEditorForDocument(doc: Document) =
    EditorFactory.getInstance().getEditors(doc).firstOrNull()?.let { IjVimEditor(it) }
}
