/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
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
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimFileBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.execute
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim
import kotlin.io.path.Path

class FileGroup : VimFileBase() {
  override fun openFile(filename: String, context: ExecutionContext, focusEditor: Boolean): String? {
    if (logger.isDebugEnabled) {
      logger.debug("openFile($filename)")
    }
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context)
      ?: return "No project" // API change - don't merge

    val found = findFile(filename, project)

    if (found != null) {
      if (logger.isDebugEnabled) {
        logger.debug("found file: $found")
      }
      // Can't open a file unless it has a known file type. The next call will return the known type.
      // If unknown, IDEA will prompt the user to pick a type.
      val type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(found, project)

      if (type != null) {
        val fem = FileEditorManager.getInstance(project)
        fem.openFile(found, focusEditor)
      }

      // Return null (success) whether we opened the file or user cancelled the type picker
      return null
    } else {
      return EngineMessageHelper.message("message.open.file.not.found", filename)
    }
  }

  override fun findFile(filename: String, context: ExecutionContext): String? {
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context)
      ?: return null
    return findFile(filename, project)?.path
  }

  fun findFile(filename: String, project: Project): VirtualFile? {
    var found: VirtualFile?
    // Vim supports both ~/ and ~\ (tested on Mac and Windows). On Windows, it supports forward- and back-slashes, but
    // it only supports forward slash on Unix (tested on Mac)
    // VFS works with both directory separators (tested on Mac and Windows)
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

  /**
   * Closes the current editor.
   */
  override fun closeFile(editor: VimEditor, context: ExecutionContext) {
    val project = PlatformDataKeys.PROJECT.getData((context.context as DataContext))
    if (project != null) {
      val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
      val window = fileEditorManager.currentWindow
      val virtualFile = fileEditorManager.currentFile

      if (virtualFile != null && window != null) {
        // During the work on VIM-2912 I've changed the close function to this one.
        //   However, the function with manager seems to work weirdly and it causes VIM-2953
        //window.getManager().closeFile(virtualFile, true, false);
        window.closeFile(virtualFile)

        // Get focus after closing tab
        window.requestFocus(true)
        if (!ApplicationManager.getApplication().isUnitTestMode) {
          // This thing doesn't have an implementation in test mode
          EditorsSplitters.focusDefaultComponentInSplittersIfPresent(project)
        }
      } else {
        // Split mode fallback: no focused window, close by editor's virtual file
        val vimVf = editor.getVirtualFile()
        val vf = vimVf?.let {
          VirtualFileManager.getInstance().getFileSystem(it.protocol)?.findFileByPath(it.path)
        }
        if (vf != null) {
          fileEditorManager.closeFile(vf)
        }
      }
    }
  }

  /**
   * Closes editor.
   */
  override fun closeFile(number: Int, context: ExecutionContext) {
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context) ?: return
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    val window = fileEditorManager.currentWindow
    val editors = fileEditorManager.openFiles
    if (window != null) {
      if (number >= 0 && number < editors.size) {
        fileEditorManager.closeFile(editors[number], window)
      }
    }
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      // This thing doesn't have an implementation in test mode
      EditorsSplitters.focusDefaultComponentInSplittersIfPresent(project)
    }
  }

  /**
   * Saves specific file in the project.
   *
   * Note: reads `ideawrite` from `injector` — this method is only called in monolith mode
   * where `injector` is fully initialized. In split mode, [FileGroupSplitClient] reads the
   * option on the frontend and passes `saveAll` to [FileRemoteApiImpl] via RPC.
   */
  override fun saveFile(editor: VimEditor, context: ExecutionContext) {
    val saveAll = injector.globalIjOptions().ideawrite.contains(IjOptionConstants.ideawrite_all)
    saveFile(editor, context, saveAll)
  }

  /**
   * Saves file(s) based on the [saveAll] flag.
   * Entry point for [FileRemoteApiImpl] where the option is already resolved by the frontend.
   */
  fun saveFile(editor: VimEditor, context: ExecutionContext, saveAll: Boolean) {
    val action = if (saveAll) {
      injector.nativeActionManager.saveAll
    } else {
      injector.nativeActionManager.saveCurrent
    }
    action.execute(editor, context)
  }

  /**
   * Saves all files in the project.
   */
  override fun saveFiles(editor: VimEditor, context: ExecutionContext) {
    injector.nativeActionManager.saveAll.execute(editor, context)
  }

  /**
   * Selects then next or previous editor.
   */
  override fun selectFile(count: Int, context: ExecutionContext): Boolean {
    var count = count
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context) ?: return false
    val fem = FileEditorManager.getInstance(project) // API change - don't merge
    val editors = fem.openFiles
    if (count == 99) {
      count = editors.size - 1
    }
    if (count < 0 || count >= editors.size) {
      return false
    }

    fem.openFile(editors[count], true)

    return true
  }

  /**
   * Selects then next or previous editor.
   */
  override fun selectNextFile(count: Int, context: ExecutionContext) {
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context) ?: return
    val fem = FileEditorManager.getInstance(project) // API change - don't merge
    val editors = fem.openFiles
    val current = fem.selectedFiles[0]
    for (i in editors.indices) {
      if (editors[i] == current) {
        val pos = (i + (count % editors.size) + editors.size) % editors.size

        fem.openFile(editors[pos], true)
      }
    }
  }

  /**
   * Selects previous editor tab.
   */
  override fun selectPreviousTab(context: ExecutionContext): Boolean {
    val project = PlatformDataKeys.PROJECT.getData((context.context as DataContext)) ?: return false
    val vf = LastTabService.getInstance(project).lastTab
    if (vf != null && vf.isValid) {
      FileEditorManager.getInstance(project).openFile(vf, true)
      return true
    }
    return false
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

  override fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean): String {
    return buildFileInfoMessage(vimEditor, fullPath)
  }

  /**
   * Builds the `:file` / Ctrl-G message string for the given editor.
   * Used by [displayFileInfo] (monolith) and [FileRemoteApiImpl] (split mode RPC).
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
          .firstOrNull { injector.file.getProjectId(it) == vimEditor.projectId }
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


  override fun selectEditor(projectId: String, documentPath: String, protocol: String): VimEditor? {
    // Try the requested protocol first, then fall back to common protocols.
    // In split mode the thin client may pass "cwm" which doesn't exist on the backend.
    val virtualFile = VirtualFileManager.getInstance().getFileSystem(protocol)?.findFileByPath(documentPath)
      ?: VirtualFileManager.getInstance().getFileSystem("file")?.findFileByPath(documentPath)
      ?: VirtualFileManager.getInstance().getFileSystem("jar")?.findFileByPath(documentPath)
      ?: return null

    val project = findProjectById(projectId) ?: return null

    val editor = selectEditor(project, virtualFile) ?: return null
    return editor.vim
  }

  override fun getProjectId(project: Any): String {
    require(project is Project)
    return project.name + "-" + project.locationHash
  }

  companion object {
    private val logger = Logger.getInstance(
      FileGroup::class.java.name
    )
  }
}
