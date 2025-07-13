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
import com.maddyhome.idea.vim.api.VimOutputPanelBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.keyStroke
import com.maddyhome.idea.vim.helper.vimExOutput
import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.ui.ExOutputPanel
import java.lang.ref.WeakReference

// TODO: We need a nicer way to handle output, especially wrt testing, appending + clearing
class ExOutputModel(private val myEditor: WeakReference<Editor>) : VimOutputPanelBase() {
  private var isActiveInTestMode = false

  val editor get() = myEditor.get()

  val isActive: Boolean
    get() = if (!ApplicationManager.getApplication().isUnitTestMode) {
      editor?.let { ExOutputPanel.getNullablePanel(it) }?.myActive ?: false
    } else {
      isActiveInTestMode
    }

  override fun addText(text: String, isNewLine: Boolean) {
    if (this.text.isNotEmpty() && isNewLine) this.text += "\n$text" else this.text += text
  }

  override fun show() {
    if (editor == null) return
    val currentPanel = injector.outputPanel.getCurrentOutputPanel()
    if (currentPanel != null && currentPanel != this) currentPanel.close()

    editor!!.vimExOutput = this
    val exOutputPanel = ExOutputPanel.getInstance(editor!!)
    if (!exOutputPanel.myActive) {
      if (ApplicationManager.getApplication().isUnitTestMode) {
        isActiveInTestMode = true
      } else {
        exOutputPanel.activate()
      }
    }
  }

  override fun scrollPage() {
    val notNullEditor = editor ?: return
    val panel = ExOutputPanel.getNullablePanel(notNullEditor) ?: return
    panel.scrollPage()
  }

  override fun scrollHalfPage() {
    val notNullEditor = editor ?: return
    val panel = ExOutputPanel.getNullablePanel(notNullEditor) ?: return
    panel.scrollHalfPage()
  }

  override fun scrollLine() {
    val notNullEditor = editor ?: return
    val panel = ExOutputPanel.getNullablePanel(notNullEditor) ?: return
    panel.scrollLine()
  }

  override var text: String = ""
    get() = if (!ApplicationManager.getApplication().isUnitTestMode) {
      editor?.let { ExOutputPanel.getInstance(it).text } ?: ""
    } else {
      // ExOutputPanel always returns a non-null string
      field
    }
    set(value) {
      // ExOutputPanel will strip a trailing newline. We'll do it now so that tests have the same behaviour. We also
      // never pass null to ExOutputPanel, but we do store it for tests, so we know if we're active or not
      val newValue = value.removeSuffix("\n")
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        editor?.let { ExOutputPanel.getInstance(it).setText(newValue) }
      } else {
        field = newValue
        isActiveInTestMode = newValue.isNotEmpty()
      }
    }
  override var label: String
    get() {
      val notNullEditor = editor ?: return ""
      val panel = ExOutputPanel.getNullablePanel(notNullEditor) ?: return ""
      return panel.myLabel.text
    }
    set(value) {
      val notNullEditor = editor ?: return
      val panel = ExOutputPanel.getNullablePanel(notNullEditor) ?: return
      panel.myLabel.text = value
    }

  fun output(text: String) {
    this.text = text
  }

  fun clear() {
    text = ""
  }

  override val atEnd: Boolean
    get() {
      val notNullEditor = editor ?: return false
      val panel = ExOutputPanel.getNullablePanel(notNullEditor) ?: return false
      return panel.isAtEnd()
    }

  override fun onBadKey() {
    val notNullEditor = editor ?: return
    val panel = ExOutputPanel.getNullablePanel(notNullEditor) ?: return
    panel.onBadKey()
  }

  override fun close(key: VimKeyStroke?) {
    val notNullEditor = editor ?: return
    val panel = ExOutputPanel.getNullablePanel(notNullEditor) ?: return
    panel.close(key?.keyStroke)
  }

  override fun close() {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      editor?.let { ExOutputPanel.getInstance(it).close() }
    } else {
      isActiveInTestMode = false
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(editor: Editor): ExOutputModel {
      var model = editor.vimExOutput
      if (model == null) {
        model = ExOutputModel(WeakReference(editor))
        editor.vimExOutput = model
      }
      return model
    }

    @JvmStatic
    fun tryGetInstance(editor: Editor) = editor.vimExOutput
  }
}
