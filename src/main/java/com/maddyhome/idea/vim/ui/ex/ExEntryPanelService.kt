/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.action.change.Extension
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimCommandLineCaret
import com.maddyhome.idea.vim.api.VimCommandLineServiceBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimModalInput
import com.maddyhome.idea.vim.api.VimModalInputBase
import com.maddyhome.idea.vim.api.VimModalInputService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.TestInputModel
import com.maddyhome.idea.vim.helper.inRepeatMode
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.ui.ModalEntry
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class ExEntryPanelService : VimCommandLineServiceBase(), VimModalInputService {
  override fun getActiveCommandLine(): VimCommandLine? {
    val instance = ExEntryPanel.instance ?: return null
    return if (instance.isActive) instance else null
  }

  @Deprecated("Please use readInputAndProcess")
  fun inputString(vimEditor: VimEditor, context: ExecutionContext, prompt: String, finishOn: Char?): String? {
    val editor = vimEditor.ij
    if (vimEditor.inRepeatMode) {
      val input = Extension.consumeString()
      return input ?: error("Not enough strings saved: ${Extension.lastExtensionHandler}")
    }

    if (ApplicationManager.getApplication().isUnitTestMode) {
      val builder = StringBuilder()
      val inputModel = TestInputModel.getInstance(editor)
      var key: KeyStroke? = inputModel.nextKeyStroke()
      while (key != null &&
        !key.isCloseKeyStroke() && key.keyCode != KeyEvent.VK_ENTER &&
        (finishOn == null || key.keyChar != finishOn)
      ) {
        val c = key.keyChar
        if (c != KeyEvent.CHAR_UNDEFINED) {
          builder.append(c)
        }
        key = inputModel.nextKeyStroke()
      }
      if (finishOn != null && key != null && key.keyChar == finishOn) {
        builder.append(key.keyChar)
      }
      Extension.addString(builder.toString())
      return builder.toString()
    } else {
      var text: String? = null
      // XXX: The Ex entry panel is used only for UI here, its logic might be inappropriate for input()
      val commandLine = injector.commandLine.createSearchPrompt(vimEditor, context, prompt.ifEmpty { " " }, "")
      ModalEntry.activate(editor.vim) { key: KeyStroke ->
        return@activate when {
          key.isCloseKeyStroke() -> {
            commandLine.deactivate(refocusOwningEditor = true, resetCaret = true)
            false
          }

          key.keyCode == KeyEvent.VK_ENTER -> {
            text = commandLine.text
            commandLine.deactivate(refocusOwningEditor = true, resetCaret = true)
            false
          }

          finishOn != null && key.keyChar == finishOn -> {
            commandLine.handleKey(key)
            text = commandLine.text
            commandLine.deactivate(refocusOwningEditor = true, resetCaret = true)
            false
          }

          else -> {
            commandLine.handleKey(key)
            true
          }
        }
      }
      if (text != null) {
        Extension.addString(text)
      }
      return text
    }
  }

  override fun readInputAndProcess(
    vimEditor: VimEditor,
    context: ExecutionContext,
    prompt: String,
    finishOn: Char?,
    processing: (String) -> Unit,
  ) {
    val currentMode = vimEditor.mode

    // Make sure the Visual selection marks are up to date before we use them.
    injector.markService.setVisualSelectionMarks(vimEditor)

    // Note that we should remove selection and reset caret offset before we switch back to Normal mode and then enter
    // Command-line mode. However, some IdeaVim commands can handle multiple carets, including multiple carets with
    // selection (which might or might not be a block selection). Unfortunately, because we're just entering
    // Command-line mode, we don't know which command is going to be entered, so we can't remove selection here.
    // Therefore, we switch to Normal and then Command-line even though we might still have a Visual selection...
    // On the plus side, it means we still show selection while editing the command line, which Vim also does
    // (Normal then Command-line is not strictly necessary, but done for completeness and autocmd)
    // Caret selection is finally handled in Command.execute
    vimEditor.mode = Mode.NORMAL()
    vimEditor.mode = Mode.CMD_LINE(currentMode)

    val panel = ExEntryPanel.getOrCreateInstance()
    panel.finishOn = finishOn
    panel.inputProcessing = processing
    panel.activate(vimEditor.ij, context.ij, prompt, "")
  }

  override fun createPanel(
    editor: VimEditor,
    context: ExecutionContext,
    label: String,
    initText: String,
  ): VimCommandLine {
    val panel = ExEntryPanel.getOrCreateInstance()
    panel.activate(editor.ij, context.ij, label, initText)
    return panel
  }

  override fun fullReset() {
    ExEntryPanel.fullReset()
  }

  override fun getCurrentModalInput(): VimModalInput? {
    val instance = ExEntryPanel.instance ?: return null
    if (!instance.isActive || instance.inputInterceptor == null) return null
    return WrappedAsModalInputExEntryPanel(instance)
  }

  override fun create(
    editor: VimEditor,
    context: ExecutionContext,
    label: String,
    inputInterceptor: VimInputInterceptor,
  ): VimModalInput {
    val panel = ExEntryPanel.getOrCreateInstance()
    panel.inputInterceptor = inputInterceptor
    panel.activate(editor.ij, context.ij, label, "")
    return WrappedAsModalInputExEntryPanel(panel)
  }
}

internal class WrappedAsModalInputExEntryPanel(internal val exEntryPanel: ExEntryPanel) : VimModalInputBase() {
  override var inputInterceptor: VimInputInterceptor
    get() = exEntryPanel.inputInterceptor!!
    set(value) {
      exEntryPanel.inputInterceptor = value
    }
  override val caret: VimCommandLineCaret = exEntryPanel.caret
  override val label: String = exEntryPanel.getLabel()

  override fun deactivate(refocusOwningEditor: Boolean, resetCaret: Boolean) {
    exEntryPanel.deactivate(refocusOwningEditor, resetCaret)
  }

  override fun focus() {
    exEntryPanel.focus()
  }
}
