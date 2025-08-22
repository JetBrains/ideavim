/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.OutputPanelScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOutputPanel
import com.maddyhome.idea.vim.api.injector

object OutputPanelScopeImpl : OutputPanelScope {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  private val vimContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  private val outputPanel: VimOutputPanel = injector.outputPanel.getOrCreate(vimEditor, vimContext)

  override val text: String
    get() = outputPanel.text

  override val label: String
    get() = outputPanel.label

  override fun setText(text: String) {
    outputPanel.setContent(text)
    outputPanel.show() // has to be called to update the text
  }

  override fun appendText(text: String, startNewLine: Boolean) {
    outputPanel.addText(text, startNewLine)
    outputPanel.show() // has to be called to update the text
  }

  override fun setLabel(label: String) {
    outputPanel.label = label
  }

  override fun clearText() {
    outputPanel.clearText()
    outputPanel.show() // has to be called to update the text
  }
}