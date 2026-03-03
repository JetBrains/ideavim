/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorsSplitters
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.execute
import com.maddyhome.idea.vim.newapi.vim
import kotlin.io.path.Path

/**
 * Backend [FileBackendService] implementation.
 *
 * Provides direct implementations of file operations using IntelliJ Platform APIs.
 * Registered as an application service in `ideavim-backend.xml`.
 *
 * **Options are never read here** — they are resolved on the frontend and passed
 * as explicit parameters (e.g. [saveFile]'s `saveAll` flag).
 */
class FileBackendServiceImpl : FileBackendService {

  // ======================== Interface methods (serializable params) ========================

  override fun findFile(filename: String, projectId: String?): String? {
    val project = resolveProject(projectId) ?: return null
    return findFile(filename, project)?.path
  }

  override fun openFile(filename: String, projectId: String?, focusEditor: Boolean): String? {
    val project = resolveProject(projectId) ?: return "No project"
    val found = findFile(filename, project)

    if (found != null) {
      if (logger.isDebugEnabled) {
        logger.debug("found file: $found")
      }
      val type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(found, project)
      if (type != null) {
        FileEditorManager.getInstance(project).openFile(found, focusEditor)
      }
      return null
    } else {
      return EngineMessageHelper.message("message.open.file.not.found", filename)
    }
  }

  override fun closeFileByNumber(number: Int, projectId: String?) {
    val project = resolveProject(projectId) ?: return
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

  override fun saveFile(projectId: String?, filePath: String?, saveAll: Boolean) {
    val project = resolveProject(projectId) ?: return
    val editor = filePath?.let { findEditorByFilePath(project, it) } ?: return
    val vimEditor = editor.vim
    val context = buildContext(project, editor)
    saveFile(vimEditor, context, saveAll)
  }

  override fun selectPreviousTab(projectId: String?): Boolean {
    val project = resolveProject(projectId) ?: return false
    val vf = LastTabService.getInstance(project).lastTab
    if (vf != null && vf.isValid) {
      FileEditorManager.getInstance(project).openFile(vf, true)
      return true
    }
    return false
  }

  override fun buildFileInfoMessage(projectId: String?, filePath: String?, fullPath: Boolean): String? {
    val project = resolveProject(projectId) ?: return null
    val editor = filePath?.let { findEditorByFilePath(project, it) } ?: return null
    return buildFileInfoMessage(editor.vim, fullPath)
  }

  override fun selectEditor(projectId: String, documentPath: String, protocol: String): Boolean {
    val virtualFile = findVirtualFile(documentPath, protocol) ?: return false
    val project = findProjectById(projectId) ?: return false
    val editor = selectEditor(project, virtualFile)
    return editor != null
  }

  override fun getProjectId(): String {
    val project = ProjectManager.getInstance().openProjects.firstOrNull()
      ?: error("No open projects on backend")
    return getProjectId(project)
  }

  override fun getProjectIdForProject(project: Any): String {
    require(project is Project)
    return getProjectId(project)
  }

  // ======================== Internal methods (for FileRemoteApiImpl) ========================

  fun findFile(filename: String, project: Project): VirtualFile? {
    var found: VirtualFile?
    if (filename.startsWith("~/") || filename.startsWith("~\\")) {
      val relativePath = filename.substring(2)
      val dir = System.getProperty("user.home")
      if (logger.isDebugEnabled) {
        logger.debug("home dir file")
        logger.debug("looking for $relativePath in $dir")
      }
      found = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(Path(dir, relativePath))
    } else {
      found = VirtualFileManager.getInstance().findFileByNioPath(Path(filename))

      if (found == null) {
        found = findByNameInContentRoots(filename, project)
        if (found == null) {
          found = findByNameInProject(filename, project)
        }
      }
    }

    return found
  }

  /**
   * Saves file(s) based on the [saveAll] flag.
   * Used by [FileRemoteApiImpl] which reconstructs VimEditor/ExecutionContext from RPC params.
   */
  fun saveFile(editor: VimEditor, context: IjEditorExecutionContext, saveAll: Boolean) {
    val action = if (saveAll) {
      injector.nativeActionManager.saveAll
    } else {
      injector.nativeActionManager.saveCurrent
    }
    action.execute(editor, context)
  }

  /**
   * Builds the `:file` / Ctrl-G message string for the given editor.
   * Used by [FileRemoteApiImpl] for split-mode RPC.
   */
  fun buildFileInfoMessage(vimEditor: VimEditor, fullPath: Boolean): String {
    val msg = StringBuilder()
    val vf = vimEditor.getVirtualFile()
    if (vf != null) {
      msg.append('"')
      if (fullPath) {
        msg.append(vf.path)
      } else {
        val project = ProjectManager.getInstance().openProjects
          .firstOrNull { getProjectId(it) == vimEditor.projectId }
        if (project != null) {
          val virtualFile = VirtualFileManager.getInstance().getFileSystem(vf.protocol)?.findFileByPath(vf.path)
          val root = virtualFile?.let { ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(it) }
          if (root != null) {
            msg.append(vf.path.substring(root.path.length + 1))
          } else {
            msg.append(vf.path)
          }
        }
      }
      msg.append("\" ")
    } else {
      msg.append("\"[No File]\" ")
    }

    if (!vimEditor.isDocumentWritable()) {
      msg.append("[RO] ")
    } else if (vimEditor.hasUnsavedChanges()) {
      msg.append("[+] ")
    }

    val caret = vimEditor.currentCaret()
    val bufferPosition = caret.getBufferPosition()
    val lline = bufferPosition.line
    val total = vimEditor.lineCount()
    val pct = (lline.toFloat() / total.toFloat() * 100f + 0.5).toInt()

    msg.append("line ").append(lline + 1).append(" of ").append(total)
    msg.append(" --").append(pct).append("%-- ")

    val col = bufferPosition.column

    msg.append("col ").append(col + 1)

    return msg.toString()
  }

  fun selectEditor(project: Project, file: VirtualFile): Editor? {
    val fMgr = FileEditorManager.getInstance(project)
    val feditors = fMgr.openFile(file, true)
    if (feditors.size > 0) {
      if (feditors[0] is TextEditor) {
        val editor = (feditors[0] as TextEditor).editor
        if (!editor.isDisposed) {
          return editor
        }
      }
    }

    return null
  }

  fun getProjectId(project: Project): String {
    return project.name + "-" + project.locationHash
  }

  // ======================== Private helpers ========================

  private fun resolveProject(projectId: String?): Project? {
    val projects = ProjectManager.getInstance().openProjects
    if (projectId == null) return projects.firstOrNull()
    return projects.firstOrNull { getProjectId(it) == projectId }
      ?: projects.firstOrNull()
  }

  internal fun buildContext(project: Project, editor: Editor?): IjEditorExecutionContext {
    val dataContext = SimpleDataContext.builder()
      .add(PlatformDataKeys.PROJECT, project)
      .apply { if (editor != null) add(CommonDataKeys.EDITOR, editor) }
      .build()
    return IjEditorExecutionContext(dataContext)
  }

  private fun findByNameInContentRoots(filename: String, project: Project): VirtualFile? {
    var found: VirtualFile? = null
    val prm = ProjectRootManager.getInstance(project)
    val roots = prm.contentRoots
    for (i in roots.indices) {
      if (logger.isDebugEnabled) {
        logger.debug("root[" + i + "] = " + roots[i].path)
      }
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
    if (!names.isEmpty()) {
      return names.stream().findFirst().get()
    }
    return null
  }

  companion object {
    private val logger = Logger.getInstance(
      FileBackendServiceImpl::class.java.name
    )
  }
}
