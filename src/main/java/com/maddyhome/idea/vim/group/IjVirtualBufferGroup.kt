/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VirtualBufferGroup
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.CmdwinKeys
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimEditor
import javax.swing.SwingConstants

class IjVirtualBufferGroup : VirtualBufferGroup {

  override fun open(
    context: ExecutionContext,
    editor: VimEditor,
    kind: VirtualBufferKind,
    content: String,
  ) {
    if (refuseIfAlreadyOpen(context, editor)) return
    val project = projectOf(context) ?: return
    showFile(project, createBufferFile(editor, kind, content))
  }

  override fun close(editor: VimEditor) {
    val ijEditor = (editor as IjVimEditor).editor
    val virtualFile = ijEditor.virtualFile ?: return
    val project = ijEditor.project ?: return
    val fem = FileEditorManagerEx.getInstanceEx(project)
    // Close the buffer in its hosting split so the split collapses. Plain
    // `FileEditorManager.closeFile` would leave an empty pane that the platform refills
    // with the most-recently-active file, producing two copies of the original editor.
    val hostingWindows = fem.windows.filter { it.selectedComposite?.file == virtualFile }
    if (hostingWindows.isEmpty()) {
      fem.closeFile(virtualFile)
    } else {
      hostingWindows.forEach { it.closeFile(virtualFile) }
    }
  }

  /**
   * Refuses to open a new virtual buffer while one is already open — they can't be nested
   * (`:help cmdwin`). Shows an error and returns true if the caller should abort.
   */
  private fun refuseIfAlreadyOpen(context: ExecutionContext, editor: VimEditor): Boolean {
    val openKind = openVirtualBufferKind(context) ?: return false
    injector.messages.showErrorMessage(editor, alreadyOpenMessage(openKind))
    return true
  }

  private fun alreadyOpenMessage(openKind: VirtualBufferKind): String = when (openKind) {
    VirtualBufferKind.Command, is VirtualBufferKind.Search -> "E1292: Command-line window is already open"
    VirtualBufferKind.ControlCharsEditor -> "A control characters editor is already open"
  }

  /** The kind of the currently open virtual buffer, or null if none is open. */
  private fun openVirtualBufferKind(context: ExecutionContext): VirtualBufferKind? {
    val project = projectOf(context) ?: return null
    return FileEditorManager.getInstance(project).openFiles
      .firstNotNullOfOrNull { it.getUserData(CmdwinKeys.KIND) }
  }

  private fun createBufferFile(editor: VimEditor, kind: VirtualBufferKind, content: String): LightVirtualFile {
    val file = LightVirtualFile(kind.fileName, PlainTextFileType.INSTANCE, content)
    file.putUserData(CmdwinKeys.KIND, kind)
    (editor as IjVimEditor).editor.virtualFile?.let { file.putUserData(CmdwinKeys.ORIGINAL_FILE, it) }
    return file
  }

  private fun showFile(project: Project, file: LightVirtualFile) {
    val fem = FileEditorManagerEx.getInstanceEx(project)
    if (ApplicationManager.getApplication().isUnitTestMode) {
      fem.openFile(file, true)
      return
    }
    val window = fem.splitters.currentWindow
    if (window != null) {
      window.split(SwingConstants.HORIZONTAL, true, file, true)
    } else {
      fem.openFile(file, true)
    }
  }

  private fun projectOf(context: ExecutionContext): Project? =
    PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context)
}
