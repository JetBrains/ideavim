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
import com.intellij.diff.impl.DiffEditorViewer
import com.intellij.diff.impl.DiffRequestProcessor
import com.intellij.diff.impl.DiffRequestProcessorListener
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.diff.util.Side
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import java.lang.reflect.Method
import java.util.UUID

/**
 * A diff open in an editor tab, as far as jumps need it: which file it shows, which other files it can show, and how
 * to get back to one of them
 *
 * A tab can show the diffs of many files, one at a time, see [DiffTabFiles].
 */
internal class DiffTab private constructor(
  private val file: VirtualFile,
  private val processor: DiffRequestProcessor,
) {
  /** Whether this is the tab of [tabFile], the file the tab is opened for */
  fun isTabOf(tabFile: VirtualFile): Boolean = file == tabFile

  /** Tells the tab apart from the other diff tabs while it is open */
  val id: String
    get() = file.putUserDataIfAbsent(ID_KEY, UUID.randomUUID().toString())

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
    FileEditorManager.getInstance(processor.project ?: return).openFile(file, true)
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
    /**
     * The file the side shows, if it shows a file at all. The text of an older revision has a file of its own too, for
     * highlighting, but that one only lives in memory
     */
    val fileOfDocument: VirtualFile? =
      FileDocumentManager.getInstance().getFile(content.document)?.takeIf { it !is LightVirtualFile }

    /** The file of the side. A side showing an older revision still knows the file, for highlighting it */
    val file: VirtualFile? = fileOfDocument ?: content.highlightFile
  }

  companion object {
    private val ID_KEY = Key.create<String>("IdeaVim.DiffTabId")

    fun allOpen(project: Project): List<DiffTab> {
      return FileEditorManager.getInstance(project).allEditors.mapNotNull { fileEditor ->
        val processor = diffViewerOf(fileEditor) as? DiffRequestProcessor
        processor?.let { DiffTab(fileEditor.file, it) }
      }
    }

    private fun diffViewerOf(fileEditor: FileEditor): DiffEditorViewer? {
      if (fileEditor is DiffEditorViewerFileEditor) return fileEditor.editorViewer
      val getProcessor = splitModeProcessorGetters.get(fileEditor.javaClass) ?: return null
      return try {
        getProcessor.invoke(fileEditor) as? DiffEditorViewer
      } catch (_: ReflectiveOperationException) {
        null
      }
    }

    /**
     * The getter of the diff viewer in a diff tab of split mode, looked up once per class of file editor
     *
     * On the backend, split mode keeps a diff tab in its own file editor, `DiffRequestDirectTransferEditor`, which has
     * the viewer only in its `processor` property. There is no public API for it.
     */
    private val splitModeProcessorGetters = object : ClassValue<Method?>() {
      override fun computeValue(type: Class<*>): Method? {
        val getter = type.methods.find { it.name == "getProcessor" && it.parameterCount == 0 } ?: return null
        if (!DiffEditorViewer::class.java.isAssignableFrom(getter.returnType)) return null
        return getter.apply { trySetAccessible() }
      }
    }
  }
}
