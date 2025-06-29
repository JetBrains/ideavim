/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.ModalInput
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimModalInput
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptorBase
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class ModalInputImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : ModalInput {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  private val vimContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  private var repeatCondition: (() -> Boolean)? = null
  private var repeatCount: Int? = null
  private var updateLabel: ((String) -> String)? = null

  override fun updateLabel(block: (String) -> String): ModalInput {
    updateLabel = block
    return this
  }

  override fun repeatUntil(condition: () -> Boolean): ModalInput {
    repeatCondition = condition
    return this
  }

  override fun repeat(count: Int): ModalInput {
    repeatCount = count
    return this
  }

  override fun inputString(label: String, handler: VimScope.(String) -> Unit) {
    val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
    val interceptor = TextInputInterceptor(repeatCount, repeatCondition, updateLabel) {
      vimScope.handler(it)
    }
    val modalInput = injector.modalInput.create(vimEditor, vimContext, label, interceptor)
    interceptor.modalInput = modalInput
  }

  override fun inputChar(label: String, handler: VimScope.(Char) -> Unit) {
    val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
    val interceptor = CharInputInterceptor(repeatCount, repeatCondition, updateLabel) { char ->
      vimScope.handler(char)
    }
    val modalInput = injector.modalInput.create(vimEditor, vimContext, label, interceptor)
    interceptor.modalInput = modalInput
  }

  override fun closeCurrentInput(refocusEditor: Boolean): Boolean {
    val currentInput = injector.modalInput.getCurrentModalInput() ?: return false
    currentInput.deactivate(refocusEditor, true)
    return true
  }

  private abstract class InputInterceptorBase<T>(
    protected val repeatCount: Int? = null,
    protected val repeatCondition: (() -> Boolean)? = null,
    protected var updateLabelFn: ((String) -> String)? = null,
    protected val handler: (T) -> Unit
  ): VimInputInterceptorBase<T>() {
    lateinit var modalInput: VimModalInput
    var counter = 0

    private fun updateLabel(newLabel: String) {
      val vimEditor = injector.editorGroup.getFocusedEditor()!!
      val vimContext = injector.executionContextManager.getEditorExecutionContext(vimEditor)

      val modalInput = this.modalInput
      modalInput.deactivate(refocusOwningEditor = false, resetCaret = false)
      this.modalInput = injector.modalInput.create(vimEditor, vimContext, newLabel, this)
    }

    override fun executeInput(input: T, editor: VimEditor, context: ExecutionContext) {
      handler(input)
      counter++

      val hasRepeatCondition = repeatCount != null || repeatCondition != null
      if (hasRepeatCondition && (counter == repeatCount || repeatCondition?.invoke() == true)) {
        modalInput.deactivate(refocusOwningEditor = true, resetCaret = true)
        return
      }

      if (!hasRepeatCondition) {
        modalInput.deactivate(refocusOwningEditor = true, resetCaret = true)
        return
      }

      val currentLabel = modalInput.label
      val newLabel = updateLabelFn?.invoke(currentLabel) ?: currentLabel
      if (currentLabel != newLabel) {
        updateLabel(newLabel)
      }
    }
  }

  private class TextInputInterceptor(
    repeatCount: Int? = null,
    repeatCondition: (() -> Boolean)? = null,
    updateLabelFn: ((String) -> String)? = null,
    handler: (String) -> Unit,
  ) : InputInterceptorBase<String>(repeatCount, repeatCondition, updateLabelFn, handler) {

    private val textBuffer = StringBuilder()

    override fun buildInput(key: KeyStroke): String? {
      if (key.isCloseKeyStroke()) return ""

      // If Enter is pressed, return the current text
      if (key.keyCode == KeyEvent.VK_ENTER) {
        return textBuffer.toString()
      }

      // todo: see if this makes sense
      if (key.keyCode == KeyEvent.VK_BACK_SPACE) {
        if (textBuffer.isNotEmpty()) {
          textBuffer.deleteCharAt(textBuffer.length - 1)
        }
        return null
      }

      // Append the character to the text
      if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
        textBuffer.append(key.keyChar)
      }

      return null
    }
  }

  private class CharInputInterceptor(
    repeatCount: Int? = null,
    repeatCondition: (() -> Boolean)? = null,
    updateLabelFn: ((String) -> String)? = null,
    handler: (Char) -> Unit,
  ) : InputInterceptorBase<Char>(repeatCount, repeatCondition, updateLabelFn, handler) {
    private var currentChar: Char? = null

    override fun buildInput(key: KeyStroke): Char? {
      if (key.isCloseKeyStroke()) return '\u0000'

      val keyChar = key.keyChar
      if (keyChar != KeyEvent.CHAR_UNDEFINED) {
        currentChar = keyChar
        return keyChar
      }

      return null
    }
  }
}

private fun KeyStroke.isCloseKeyStroke(): Boolean {
  return keyCode == KeyEvent.VK_ESCAPE
}