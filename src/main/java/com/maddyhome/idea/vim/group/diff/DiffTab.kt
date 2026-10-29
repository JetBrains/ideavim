/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.diff

import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.editor.DiffEditorViewerFileEditor
import com.intellij.diff.impl.DiffRequestProcessor
import com.intellij.diff.impl.DiffRequestProcessorListener
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.diff.util.Side
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.util.UUID

/**
 * A diff open in an editor tab, as far as jumps need it: which file it shows, which other files it can show, and how
 * to get back to one of them
 *
 * A tab can show the diffs of many files, one at a time, see [DiffTabFiles].
 */
internal class DiffTab private constructor(
  private val fileEditor: DiffEditorViewerFileEditor,
  private val processor: DiffRequestProcessor,
) {
  /** Tells the tab apart from the other diff tabs while it is open */
  val id: String
    get() = fileEditor.file.putUserDataIfAbsent(ID_KEY, UUID.randomUUID().toString())

  val view: DiffView?
    get() = DiffView.of(processor.activeViewer)

  /** The file the shown diff is about, preferring the newer side. Null if neither side comes from a file */
  val shownFile: VirtualFile?
    get() = sides().asReversed().firstNotNullOfOrNull { it.file }

  /** The file shown on [side], or null if the side shows other text, such as an older revision */
  fun fileOn(side: Side): VirtualFile? = sides().getOrNull(side.index)?.fileOfDocument

  /** Whether the tab shows the diff of [path] right now */
  fun shows(path: String): Boolean = shownFile?.path == path

  /** Whether the tab shows the diff of [path], or can switch to it */
  fun canShow(path: String): Boolean = shows(path) || files?.canShow(path) == true

  /**
   * Selects the tab, makes it show the diff of [path] and runs [action] with its view once the view shows the text
   *
   * Switching to another file loads its diff in the background, so [action] may run later.
   */
  fun show(path: String, action: (DiffView) -> Unit) {
    FileEditorManager.getInstance(processor.project ?: return).openFile(fileEditor.file, true)
    val actionOnceReady = { view: DiffView -> view.whenReady { action(view) } }
    if (shows(path)) {
      view?.let(actionOnceReady)
      return
    }
    val files = files ?: return
    onceShowing(path, actionOnceReady)
    files.show(path)
  }

  private val files: DiffTabFiles?
    get() = DiffTabFiles.of(processor)

  private fun onceShowing(path: String, action: (DiffView) -> Unit) {
    val listening = Disposer.newDisposable("IdeaVim diff jump")
    Disposer.register(processor, listening)
    processor.addListener(DiffRequestProcessorListener {
      val view = view ?: return@DiffRequestProcessorListener
      if (!shows(path)) return@DiffRequestProcessorListener
      Disposer.dispose(listening)
      action(view)
    }, listening)
  }

  private fun sides(): List<DiffSideContent> {
    val request = processor.activeRequest as? ContentDiffRequest ?: return emptyList()
    val contents = request.contents.filterIsInstance<DocumentContent>()
    if (contents.size != 2) return emptyList()
    return contents.map { DiffSideContent(it) }
  }

  private class DiffSideContent(content: DocumentContent) {
    val fileOfDocument: VirtualFile? = FileDocumentManager.getInstance().getFile(content.document)

    /** The file of the side. A side showing an older revision still knows the file, for highlighting it */
    val file: VirtualFile? = fileOfDocument ?: content.highlightFile
  }

  companion object {
    private val ID_KEY = Key.create<String>("IdeaVim.DiffTabId")

    fun allOpen(project: Project): List<DiffTab> {
      return FileEditorManager.getInstance(project).allEditors
        .filterIsInstance<DiffEditorViewerFileEditor>()
        .mapNotNull { fileEditor ->
          val processor = fileEditor.editorViewer as? DiffRequestProcessor
          processor?.let { DiffTab(fileEditor, it) }
        }
    }
  }
}
