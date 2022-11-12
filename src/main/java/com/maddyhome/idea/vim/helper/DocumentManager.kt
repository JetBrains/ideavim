/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.group.VimMarkServiceImpl
import com.maddyhome.idea.vim.group.SearchGroup

object DocumentManager {
  private val docListeners = mutableSetOf<DocumentListener>()
  private val LISTENER_MARKER = Key<String>("VimlistenerMarker")

  init {
    docListeners += VimMarkServiceImpl.MarkUpdater
    docListeners += SearchGroup.DocumentSearchListener.INSTANCE
  }

  fun addListeners(doc: Document) {
    val marker = doc.getUserData(LISTENER_MARKER)
    if (marker != null) return

    doc.putUserData(LISTENER_MARKER, "foo")
    for (docListener in docListeners) {
      EventFacade.getInstance().addDocumentListener(doc, docListener)
    }
  }

  fun removeListeners(doc: Document) {
    doc.getUserData(LISTENER_MARKER) ?: return

    doc.putUserData(LISTENER_MARKER, null)
    for (docListener in docListeners) {
      EventFacade.getInstance().removeDocumentListener(doc, docListener)
    }
  }
}
