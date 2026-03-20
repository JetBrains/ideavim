/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.file

import com.intellij.ide.vfs.VirtualFileId
import com.intellij.ide.vfs.virtualFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.openapi.editor.impl.findEditorOrNull
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorsSplitters
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.maddyhome.idea.vim.group.findVirtualFile
import com.maddyhome.idea.vim.group.onEdt
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import kotlin.io.path.Path

/**
 * RPC handler for [FileRemoteApi].
 * Contains all file operation logic directly — no intermediate service layer.
 *
 * Read-only methods use [readAction]; mutating methods use [onEdt].
 *
 * **Options are never read here** — they are resolved on the frontend and passed
 * as explicit parameters (e.g. [saveFile]'s `saveAll` flag).
 */
internal class FileRemoteApiImpl : FileRemoteApi {

  override suspend fun findFile(filename: String, projectId: ProjectId?): String? = readAction {
    val project = projectId?.findProjectOrNull() ?: return@readAction null
    findFile(filename, project)?.path
  }

  override suspend fun openFile(filename: String, projectId: ProjectId?, focusEditor: Boolean): String? = onEdt {
    val project = projectId?.findProjectOrNull() ?: return@onEdt "No project found"
    var file = findFile(filename, project)

    if (file == null) {

      val ioFile = resolveIoFile(filename, project)
        ?: return@onEdt EngineMessageHelper.message(
          "message.open.file.not.found",
          filename
        )
      WriteCommandAction.runWriteCommandAction(project) {
        ioFile.parentFile?.mkdirs()
        if (!ioFile.exists()) {
          ioFile.createNewFile()
        }
      }
      file = LocalFileSystem.getInstance()
        .refreshAndFindFileByIoFile(ioFile)
    }

    if (file != null) {
      FileEditorManager.getInstance(project)
        .openFile(file, focusEditor)
      null
    } else {
      EngineMessageHelper.message("message.open.file.not.found", filename)
    }
  }

  override suspend fun closeCurrentFile(projectId: ProjectId?, virtualFileId: VirtualFileId?) =
    onEdt {
      val project = projectId?.findProjectOrNull() ?: return@onEdt
      val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
      val window = fileEditorManager.currentWindow
      val currentFile = fileEditorManager.currentFile

      if (currentFile != null && window != null) {
        window.closeFile(currentFile)
        window.requestFocus(true)
        if (!ApplicationManager.getApplication().isUnitTestMode) {
          EditorsSplitters.focusDefaultComponentInSplittersIfPresent(project)
        }
      } else {
        val vf = virtualFileId?.virtualFile()
        if (vf != null) {
          fileEditorManager.closeFile(vf)
        }
      }
    }

