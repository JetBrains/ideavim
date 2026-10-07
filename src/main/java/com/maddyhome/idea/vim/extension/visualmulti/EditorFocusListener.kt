/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.editor.impl.EditorComponentImpl
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

private const val FOCUS_OWNER = "focusOwner"

internal class EditorFocusListener(private val onEditorFocused: () -> Unit) : PropertyChangeListener {

  override fun propertyChange(event: PropertyChangeEvent) {
    if (event.newValue is EditorComponentImpl) {
      onEditorFocused()
    }
  }

  fun install() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(FOCUS_OWNER, this)
  }

  fun uninstall() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(FOCUS_OWNER, this)
  }
}
