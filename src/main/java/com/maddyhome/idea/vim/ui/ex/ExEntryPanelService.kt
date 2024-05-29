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
import com.maddyhome.idea.vim.api.VimCommandLineService
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.TestInputModel
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.ui.ModalEntry
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

public class ExEntryPanelService : VimCommandLineService {
  public override fun getActiveCommandLine(): VimCommandLine? {
    val instance = ExEntryPanel.instance ?: ExEntryPanel.instanceWithoutShortcuts ?: return null
    return if (instance.isActive) instance else null
  }

  override fun inputString(vimEditor: VimEditor, context: ExecutionContext, prompt: String, finishOn: Char?): String? {
    val editor = vimEditor.ij
    if (vimEditor.vimStateMachine.isDotRepeatInProgress) {
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
      val commandLine = injector.commandLine.create(vimEditor, context, prompt.ifEmpty { " " }, "", 1)
      ModalEntry.activate(editor.vim) { key: KeyStroke ->
        return@activate when {
          key.isCloseKeyStroke() -> {
            commandLine.deactivate(true)
            false
          }
          key.keyCode == KeyEvent.VK_ENTER -> {
            text = commandLine.text
            commandLine.deactivate(true)
            false
          }
          finishOn != null && key.keyChar == finishOn -> {
            commandLine.handleKey(key)
            text = commandLine.text
            commandLine.deactivate(true)
            false
          }
          else -> {
            commandLine.handleKey(key)
            true
          }
        }
      }
      if (text != null) {
        Extension.addString(text!!)
      }
      return text
    }
  }

  public override fun create(editor: VimEditor, context: ExecutionContext, label: String, initText: String, count: Int): VimCommandLine {
    val panel = ExEntryPanel.getInstance()
    panel.activate(editor.ij, context.ij, label, initText, count)
    return panel
  }

  public override fun createWithoutShortcuts(
    editor: VimEditor,
    context: ExecutionContext,
    label: String,
    initText: String,
    count: Int
  ): VimCommandLine {
    val panel = ExEntryPanel.getInstanceWithoutShortcuts()
    panel.activate(editor.ij, context.ij, label, initText, count)
    return panel
  }

  override fun fullReset() {
    ExEntryPanel.fullReset()
  }
}
