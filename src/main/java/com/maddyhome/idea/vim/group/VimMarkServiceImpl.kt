/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.ide.bookmark.Bookmark
import com.intellij.ide.bookmark.BookmarkGroup
import com.intellij.ide.bookmark.BookmarksListener
import com.intellij.ide.bookmark.BookmarksManager
import com.intellij.ide.bookmark.LineBookmark
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.VimMarkServiceBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.SystemMarks.Companion.createOrGetSystemMark
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.localEditors
import com.maddyhome.idea.vim.mark.IntellijMark
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.mark.VimMark.Companion.create
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.globalIjOptions
import org.jdom.Element
import java.util.*

// todo save jumps after IDE close
// todo sync vim jumps with ide jumps

// todo exception after moving to global mark after deleting it via IDE (impossible to receive markChar)
@State(name = "VimMarksSettings", storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)])
internal class VimMarkServiceImpl : VimMarkServiceBase(), PersistentStateComponent<Element?> {
  private fun createOrGetSystemMark(ch: Char, line: Int, col: Int, editor: VimEditor): Mark? {
    val ijEditor = (editor as IjVimEditor).editor
    val systemMark = createOrGetSystemMark(ch, line, ijEditor) ?: return null
    return IntellijMark(systemMark, col, ijEditor.project)
  }

  private fun saveData(element: Element) {
    val globalMarksElement = Element("globalmarks")
    if (!injector.globalIjOptions().ideamarks) {
      for (mark in globalMarks.values) {
        val markElem = Element("mark")
        markElem.setAttribute("key", mark.key.toString())
        markElem.setAttribute("line", mark.line.toString())
        markElem.setAttribute("column", mark.col.toString())
        markElem.setAttribute("filename", StringUtil.notNullize(mark.filepath))
        markElem.setAttribute("protocol", StringUtil.notNullize(mark.protocol, "file"))
        globalMarksElement.addContent(markElem)
        if (logger.isDebugEnabled) {
          logger.debug("saved mark = $mark")
        }
      }
    }
    element.addContent(globalMarksElement)
    val localMarksElement = Element("localmarks")
    var files: List<LocalMarks<Char, Mark>> = filepathToLocalMarks.values.sortedWith(Comparator.comparing(LocalMarks<Char, Mark>::myTimestamp))
    if (files.size > SAVE_MARK_COUNT) {
      files = files.subList(files.size - SAVE_MARK_COUNT, files.size)
    }
    for (file in filepathToLocalMarks.keys) {
      val marks: LocalMarks<Char, Mark>? = filepathToLocalMarks[file]
      if (!files.contains(marks)) {
        continue
      }
      if (marks!!.size > 0) {
        val fileMarkElem = Element("file")
        fileMarkElem.setAttribute("name", file)
        fileMarkElem.setAttribute("timestamp", java.lang.Long.toString(marks.myTimestamp.time))
        for (mark in marks.values) {
          if (!Character.isUpperCase(mark.key) && injector.markService.isValidMark(mark.key, VimMarkService.Operation.SAVE, true)) {
            val markElem = Element("mark")
            markElem.setAttribute("key", mark.key.toString())
            markElem.setAttribute("line", mark.line.toString())
            markElem.setAttribute("column", mark.col.toString())
            fileMarkElem.addContent(markElem)
          }
        }
        localMarksElement.addContent(fileMarkElem)
      }
    }
    element.addContent(localMarksElement)
  }

  private fun readData(element: Element) {
    // We need to keep the filename for now and create the virtual file later. Any attempt to call
    // LocalFileSystem.getInstance().findFileByPath() results in the following error:
    // Read access is allowed from event dispatch thread or inside read-action only
    // (see com.intellij.openapi.application.Application.runReadAction())
    val marksElem = element.getChild("globalmarks")
    if (marksElem != null && !injector.globalIjOptions().ideamarks) {
      val markList = marksElem.getChildren("mark")
      for (aMarkList in markList) {
        val mark: Mark? = create(
          aMarkList.getAttributeValue("key")[0],
          aMarkList.getAttributeValue("line").toInt(),
          aMarkList.getAttributeValue("column").toInt(),
          aMarkList.getAttributeValue("filename"),
          aMarkList.getAttributeValue("protocol"),
        )
        if (mark != null) {
          globalMarks[mark.key] = mark
          val lmarks: HashMap<Char, Mark> = getLocalMarks(mark.filepath)
          lmarks[mark.key] = mark
        }
      }
    }
    if (logger.isDebugEnabled) {
      logger.debug("globalMarks=$globalMarks")
    }
    val fileMarksElem = element.getChild("localmarks")
    if (fileMarksElem != null) {
      val fileList = fileMarksElem.getChildren("file")
      for (aFileList in fileList) {
        val filename = aFileList.getAttributeValue("name")
        val timestamp = Date()
        try {
          val date = aFileList.getAttributeValue("timestamp").toLong()
          timestamp.time = date
        } catch (e: NumberFormatException) {
          // ignore
        }
        val fmarks = getLocalMarks(filename)
        val markList = aFileList.getChildren("mark")
        for (aMarkList in markList) {
          val mark: Mark? = create(
            aMarkList.getAttributeValue("key")[0],
            aMarkList.getAttributeValue("line").toInt(),
            aMarkList.getAttributeValue("column").toInt(),
            filename,
            aMarkList.getAttributeValue("protocol"),
          )
          if (mark != null) fmarks[mark.key] = mark
        }
        fmarks.setTimestamp(timestamp)
      }
    }
    if (logger.isDebugEnabled) {
      logger.debug("localMarks=$filepathToLocalMarks")
    }
  }

