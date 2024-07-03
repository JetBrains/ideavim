/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.VimExOutputPanel
import com.maddyhome.idea.vim.helper.vimExOutput
import com.maddyhome.idea.vim.ui.ExOutputPanel

// TODO: We need a nicer way to handle output, especially wrt testing, appending + clearing
class ExOutputModel private constructor(private val myEditor: Editor) : VimExOutputPanel {
  private var isActiveInTestMode = false

  override val isActive: Boolean
    get() = if (!ApplicationManager.getApplication().isUnitTestMode) {
      ExOutputPanel.isPanelActive(myEditor)
    } else {
      isActiveInTestMode
    }

  override var text: String? = null
    get() = if (!ApplicationManager.getApplication().isUnitTestMode) {
      ExOutputPanel.getInstance(myEditor).text
    } else {
      // ExOutputPanel always returns a non-null string
      field ?: ""
    }
    set(value) {
      // ExOutputPanel will strip a trailing newline. We'll do it now so that tests have the same behaviour. We also
      // never pass null to ExOutputPanel, but we do store it for tests, so we know if we're active or not
      val newValue = value?.removeSuffix("\n")
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        ExOutputPanel.getInstance(myEditor).setText(newValue ?: "")
      } else {
        field = newValue
        isActiveInTestMode = !newValue.isNullOrEmpty()
      }
    }

  override fun output(text: String) {
    this.text = text
  }

  override fun clear() {
    text = null
  }

  override fun close() {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      ExOutputPanel.getInstance(myEditor).close()
    }
    else {
      isActiveInTestMode = false
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(editor: Editor): ExOutputModel {
      var model = editor.vimExOutput
      if (model == null) {
        model = ExOutputModel(editor)
        editor.vimExOutput = model
      }
      return model
    }

    @JvmStatic
    fun tryGetInstance(editor: Editor) = editor.vimExOutput
  }
}
