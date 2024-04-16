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
public class ExOutputModel private constructor(private val myEditor: Editor) : VimExOutputPanel {
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
      field
    }
    set(value) {
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        ExOutputPanel.getInstance(myEditor).setText(value ?: "")
      } else {
        field = value
        isActiveInTestMode = !value.isNullOrEmpty()
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

  public companion object {
    @JvmStatic
    public fun getInstance(editor: Editor): ExOutputModel {
      var model = editor.vimExOutput
      if (model == null) {
        model = ExOutputModel(editor)
        editor.vimExOutput = model
      }
      return model
    }
  }
}
