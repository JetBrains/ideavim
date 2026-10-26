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
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VirtualBufferGroup
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.CmdwinKeys
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.vim
import javax.swing.SwingConstants

class IjVirtualBufferGroup : VirtualBufferGroup {

  override fun open(
    context: ExecutionContext,
    editor: VimEditor,
    kind: VirtualBufferKind,
    content: String,
    focus: Boolean,
  ): VimEditor? {
    if (kind != VirtualBufferKind.SubstitutePreview && refuseIfAlreadyOpen(context, editor)) return null
    val project = projectOf(context) ?: (editor as IjVimEditor).editor.project ?: return null
    val file = createBufferFile(editor, kind, content)
    showFile(project, file, kind, focus)
    return if (focus) focusedEditorOf(project, file) else null
  }

  override fun close(editor: VimEditor) {
    val ijEditor = (editor as IjVimEditor).editor
    val virtualFile = ijEditor.virtualFile ?: return
    val project = ijEditor.project ?: return
    closeFile(project, virtualFile)
  }

  override fun close(context: ExecutionContext, kind: VirtualBufferKind) {
    val project = projectOf(context) ?: return
    findOpenFile(project, kind)?.let { closeFile(project, it) }
  }

  override fun isOpen(context: ExecutionContext, kind: VirtualBufferKind): Boolean =
    findOpenFile(projectOf(context), kind) != null

  override fun refresh(context: ExecutionContext, kind: VirtualBufferKind, content: String) {
    CommandProcessor.getInstance().runUndoTransparentAction {
      ApplicationManager.getApplication().runWriteAction {
        val project = projectOf(context) ?: return@runWriteAction
        val file = findOpenFile(project, kind) ?: return@runWriteAction
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runWriteAction
        if (!document.immutableCharSequence.contentEquals(content)) document.setText(content)
      }
    }
  }

  override fun openHelp(context: ExecutionContext, editor: VimEditor, content: String, line: Int) {
    val helpEditor = openReadOnly(context, editor, VirtualBufferKind.Help, content, line) ?: return
    // `:help` shows the tag at the top of the window
    injector.scroll.scrollCurrentLineToDisplayTop(helpEditor, line + 1, false)
  }

  override fun openQuickFix(context: ExecutionContext, editor: VimEditor, content: String, line: Int) {
    openReadOnly(context, editor, VirtualBufferKind.QuickFix, content, line)
    refreshQuickFix(context, content, line)
  }

  override fun refreshQuickFix(context: ExecutionContext, content: String, line: Int) {
    val project = projectOf(context) ?: return
    val file = findOpenFile(project, VirtualBufferKind.QuickFix) ?: return
    val document = FileDocumentManager.getInstance().getDocument(file) ?: return
    if (!document.immutableCharSequence.contentEquals(content)) {
      CommandProcessor.getInstance().runUndoTransparentAction {
        ApplicationManager.getApplication().runWriteAction {
          document.setReadOnly(false)
          document.setText(content)
          document.setReadOnly(true)
        }
      }
    }
    FileEditorManager.getInstance(project).getEditors(file).filterIsInstance<TextEditor>().forEach {
      val quickFixEditor = it.editor.vim
      quickFixEditor.primaryCaret().moveToOffset(quickFixEditor.getLineStartOffset(line))
    }
  }

  override fun selectOriginalWindow(buffer: VimEditor): VimEditor? {
    val bufferEditor = (buffer as IjVimEditor).editor
    val project = bufferEditor.project ?: return null
    val bufferFile = bufferEditor.virtualFile ?: return null
    val originalFile = bufferFile.getUserData(CmdwinKeys.ORIGINAL_FILE)
    val otherWindows = FileEditorManagerEx.getInstanceEx(project).windows.filter { it.selectedComposite?.file != bufferFile }
    val window = otherWindows.firstOrNull { it.selectedComposite?.file == originalFile } ?: otherWindows.firstOrNull()
      ?: return null
    window.setAsCurrentWindow(true)
    return (window.selectedComposite?.selectedEditor as? TextEditor)?.editor?.vim
  }

