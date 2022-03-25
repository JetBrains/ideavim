package com.maddyhome.idea.vim.mark

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import java.util.*

abstract class VimMarkGroupBase : VimMarkGroup {
  @JvmField
  protected val fileMarks = HashMap<String, FileMarks<Char, Mark>>()
  @JvmField
  protected val globalMarks = HashMap<Char, Mark>()
  @JvmField
  protected val jumps: MutableList<Jump> = ArrayList<Jump>()
  @JvmField
  protected var jumpSpot = -1

  protected class FileMarks<K, V> : HashMap<K, V>() {
    var myTimestamp = Date()

    fun setTimestamp(timestamp: Date) {
      this.myTimestamp = timestamp
    }

    override fun put(key: K, value: V): V? {
      myTimestamp = Date()
      return super.put(key, value)
    }
  }

  companion object {
    const val SAVE_JUMP_COUNT = 100
  }

  override fun addJump(editor: VimEditor, reset: Boolean) {
    addJump(editor, editor.primaryCaret().offset.point, reset)
  }

  private fun addJump(editor: VimEditor, offset: Int, reset: Boolean) {
    val path = editor.getPath() ?: return

    val lp = editor.offsetToLogicalPosition(offset)
    val jump = Jump(lp.line, lp.column, path)
    val filename = jump.filepath

    for (i in jumps.indices) {
      val j = jumps[i]
      if (filename == j.filepath && j.logicalLine == jump.logicalLine) {
        jumps.removeAt(i)
        break
      }
    }

    jumps.add(jump)

    if (reset) {
      jumpSpot = -1
    } else {
      jumpSpot++
    }

    if (jumps.size > SAVE_JUMP_COUNT) {
      jumps.removeAt(0)
    }
  }

  /**
   * Gets the map of marks for the specified file
   *
   * @param filename The file to get the marks for
   * @return The map of marks. The keys are `Character`s of the mark names, the values are
   * `Mark`s.
   */
  protected fun getFileMarks(filename: String): FileMarks<Char, Mark> {
    return fileMarks.getOrPut(filename) { FileMarks() }
  }

  /**
   * Gets the requested mark for the editor
   *
   * @param editor The editor to get the mark for
   * @param ch     The desired mark
   * @return The requested mark if set, null if not set
   */
  override fun getMark(editor: VimEditor, ch: Char): Mark? {
    var myCh = ch
    var mark: Mark? = null
    if (myCh == '`') myCh = '\''

    // Make sure this is a valid mark
    if (VimMarkConstants.VALID_GET_MARKS.indexOf(myCh) < 0) return null

    val editorPath = editor.getPath()
    if ("{}".indexOf(myCh) >= 0 && editorPath != null) {
      var offset = injector.searchHelper.findNextParagraph(
        editor, editor.primaryCaret(), if (myCh == '{') -1 else 1,
        false
      )
      offset = injector.engineEditorHelper.normalizeOffset(editor, offset, false)
      val lp = editor.offsetToLogicalPosition(offset)
      mark = VimMark(myCh, lp.line, lp.column, editorPath, editor.extractProtocol())
    } else if ("()".indexOf(myCh) >= 0 && editorPath != null) {
      var offset = injector.searchHelper
        .findNextSentenceStart(editor, editor.primaryCaret(), if (myCh == '(') -1 else 1,
          countCurrent = false,
          requireAll = true
        )

      offset = injector.engineEditorHelper.normalizeOffset(editor, offset, false)
      val lp = editor.offsetToLogicalPosition(offset)
      mark = VimMark(myCh, lp.line, lp.column, editorPath, editor.extractProtocol())
    } else if (VimMarkConstants.FILE_MARKS.indexOf(myCh) >= 0) {
      var fmarks: FileMarks<Char, Mark>? = null
      if (editorPath != null) {
        fmarks = getFileMarks(editorPath)
      }

      if (fmarks != null) {
        mark = fmarks[myCh]
        if (mark != null && mark.isClear()) {
          fmarks.remove(myCh)
          mark = null
        }
      }
    } else if (VimMarkConstants.GLOBAL_MARKS.indexOf(myCh) >= 0) {
      mark = globalMarks[myCh]
      if (mark != null && mark.isClear()) {
        globalMarks.remove(myCh)
        mark = null
      }
    }// This is a mark from another file
    // If this is a file mark, get the mark from this file

    return mark
  }

  /**
   * Get the requested jump.
   *
   * @param count Postive for next jump (Ctrl-I), negative for previous jump (Ctrl-O).
   * @return The jump or null if out of range.
   */
  override fun getJump(count: Int): Jump? {
    val index = jumps.size - 1 - (jumpSpot - count)
    return if (index < 0 || index >= jumps.size) {
      null
    } else {
      jumpSpot -= count
      jumps[index]
    }
  }

  /**
   * Sets the specified mark to the specified location.
   *
   * @param editor  The editor the mark is associated with
   * @param ch      The mark to set
   * @param offset  The offset to set the mark to
   * @return true if able to set the mark, false if not
   */
  override fun setMark(editor: VimEditor, ch: Char, offset: Int): Boolean {
    var myCh = ch
    if (myCh == '`') myCh = '\''
    val lp = editor.offsetToLogicalPosition(offset)

    val path = editor.getPath() ?: return false

    // File specific marks get added to the file
    if (VimMarkConstants.FILE_MARKS.indexOf(myCh) >= 0) {
      val fmarks = getFileMarks(path)

      val mark = VimMark(myCh, lp.line, lp.column, path, editor.extractProtocol())
      fmarks[myCh] = mark
    } else if (VimMarkConstants.GLOBAL_MARKS.indexOf(myCh) >= 0) {
      val fmarks = getFileMarks(path)

      var mark = createSystemMark(myCh, lp.line, lp.column, editor)
      if (mark == null) {
        mark = VimMark(myCh, lp.line, lp.column, path, editor.extractProtocol())
      }
      fmarks[myCh] = mark
      val oldMark = globalMarks.put(myCh, mark)
      if (oldMark is VimMark) {
        oldMark.clear()
      }
    }// Global marks get set to both the file and the global list of marks

    return true
  }

  /**
   * Sets the specified mark to the caret position of the editor
   *
   * @param editor  The editor to get the current position from
   * @param ch      The mark set set
   * @return True if a valid, writable mark, false if not
   */
  override fun setMark(editor: VimEditor, ch: Char): Boolean {
    return VimMarkConstants.VALID_SET_MARKS.indexOf(ch) >= 0 && setMark(
      editor,
      ch,
      editor.primaryCaret().offset.point
    )
  }
}