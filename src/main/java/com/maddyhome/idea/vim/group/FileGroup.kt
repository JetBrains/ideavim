/*
 * Copyright 2003-2023 The IdeaVim authors
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
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
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
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimFileBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.LastTabService.Companion.getInstance
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper.message
import com.maddyhome.idea.vim.helper.countWords
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.execute
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.state.VimStateMachine.Companion.getInstance
import com.maddyhome.idea.vim.state.mode.Mode.VISUAL
import java.io.File
import java.util.*

class FileGroup : VimFileBase() {
  override fun openFile(filename: String, context: ExecutionContext): Boolean {
    if (logger.isDebugEnabled) {
      logger.debug("openFile($filename)")
    }
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context)
      ?: return false // API change - don't merge

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
        fem.openFile(found, true)

        return true
      } else {
        // There was no type and user didn't pick one. Don't open the file
        // Return true here because we found the file but the user canceled by not picking a type.
        return true
      }
    } else {
      VimPlugin.showMessage(message("unable.to.find.0", filename))

      return false
    }
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
      found = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(dir, relativePath))
    } else {
      found = LocalFileSystem.getInstance().findFileByIoFile(File(filename))

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
   */
  override fun saveFile(context: ExecutionContext) {
    val action = if (injector.globalIjOptions().ideawrite.contains(IjOptionConstants.ideawrite_all)) {
      injector.nativeActionManager.saveAll
    } else {
      injector.nativeActionManager.saveCurrent
    }
    action.execute(context)
  }

  /**
   * Saves all files in the project.
   */
  override fun saveFiles(context: ExecutionContext) {
    injector.nativeActionManager.saveAll.execute(context)
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
  override fun selectPreviousTab(context: ExecutionContext) {
    val project = PlatformDataKeys.PROJECT.getData((context.context as DataContext)) ?: return
    val vf = getInstance(project).lastTab
    if (vf != null && vf.isValid) {
      FileEditorManager.getInstance(project).openFile(vf, true)
    } else {
      VimPlugin.indicateError()
    }
  }

  /**
   * Returns the previous tab.
   */
  fun getPreviousTab(context: DataContext): VirtualFile? {
    val project = PlatformDataKeys.PROJECT.getData(context) ?: return null
    val vf = getInstance(project).lastTab
    if (vf != null && vf.isValid) {
      return vf
    }
    return null
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

  override fun displayLocationInfo(vimEditor: VimEditor) {
    val editor = (vimEditor as IjVimEditor).editor
    val msg = StringBuilder()
    val doc = editor.document

    if (injector.vimState.mode !is VISUAL) {
      val lp = editor.caretModel.logicalPosition
      val col = editor.caretModel.offset - doc.getLineStartOffset(lp.line)
      var endoff = doc.getLineEndOffset(lp.line)
      if (endoff < editor.fileSize && doc.charsSequence[endoff] == '\n') {
        endoff--
      }
      val ecol = endoff - doc.getLineStartOffset(lp.line)
      val elp = editor.offsetToLogicalPosition(endoff)

      msg.append("Col ").append(col + 1)
      if (col != lp.column) {
        msg.append("-").append(lp.column + 1)
      }

      msg.append(" of ").append(ecol + 1)
      if (ecol != elp.column) {
        msg.append("-").append(elp.column + 1)
      }

      val lline = editor.caretModel.logicalPosition.line
      val total = IjVimEditor(editor).lineCount()

      msg.append("; Line ").append(lline + 1).append(" of ").append(total)

      val cp = countWords(vimEditor)

      msg.append("; Word ").append(cp.position).append(" of ").append(cp.count)

      val offset = editor.caretModel.offset
      val size = editor.fileSize

      msg.append("; Character ").append(offset + 1).append(" of ").append(size)
    } else {
      msg.append("Selected ")

      val vr = TextRange(
        editor.selectionModel.blockSelectionStarts,
        editor.selectionModel.blockSelectionEnds
      )
      vr.normalize()

      val lines: Int
      var cp = countWords(vimEditor)
      val words = cp.count
      var word = 0
      if (vr.isMultiple) {
        lines = vr.size()
        val cols = vr.maxLength

        msg.append(cols).append(" Cols; ")

        for (i in 0 until vr.size()) {
          cp = countWords(vimEditor, vr.startOffsets[i], (vr.endOffsets[i] - 1).toLong())
          word += cp.count
        }
      } else {
        val slp = editor.offsetToLogicalPosition(vr.startOffset)
        val elp = editor.offsetToLogicalPosition(vr.endOffset)

        lines = elp.line - slp.line + 1

        cp = countWords(vimEditor, vr.startOffset, (vr.endOffset - 1).toLong())
        word = cp.count
      }

      val total = IjVimEditor(editor).lineCount()

      msg.append(lines).append(" of ").append(total).append(" Lines")

      msg.append("; ").append(word).append(" of ").append(words).append(" Words")

      val chars = vr.selectionCount
      val size = editor.fileSize

      msg.append("; ").append(chars).append(" of ").append(size).append(" Characters")
    }

    VimPlugin.showMessage(msg.toString())
  }

  override fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean) {
    val editor = (vimEditor as IjVimEditor).editor
    val msg = StringBuilder()
    val vf = EditorHelper.getVirtualFile(editor)
    if (vf != null) {
      msg.append('"')
      if (fullPath) {
        msg.append(vf.path)
      } else {
        val project = editor.project
        if (project != null) {
          val root = ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(vf)
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

    val doc = editor.document
    if (!doc.isWritable) {
      msg.append("[RO] ")
    } else if (FileDocumentManager.getInstance().isDocumentUnsaved(doc)) {
      msg.append("[+] ")
    }

    val lline = editor.caretModel.logicalPosition.line
    val total = IjVimEditor(editor).lineCount()
    val pct = (lline.toFloat() / total.toFloat() * 100f + 0.5).toInt()

    msg.append("line ").append(lline + 1).append(" of ").append(total)
    msg.append(" --").append(pct).append("%-- ")

    val lp = editor.caretModel.logicalPosition
    val col = editor.caretModel.offset - doc.getLineStartOffset(lline)

    msg.append("col ").append(col + 1)
    if (col != lp.column) {
      msg.append("-").append(lp.column + 1)
    }

    VimPlugin.showMessage(msg.toString())
  }

  override fun selectEditor(projectId: String, documentPath: String, protocol: String?): VimEditor? {
    val fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol) ?: return null
    val virtualFile = fileSystem.findFileByPath(documentPath) ?: return null

    val project = Arrays.stream(ProjectManager.getInstance().openProjects)
      .filter { p: Project? -> injector.file.getProjectId(p!!) == projectId }
      .findFirst().orElseThrow()

    val editor = selectEditor(project, virtualFile) ?: return null
    return IjVimEditor(editor)
  }

  override fun getProjectId(project: Any): String {
    require(project is Project)
    return project.name + "-" + project.locationHash
  }

  companion object {
    private fun findByNameInProject(filename: String, project: Project): VirtualFile? {
      val projectScope = ProjectScope.getProjectScope(project)
      val names = FilenameIndex.getVirtualFilesByName(filename, projectScope)
      if (!names.isEmpty()) {
        return names.stream().findFirst().get()
      }
      return null
    }

    private val logger = Logger.getInstance(
      FileGroup::class.java.name
    )

    /**
     * Respond to editor tab selection and remember the last used tab
     */
    fun fileEditorManagerSelectionChangedCallback(event: FileEditorManagerEvent) {
      if (event.oldFile != null) {
        getInstance(event.manager.project).lastTab = event.oldFile
      }
    }
  }
}
