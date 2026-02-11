/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCommandLineCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimModalInput
import com.maddyhome.idea.vim.api.VimModalInputBase
import com.maddyhome.idea.vim.api.VimModalInputService
import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptor
import com.maddyhome.idea.vim.newapi.ij

class ExEntryModalInputService : VimModalInputService {
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
    val panel = ExEntryPanel.getOrCreatePanelInstance()
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
