/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.VimMarkServiceBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.bookmark.BookmarkBackendService
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.mark.VimMark
import com.maddyhome.idea.vim.mark.VimMark.Companion.create
import com.maddyhome.idea.vim.newapi.globalIjOptions
import org.jdom.Element
import java.util.*

// todo save jumps after IDE close
// todo sync vim jumps with ide jumps

// todo exception after moving to global mark after deleting it via IDE (impossible to receive markChar)
@State(
  name = "VimMarksSettings",
  storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)]
)
internal class VimMarkServiceImpl : VimMarkServiceBase(), PersistentStateComponent<Element?> {

  private val bookmarkBackend get() = BookmarkBackendService.getInstance()

  override fun createGlobalMark(editor: VimEditor, char: Char, offset: Int): Mark? {
    if (!injector.globalIjOptions().ideamarks) {
      return super.createGlobalMark(editor, char, offset)
    }
    val lp = editor.offsetToBufferPosition(offset)
    val virtualFile = editor.getVirtualFile() ?: return super.createGlobalMark(editor, char, offset)
    val projectId = editor.projectId
    val info =
      bookmarkBackend.createOrGetSystemMark(char, lp.line, lp.column, virtualFile.path, projectId, virtualFile.protocol)
      ?: return super.createGlobalMark(editor, char, offset)
    return VimMark(info.key, info.line, lp.column, virtualFile.path, info.protocol)
  }

  override fun getGlobalMark(char: Char): Mark? {
    if (!char.isGlobalMark()) return null

    if (!injector.globalIjOptions().ideamarks) {
      return super.getGlobalMark(char)
    }

    // When ideamarks is on, the BookmarksManager is the source of truth.
    val info = bookmarkBackend.getBookmarkForMark(char)
    if (info != null) {
      val mark = VimMark(info.key, info.line, info.col, info.filepath, info.protocol)
      globalMarks[char] = mark
      return mark
    }

    // No IDE bookmark found — fall back to in-memory mark if available.
    // This handles cases where bookmark creation wasn't possible (e.g. temp files in tests)
    // but the mark was still stored in-memory by setGlobalMark.
    return super.getGlobalMark(char)
  }

  override fun getAllGlobalMarks(): Set<Mark> {
    if (!injector.globalIjOptions().ideamarks) {
      return super.getAllGlobalMarks()
    }

    // Update in-memory marks from IDE bookmarks (bookmarks are source of truth when they exist)
    val bookmarks = bookmarkBackend.getAllBookmarks()
    for (info in bookmarks) {
      globalMarks[info.key] = VimMark(info.key, info.line, info.col, info.filepath, info.protocol)
    }
    return globalMarks.values.toSet()
  }

  override fun removeGlobalMark(char: Char) {
    if (injector.globalIjOptions().ideamarks) {
      bookmarkBackend.removeBookmark(char)
    }
    super.removeGlobalMark(char)
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
    var files: List<LocalMarks<Char, Mark>> =
      filepathToLocalMarks.values.sortedWith(Comparator.comparing(LocalMarks<Char, Mark>::myTimestamp))
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
          if (!Character.isUpperCase(mark.key) && injector.markService.isValidMark(
              mark.key,
              VimMarkService.Operation.SAVE,
              true
            )
          ) {
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

  override fun loadLegacyState(element: Any) {
    loadState(element as Element)
  }

  companion object {
    private const val SAVE_MARK_COUNT = 20
    private val logger = Logger.getInstance(
      VimMarkServiceImpl::class.java.name,
    )
  }
}