  override fun getState(): Element {
    val element = Element("marks")
    saveData(element)
    return element
  }

  override fun loadState(state: Element) {
    readData(state)
  }

  override fun createGlobalMark(editor: VimEditor, char: Char, offset: Int): Mark? {
    if (!injector.globalIjOptions().ideamarks) {
      return super.createGlobalMark(editor, char, offset)
    }
    val lp = editor.offsetToBufferPosition(offset)
    val line = lp.line
    val col = lp.column
    return createOrGetSystemMark(char, line, col, editor)
  }

  @Deprecated("Please use removeMark with other signature")
  override fun removeMark(ch: Char, mark: Mark) {
    if (ch.isGlobalMark()) {
      removeGlobalMark(ch)
    } else if (ch.isLocalMark()) {
      getLocalMarks(mark.filepath).remove(ch)
    }
  }

  override fun removeGlobalMark(char: Char) {
    val mark = getGlobalMark(char)
    if (mark is IntellijMark) {
      mark.clear()
    }
    super.removeGlobalMark(char)
  }

  /**
   * This class is used to listen to editor document changes
   */
  object MarkUpdater : DocumentListener {
    /**
     * This event indicates that a document is about to be changed. We use this event to update all the
     * editor's marks if text is about to be deleted.
     *
     * @param event The change event
     */
    override fun beforeDocumentChange(event: DocumentEvent) {
      if (!VimPlugin.isEnabled()) return
      if (logger.isDebugEnabled) logger.debug("MarkUpdater before, event = $event")
      if (event.oldLength == 0) return
      val doc = event.document
      val anEditor = getAnEditor(doc) ?: return
      injector.markService
        .updateMarksFromDelete(IjVimEditor(anEditor), event.offset, event.oldLength)
    }

    /**
     * This event indicates that a document was just changed. We use this event to update all the editor's
     * marks if text was just added.
     *
     * @param event The change event
     */
    override fun documentChanged(event: DocumentEvent) {
      if (!VimPlugin.isEnabled()) return
      if (logger.isDebugEnabled) logger.debug("MarkUpdater after, event = $event")
      if (event.newLength == 0 || event.newLength == 1 && event.newFragment[0] != '\n') return
      val doc = event.document
      val anEditor = getAnEditor(doc) ?: return
      injector.markService
        .updateMarksFromInsert(IjVimEditor(anEditor), event.offset, event.newLength)
    }

    private fun getAnEditor(doc: Document): Editor? {
      val editors = localEditors(doc)
      return if (editors.size > 0) {
        editors[0]
      } else {
        null
      }
    }
  }

  class VimBookmarksListener(private val myProject: Project) : BookmarksListener {
    override fun bookmarkAdded(group: BookmarkGroup, bookmark: Bookmark) {
      if (!VimPlugin.isEnabled()) return
      if (!injector.globalIjOptions().ideamarks) {
        return
      }
      if (bookmark !is LineBookmark) return
      val bookmarksManager = BookmarksManager.getInstance(myProject) ?: return
      val type = bookmarksManager.getType(bookmark) ?: return
      val mnemonic = type.mnemonic
      if ((VimMarkService.UPPERCASE_MARKS + VimMarkService.NUMBERED_MARKS).indexOf(mnemonic) == -1) return
      createVimMark(bookmark)
    }

    override fun bookmarkRemoved(group: BookmarkGroup, bookmark: Bookmark) {
      if (!VimPlugin.isEnabled()) return
      if (!injector.globalIjOptions().ideamarks) {
        return
      }
      if (bookmark !is LineBookmark) return
      val bookmarksManager = BookmarksManager.getInstance(myProject) ?: return
      val type = bookmarksManager.getType(bookmark) ?: return
      val ch = type.mnemonic
      if ((VimMarkService.UPPERCASE_MARKS + VimMarkService.NUMBERED_MARKS).indexOf(ch) != -1) {
        injector.markService.removeGlobalMark(ch)
      }
    }

    private fun createVimMark(b: LineBookmark) {
      var col = 0
      val editor = EditorHelper.getEditor(b.file)
      if (editor != null) col = editor.caretModel.currentCaret.logicalPosition.column
      val mark = IntellijMark(b, col, myProject)
      injector.markService.setGlobalMark(mark)
    }
  }

  /**
   * COMPATIBILITY-LAYER: Method added
   * Please see: [doc](https://jb.gg/zo8n0r)
   *
   */
  @Deprecated("Please use method with VimEditor")
  fun saveJumpLocation(editor: Editor?) {
    injector.jumpService.saveJumpLocation(IjVimEditor(editor!!))
  }

  companion object {
    private const val SAVE_MARK_COUNT = 20
    private val logger = Logger.getInstance(
      VimMarkServiceImpl::class.java.name,
    )
  }
}
