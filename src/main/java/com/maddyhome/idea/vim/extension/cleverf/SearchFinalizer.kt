/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.Disposer
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.state.mode.Mode

/**
 * Ends the searches of [session] when the cursor leaves the position the last search left it at, the text changes,
 * or Insert mode is entered. These are clever-f's `CursorMoved`, `TextChanged` and `InsertEnter` autocommands.
 */
internal class SearchFinalizer(private val session: CleverFSession) : CaretListener, DocumentListener,
  ModeChangeListener {
  private var disposable: Disposable? = null

  fun attach() {
    if (disposable != null) return
    val disposable = Disposer.newDisposable(VimPlugin.getInstance(), "clever-f listeners")
    EditorFactory.getInstance().eventMulticaster.addCaretListener(this, disposable)
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(this, disposable)
    injector.listenersNotifier.modeChangeListeners.add(this)
    this.disposable = disposable
  }

  fun detach() {
    injector.listenersNotifier.modeChangeListeners.remove(this)
    disposable?.let(Disposer::dispose)
    disposable = null
  }

  override fun caretPositionChanged(event: CaretEvent) {
    val caret = event.caret
    if (caret != event.editor.caretModel.primaryCaret) return
    val state = session.activeState ?: return
    if (state.isIn(event.editor) && !state.isAt(event.editor, caret.offset)) session.endSearches()
  }

  override fun documentChanged(event: DocumentEvent) {
    if (session.activeState?.isIn(event.document) == true) session.endSearches()
  }

  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    if (editor.mode is Mode.INSERT || editor.mode is Mode.REPLACE) session.endSearches()
  }
}
