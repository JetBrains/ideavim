/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.VimOutputPanel
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.vimExOutput
import java.awt.Color
import java.awt.event.KeyEvent
import java.lang.ref.WeakReference
import javax.swing.KeyStroke

// TODO: We need a nicer way to handle output, especially wrt testing, appending + clearing
class OutputModel(private val myEditor: WeakReference<Editor>) : VimOutputPanel {

  private val segments = mutableListOf<TextLine>()

  val editor: Editor? get() = myEditor.get()

  override val isActive: Boolean
    get() = getNullablePanel()?.myActive ?: false

  override var text: String
    get() = getOrCreatePanel().text ?: ""
    set(value) {
      val newValue = value.removeSuffix("\n")
      segments.clear()
      if (newValue.isEmpty()) return
      segments.add(TextLine(newValue, null))
    }

  override var label: String
    get() {
      val notNullEditor = editor ?: return ""
      val panel = OutputPanel.getNullablePanel(notNullEditor) ?: return ""
      return panel.myLabel.text
    }
    set(value) {
      val notNullEditor = editor ?: return
      val panel = OutputPanel.getNullablePanel(notNullEditor) ?: return
      panel.myLabel.text = value
    }

  override fun addText(text: String, isNewLine: Boolean, color: Color?) {
    segments.add(TextLine(text, color))
  }

  override fun show() {
    if (editor == null) return
    val currentPanel = injector.outputPanel.getCurrentOutputPanel()
    if (currentPanel != null && currentPanel != this) currentPanel.close()

    editor?.let { it.vimExOutput = this }
    val panel = getOrCreatePanel()
    panel.setStyledText(segments)
    if (!panel.myActive) {
      panel.activate()
    }
  }

  override fun close() {
    editor?.let { getOrCreatePanel().close() }
  }

  override fun setContent(text: String) {
    this.text = text
  }

  override fun clearText() {
    segments.clear()
  }

  override fun handleKey(key: KeyStroke) {
    if (atEnd) {
      closeWithKey(key)
      return
    }

    when (key.keyChar) {
      ' ' -> scrollPage()
      'd' -> scrollHalfPage()
      'q', '\u001b' -> close()
      '\n' -> scrollLine()
      KeyEvent.CHAR_UNDEFINED -> {
        when (key.keyCode) {
          KeyEvent.VK_ENTER -> scrollLine()
          KeyEvent.VK_ESCAPE -> close()
          else -> onBadKey()
        }
      }

      else -> onBadKey()
    }
  }

  override fun scrollPage() {
    val notNullEditor = editor ?: return
    val panel = OutputPanel.getNullablePanel(notNullEditor) ?: return
    panel.scrollPage()
  }

  override fun scrollHalfPage() {
    val notNullEditor = editor ?: return
    val panel = OutputPanel.getNullablePanel(notNullEditor) ?: return
    panel.scrollHalfPage()
  }

  override fun scrollLine() {
    val notNullEditor = editor ?: return
    val panel = OutputPanel.getNullablePanel(notNullEditor) ?: return
    panel.scrollLine()
  }

  fun output(text: String) {
    this.text = text
  }

  fun clear() {
    text = ""
  }

  private val atEnd: Boolean
    get() {
      val notNullEditor = editor ?: return false
      val panel = OutputPanel.getNullablePanel(notNullEditor) ?: return false
      return panel.isAtEnd
    }

  private fun onBadKey() {
    val notNullEditor = editor ?: return
    val panel = OutputPanel.getNullablePanel(notNullEditor) ?: return
    panel.onBadKey()
  }

  private fun closeWithKey(key: KeyStroke?) {
    editor ?: return
    val panel = getNullablePanel() ?: return
    panel.close(key)
  }

  private fun getNullablePanel(): OutputPanel? {
    return editor?.let { OutputPanel.getNullablePanel(it) }
  }

  private fun getOrCreatePanel(): OutputPanel {
    return OutputPanel.getInstance(editor!!)
  }

  companion object {
    @JvmStatic
    fun getInstance(editor: Editor): OutputModel {
      var model = editor.vimExOutput
      if (model == null) {
        model = OutputModel(WeakReference(editor))
        editor.vimExOutput = model
      }
      return model
    }

    @JvmStatic
    fun tryGetInstance(editor: Editor) = editor.vimExOutput
  }
}