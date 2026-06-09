/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.editor

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.injector

/**
 * Opens a dedicated editor for editing registers/macros in printable (caret-notation) form.
 *
 * IntelliJ Documents cannot hold a lone carriage return (0x0D), so control keys such as `<CR>`
 * cannot be pasted into a buffer as raw bytes. Instead, this editor will show registers in their
 * printable form (e.g. `^M`, `^[`) and parse that form back into keystrokes on commit, allowing
 * macros that contain control characters to be edited.
 */
internal class OpenControlCharsEditorAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = injector.editorGroup.getSelectedEditor() ?: return
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    injector.virtualBufferGroup.open(
      context,
      editor,
      VirtualBufferKind.ControlCharsEditor,
      ""
    )
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.getData(CommonDataKeys.EDITOR) != null
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
