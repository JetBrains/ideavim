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
import com.maddyhome.idea.vim.api.VimOutputPanel
import com.maddyhome.idea.vim.helper.vimExOutput
import com.maddyhome.idea.vim.ui.ExOutputPanel

// TODO: We need a nicer way to handle output, especially wrt testing, appending + clearing
class ExOutputModel(private val myEditor: Editor) : VimExOutputPanel, VimOutputPanel {
  private var isActiveInTestMode = false

  override val isShown: Boolean
    get() = if (!ApplicationManager.getApplication().isUnitTestMode) {
      ExOutputPanel.getNullablePanel(myEditor)?.myActive ?: false
    } else {
      isActiveInTestMode
    }

  override val isActive: Boolean
    get() = if (!ApplicationManager.getApplication().isUnitTestMode) {
      ExOutputPanel.isPanelActive(myEditor)
    } else {
      isActiveInTestMode
    }

  override fun addText(text: String, isNewLine: Boolean) {
    if (this.text.isNotEmpty() && isNewLine) this.text += "\n$text" else this.text += text
  }

  override fun show() {
    myEditor.vimExOutput = this
    val exOutputPanel = ExOutputPanel.getInstance(myEditor)
    if (!exOutputPanel.myActive) {
      if (ApplicationManager.getApplication().isUnitTestMode) {
        isActiveInTestMode = true
      } else {
        exOutputPanel.activate()
      }
    }
  }

  override fun update() {
    // the current implementation updates text as soon as it is modified
  }

  override var text: String = ""
    get() = if (!ApplicationManager.getApplication().isUnitTestMode) {
      ExOutputPanel.getInstance(myEditor).text
    } else {
      // ExOutputPanel always returns a non-null string
      field
    }
    set(value) {
      // ExOutputPanel will strip a trailing newline. We'll do it now so that tests have the same behaviour. We also
      // never pass null to ExOutputPanel, but we do store it for tests, so we know if we're active or not
      val newValue = value.removeSuffix("\n")
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        ExOutputPanel.getInstance(myEditor).setText(newValue)
      } else {
        field = newValue
        isActiveInTestMode = newValue.isNotEmpty()
      }
    }

  override fun output(text: String) {
    this.text = text
  }

  override fun clear() {
    text = ""
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
