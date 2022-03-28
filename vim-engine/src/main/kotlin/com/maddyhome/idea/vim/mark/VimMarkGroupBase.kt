package com.maddyhome.idea.vim.mark

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
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

  /**
   * Saves the caret location prior to doing a jump
   *
   * @param editor  The editor the jump will occur in
   */
  override fun saveJumpLocation(editor: VimEditor) {
    addJump(editor, true)
    setMark(editor, '\'')

    includeCurrentCommandAsNavigation(editor)
  }

  /**
   * Get's a mark from the file
   *
   * @param editor The editor to get the mark from
   * @param ch     The mark to get
   * @return The mark in the current file, if set, null if no such mark
   */
  override fun getFileMark(editor: VimEditor, ch: Char): Mark? {
    var myCh = ch
    if (myCh == '`') myCh = '\''
    val path = editor.getPath() ?: return null
    val fmarks = getFileMarks(path)
    var mark: Mark? = fmarks[myCh]
    if (mark != null && mark.isClear()) {
      fmarks.remove(myCh)
      mark = null
    }

    return mark
  }

  override fun setVisualSelectionMarks(editor: VimEditor, range: TextRange) {
    setMark(editor, VimMarkConstants.MARK_VISUAL_START, range.startOffset)
    setMark(editor, VimMarkConstants.MARK_VISUAL_END, range.endOffset)
  }

  override fun setChangeMarks(vimEditor: VimEditor, range: TextRange) {
    setMark(vimEditor, VimMarkConstants.MARK_CHANGE_START, range.startOffset)
    setMark(vimEditor, VimMarkConstants.MARK_CHANGE_END, range.endOffset - 1)
  }

  private fun getMarksRange(editor: VimEditor, startMark: Char, endMark: Char): TextRange? {
    val start = getMark(editor, startMark)
    val end = getMark(editor, endMark)
    if (start != null && end != null) {
      val startOffset = injector.engineEditorHelper.getOffset(editor, start.logicalLine, start.col)
      val endOffset = injector.engineEditorHelper.getOffset(editor, end.logicalLine, end.col)
      return TextRange(startOffset, endOffset + 1)
    }
    return null
  }

  override fun getChangeMarks(editor: VimEditor): TextRange? {
    return getMarksRange(editor, VimMarkConstants.MARK_CHANGE_START, VimMarkConstants.MARK_CHANGE_END)
  }

  override fun getVisualSelectionMarks(editor: VimEditor): TextRange? {
    return getMarksRange(editor, VimMarkConstants.MARK_VISUAL_START, VimMarkConstants.MARK_VISUAL_END)
  }

  override fun resetAllMarks() {
    globalMarks.clear()
    fileMarks.clear()
    jumps.clear()
  }

  override fun removeMark(ch: Char, mark: Mark) {
    if (VimMarkConstants.FILE_MARKS.indexOf(ch) >= 0) {
      val fmarks = getFileMarks(mark.filename)
      fmarks.remove(ch)
    } else if (VimMarkConstants.GLOBAL_MARKS.indexOf(ch) >= 0) {
      // Global marks are added to global and file marks
      val fmarks = getFileMarks(mark.filename)
      fmarks.remove(ch)
      globalMarks.remove(ch)
    }

    mark.clear()
  }

  override fun getMarks(editor: VimEditor): List<Mark> {
    val res = HashSet<Mark>()

    val path = editor.getPath()
    if (path != null) {
      val marks = getFileMarks(path)
      res.addAll(marks.values)
    }
    res.addAll(globalMarks.values)

    val list = ArrayList(res)

    list.sortWith(Mark.KeySorter)

    return list
  }

  override fun getJumps(): List<Jump> {
    return jumps
  }

  override fun getJumpSpot(): Int {
    return jumpSpot
  }
}