  override suspend fun closeFile(number: Int, projectId: ProjectId?) = onEdt {
    val project = projectId?.findProjectOrNull() ?: return@onEdt
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    val window = fileEditorManager.currentWindow
    val editors = fileEditorManager.openFiles
    if (window != null) {
      if (number >= 0 && number < editors.size) {
        fileEditorManager.closeFile(editors[number], window)
      }
    }
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      EditorsSplitters.focusDefaultComponentInSplittersIfPresent(project)
    }
  }

  override suspend fun saveFile(editorId: EditorId, saveAll: Boolean) =
    onEdt {
      val editor = editorId.findEditorOrNull() ?: return@onEdt
      if (saveAll) {
        ApplicationManager.getApplication().saveAll()
      } else {
        val document = editor.document
        val fileDocumentManager = FileDocumentManager.getInstance()
        fileDocumentManager.saveDocument(document)
      }
    }

  override suspend fun selectFile(count: Int, projectId: ProjectId?): Boolean = onEdt {
    var idx = count
    val project = projectId?.findProjectOrNull() ?: return@onEdt false
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    if (idx == 99) {
      idx = editors.size - 1
    }
    if (idx < 0 || idx >= editors.size) {
      return@onEdt false
    }
    fem.openFile(editors[idx], true)
    true
  }

  override suspend fun selectNextFile(count: Int, projectId: ProjectId?) = onEdt {
    val project = projectId?.findProjectOrNull() ?: return@onEdt
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    val current = fem.selectedFiles.getOrNull(0) ?: return@onEdt
    for (i in editors.indices) {
      if (editors[i] == current) {
        val pos = (i + (count % editors.size) + editors.size) % editors.size
        fem.openFile(editors[pos], true)
      }
    }
  }

  override suspend fun buildFileInfoMessage(editorId: EditorId, fullPath: Boolean): String? =
    readAction {
      val editor = editorId.findEditorOrNull() ?: return@readAction null
      val project = editor.project ?: return@readAction null
      buildFileInfoMessage(editor, project, fullPath)
    }

  override suspend fun selectEditor(projectId: ProjectId, documentPath: String, protocol: String): Boolean =
    onEdt {
      val virtualFile = findVirtualFile(documentPath, protocol) ?: return@onEdt false
      val project = projectId.findProjectOrNull() ?: return@onEdt false
      val fMgr = FileEditorManager.getInstance(project)
      val feditors = fMgr.openFile(virtualFile, true)
      val first = feditors.firstOrNull()
      if (first is TextEditor) !first.editor.isDisposed else false
    }

  // ======================== Private helpers ========================

  private fun resolveIoFile(filename: String, project: Project): java.io.File? {

    // ~/path support
    if (filename.startsWith("~/") || filename.startsWith("~\\")) {
      val home = System.getProperty("user.home")
      return java.io.File(home, filename.substring(2))
    }

    val file = java.io.File(filename)

    // absolute path
    if (file.isAbsolute) {
      return file
    }

    // relative → project root
    val basePath = project.basePath ?: return null
    return java.io.File(basePath, filename)
  }
  private fun findFile(filename: String, project: Project): VirtualFile? {
    if (filename.startsWith("~/") || filename.startsWith("~\\")) {
      val relativePath = filename.substring(2)
      val dir = System.getProperty("user.home")
      logger.debug { "home dir file" }
      logger.debug { "looking for $relativePath in $dir" }
      return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(Path(dir, relativePath))
    }

    val basePath = project.basePath
    if (basePath != null) {
      val baseDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(Path(basePath))
      baseDir?.findFileByRelativePath(filename)?.let { return it }
    }

    VirtualFileManager.getInstance().findFileByNioPath(Path(filename))?.let { return it }

    findByNameInContentRoots(filename, project)?.let { return it }

    findByNameInProject(filename, project)?.let { return it }

    return null
  }

  private fun buildFileInfoMessage(editor: Editor, project: Project, fullPath: Boolean): String {
    val msg = StringBuilder()
    val vf = editor.virtualFile
    if (vf != null) {
      msg.append('"')
      if (fullPath) {
        msg.append(vf.path)
      } else {
        val root = ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(vf)
        if (root != null) {
          msg.append(vf.path.substring(root.path.length + 1))
        } else {
          msg.append(vf.path)
        }
      }
      msg.append("\" ")
    } else {
      msg.append("\"[No File]\" ")
    }

    if (!editor.document.isWritable) {
      msg.append("[RO] ")
    } else if (FileDocumentManager.getInstance().isDocumentUnsaved(editor.document)) {
      msg.append("[+] ")
    }

    val logicalPosition = editor.caretModel.logicalPosition
    val lline = logicalPosition.line
    val total = editor.document.lineCount
    val pct = if (total > 0) (lline.toFloat() / total.toFloat() * 100f + 0.5).toInt() else 0

    msg.append("line ").append(lline + 1).append(" of ").append(total)
    msg.append(" --").append(pct).append("%-- ")

    msg.append("col ").append(logicalPosition.column + 1)

    return msg.toString()
  }

  private fun findByNameInContentRoots(filename: String, project: Project): VirtualFile? {
    var found: VirtualFile? = null
    val prm = ProjectRootManager.getInstance(project)
    val roots = prm.contentRoots
    for (i in roots.indices) {
      logger.debug { "root[$i] = ${roots[i].path}" }
      found = roots[i].findFileByRelativePath(filename)
      if (found != null) {
        break
      }
    }
    return found
  }

  private fun findByNameInProject(filename: String, project: Project): VirtualFile? {
    val projectScope = ProjectScope.getProjectScope(project)
    val names = FilenameIndex.getVirtualFilesByName(filename, projectScope)
    return names.firstOrNull()
  }

  companion object {
    private val logger = Logger.getInstance(FileRemoteApiImpl::class.java.name)
  }
}