  /**
   * Moves to the window that already shows the buffer of [kind], or opens a read-only one with [content].
   *
   * @return the editor showing the buffer, with the caret at the start of [line]
   */
  private fun openReadOnly(
    context: ExecutionContext,
    editor: VimEditor,
    kind: VirtualBufferKind,
    content: String,
    line: Int,
  ): VimEditor? {
    val project = projectOf(context) ?: (editor as IjVimEditor).editor.project ?: return null
    val fem = FileEditorManagerEx.getInstanceEx(project)
    val file = findOpenFile(project, kind)
      ?: createBufferFile(editor, kind, content).apply { isWritable = false }
        .also { openSplitFile(fem, it, focus = true) }

    val bufferEditor = focusedEditorOf(project, file) ?: return null
    bufferEditor.primaryCaret().moveToOffset(bufferEditor.getLineStartOffset(line))
    return bufferEditor
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
    VirtualBufferKind.SubstitutePreview -> "A substitute preview is already open"
    VirtualBufferKind.Help -> "A help window is already open"
    VirtualBufferKind.QuickFix -> "A quickfix window is already open"
  }

  /** The kind of the currently open (nesting-restricted) virtual buffer, or null if none is open. */
  private fun openVirtualBufferKind(context: ExecutionContext): VirtualBufferKind? {
    val project = projectOf(context) ?: return null
    return FileEditorManager.getInstance(project).openFiles
      .mapNotNull { it.getUserData(CmdwinKeys.KIND) }
      // The inccommand=split preview is transient, the help and the quickfix list are regular read-only windows.
      // None of them blocks cmdwin
      .firstOrNull { it !in NON_BLOCKING_KINDS }
  }

  private fun findOpenFile(project: Project?, kind: VirtualBufferKind): VirtualFile? {
    project ?: return null
    return FileEditorManager.getInstance(project).openFiles
      .firstOrNull { it.getUserData(CmdwinKeys.KIND) == kind }
  }

  private fun createBufferFile(editor: VimEditor, kind: VirtualBufferKind, content: String): LightVirtualFile {
    val file = LightVirtualFile(kind.fileName, PlainTextFileType.INSTANCE, content)
    file.putUserData(CmdwinKeys.KIND, kind)
    (editor as IjVimEditor).editor.virtualFile?.let { file.putUserData(CmdwinKeys.ORIGINAL_FILE, it) }
    return file
  }

  private fun showFile(project: Project, file: LightVirtualFile, kind: VirtualBufferKind, focus: Boolean) {
    val fem = FileEditorManagerEx.getInstanceEx(project)
    openSplitFile(fem, file, focus)
    if (kind == VirtualBufferKind.SubstitutePreview) {
      fem.getEditors(file).filterIsInstance<TextEditor>()
        .forEach { (it.editor as? EditorEx)?.isViewer = true }
    }
  }

  /** Also waits for the editor of a just opened split to be created */
  private fun focusedEditorOf(project: Project, file: VirtualFile): VimEditor? =
    FileEditorManagerEx.getInstanceEx(project).openFile(file, true, true)
      .filterIsInstance<TextEditor>().firstOrNull()?.editor?.vim

  private fun openSplitFile(
    fem: FileEditorManagerEx,
    file: LightVirtualFile,
    focus: Boolean,
  ) {
    // in unit tests there is no way to create splits
    if (ApplicationManager.getApplication().isUnitTestMode) {
      fem.openFile(file, true)
      return
    }
    val window = fem.splitters.currentWindow
    if (window == null) {
      fem.openFile(file, focus)
      return
    }

    window.split(SwingConstants.HORIZONTAL, true, file, focus)
  }

  // Close the buffer in its hosting split so the split collapses. Plain `FileEditorManager.closeFile` would leave an
  // empty pane that the platform refills with the most-recently-active file, producing two copies of the original.
  private fun closeFile(project: Project, file: VirtualFile) {
    val fem = FileEditorManagerEx.getInstanceEx(project)
    val hostingWindows = fem.windows.filter { it.selectedComposite?.file == file }
    if (hostingWindows.isEmpty()) {
      fem.closeFile(file)
    } else {
      hostingWindows.forEach { it.closeFile(file) }
    }
  }

  private fun projectOf(context: ExecutionContext): Project? =
    PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context)

  private companion object {
    val NON_BLOCKING_KINDS = setOf(VirtualBufferKind.SubstitutePreview, VirtualBufferKind.Help, VirtualBufferKind.QuickFix)
  }
}
