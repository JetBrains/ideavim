/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.CommandLineScope
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner

class CommandLineScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
  ) : CommandLineScope {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  private val vimContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  private val activeCommandLine: VimCommandLine?
    get() = injector.commandLine.getActiveCommandLine()

  override val text: String
    get() = activeCommandLine?.actualText ?: ""

  override val caretPosition: Int
    get() = activeCommandLine?.caret?.offset ?: 0

  override val isActive: Boolean
    get() = activeCommandLine != null

  override fun setText(text: String) {
    activeCommandLine?.setText(text)
  }

  override fun insertText(offset: Int, text: String) {
    activeCommandLine?.insertText(offset, text)
  }

  override fun setCaretPosition(position: Int) {
    activeCommandLine?.caret?.offset = position
  }

  override fun createCommandPrompt(initialText: String): Boolean {
    if (activeCommandLine != null) return false
    injector.commandLine.createCommandPrompt(vimEditor, vimContext, 0, initialText)
    return true
  }

  override fun createSearchPrompt(label: String, initialText: String): Boolean {
    if (activeCommandLine != null) return false
    injector.commandLine.createSearchPrompt(vimEditor, vimContext, label, initialText)
    return true
  }

  override fun input(prompt: String, finishOn: Char?, callback: VimScope.(String) -> Unit) {
    val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
    injector.commandLine.readInputAndProcess(vimEditor, vimContext, prompt, finishOn) {
      vimScope.callback(it)
    }
  }

  override fun close(refocusEditor: Boolean): Boolean {
    val commandLine = activeCommandLine ?: return false
    commandLine.close(refocusEditor, true)
    return true
  }
}