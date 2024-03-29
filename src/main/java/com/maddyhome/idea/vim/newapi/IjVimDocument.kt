/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.maddyhome.idea.vim.api.VimDocument
import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.common.LiveRange

internal class IjVimDocument(val document: Document) : VimDocument {

  private val changeListenersMap: MutableMap<ChangesListener, DocumentListener> = mutableMapOf()

  override fun addChangeListener(listener: ChangesListener) {
    val nativeListener = object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        listener.documentChanged(
          ChangesListener.Change(
            event.oldFragment.toString(),
            event.newFragment.toString(),
            event.offset,
          ),
        )
      }
    }
    changeListenersMap[listener] = nativeListener
    document.addDocumentListener(nativeListener)
  }

  override fun removeChangeListener(listener: ChangesListener) {
    val nativeListener = changeListenersMap.remove(listener) ?: error("Existing listener expected")
    document.removeDocumentListener(nativeListener)
  }

  override fun getOffsetGuard(offset: Int): LiveRange? {
    return document.getOffsetGuard(offset)?.vim
  }
}
