/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.api.VimMarkService.Companion.BEFORE_JUMP_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.CHANGE_END_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.CHANGE_START_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.INSERT_EXIT_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.LAST_BUFFER_POSITION
import com.maddyhome.idea.vim.api.VimMarkService.Companion.LAST_CHANGE_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.LOWERCASE_MARKS
import com.maddyhome.idea.vim.api.VimMarkService.Companion.NUMBERED_MARKS
import com.maddyhome.idea.vim.api.VimMarkService.Companion.PARAGRAPH_END_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.PARAGRAPH_START_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.SELECTION_END_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.SELECTION_START_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.SENTENCE_END_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.SENTENCE_START_MARK
import com.maddyhome.idea.vim.api.VimMarkService.Companion.UPPERCASE_MARKS
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.mark.VimMark
import java.lang.IllegalArgumentException
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*
import kotlin.collections.HashMap

abstract class VimMarkServiceBase : VimMarkService {
  companion object {
    private val logger = vimLogger<VimMarkServiceBase>()
  }
  @JvmField
  protected val globalMarks = HashMap<Char, Mark>()

  // marks are stored for primary caret only
  protected val filepathToLocalMarks = HashMap<String, LocalMarks<Char, Mark>>()

  class LocalMarks<K, V> : HashMap<K, V>() {
    var myTimestamp = Date()

    fun setTimestamp(timestamp: Date) {
      this.myTimestamp = timestamp
    }

    override fun put(key: K, value: V): V? {
      myTimestamp = Date()
      return super.put(key, value)
    }
  }

  /**
   * Gets the map of marks for the specified file
   *
   * @param filePath The file to get the marks for
   * @return The map of marks. The keys are `Character`s of the mark names, the values are
   * `Mark`s.
   */
  fun getLocalMarks(filePath: String): LocalMarks<Char, Mark> {
    return filepathToLocalMarks.getOrPut(filePath) { LocalMarks() }
  }

  override fun getGlobalMark(char: Char): Mark? {
    if (char.isGlobalMark()) {
      return globalMarks[char]
    }
    return null
  }

  protected fun getLocalMark(caret: ImmutableVimCaret, char: Char): Mark? {
    val markChar = if (char == '`') '\'' else char
    if (!markChar.isLocalMark()) return null
    val editor = caret.editor
    val path = editor.getPath() ?: return null
    return if (caret.isPrimary) {
      val mark = getLocalMarks(path)[markChar]
      when (markChar) {
        VimMarkService.LAST_BUFFER_POSITION -> mark ?: VimMark('"', 1, 1, path, editor.extractProtocol())
        else -> mark
      }
    } else {
      caret.markStorage.getMark(markChar)
    }
  }

  override fun getMark(caret: ImmutableVimCaret, char: Char): Mark? {
    val markChar = if (char == '`') '\'' else char
    if (!markChar.isValid(VimMarkService.Operation.GET, caret)) return null

    val editor = caret.editor
    val path = editor.getPath()
    return if (path != null && setOf(VimMarkService.PARAGRAPH_START_MARK, VimMarkService.PARAGRAPH_END_MARK).contains(markChar)) {
      getParagraphMark(editor, caret, markChar)
    } else if (path != null && setOf(VimMarkService.SENTENCE_START_MARK, VimMarkService.SENTENCE_END_MARK).contains(markChar)) {
      getSentenceMark(editor, caret, markChar)
    } else if (markChar.isLocalMark()) {
      getLocalMark(caret, markChar)
    } else if (markChar.isGlobalMark()) {
      getGlobalMark(markChar)
    } else {
      null
    }
  }

  override fun getAllLocalMarks(caret: ImmutableVimCaret): Set<Mark> {
    val path = caret.editor.getPath() ?: return emptySet()
    return if (caret.isPrimary) {
      getLocalMarks(path).values.toSet()
    } else {
      caret.markStorage.getMarks().values.toSet()
    }
  }

  override fun getAllMarksForFile(editor: VimEditor): Set<Mark> {
    val localMarks = injector.editorGroup.getAllEditors()
      .filter {  it.getPath() == editor.getPath() }
      .flatMap { it.carets() }
      .flatMap { getAllLocalMarks(it) }
      .toSet()
    val globalMarks = getGlobalMarks(editor)
    return localMarks + globalMarks
  }

  override fun getAllGlobalMarks(): Set<Mark> {
    return globalMarks.values.toSet()
  }

  override fun getGlobalMarks(editor: VimEditor): Set<Mark> {
    val filePath = editor.getPath() ?: return emptySet()
    return globalMarks
      .map { it.value }
      .filter { it.filepath == filePath }
      .toSet()
  }

  override fun setMark(editor: VimEditor, char: Char): Boolean {
    val markChar = if (char == '`') '\'' else char
    if (!markChar.isValid(VimMarkService.Operation.SET, editor.primaryCaret())) return false

    return when {
      markChar.isGlobalMark() -> {
        setGlobalMark(editor, markChar, editor.primaryCaret().offset.point)
      }

      markChar.isLocalMark() -> {
        editor.carets().forEach { setMark(it, markChar, it.offset.point) }
        true
      }

      else -> false
    }
  }

  override fun setMark(caret: ImmutableVimCaret, mark: Mark): Boolean {
    val markChar = mark.key

    if (!markChar.isValid(VimMarkService.Operation.SET, caret)) return false

    if (markChar.isGlobalMark()) {
      setGlobalMark(caret.editor, mark)
      return true
    }

    if (markChar.isLocalMark()) {
      if (caret.isPrimary) {
        getLocalMarks(mark.filepath)[markChar] = mark
      } else {
        caret.markStorage.setMark(mark)
      }
      return true
    }

    return false
  }

  override fun setMark(caret: ImmutableVimCaret, char: Char, offset: Int): Boolean {
    val markChar = if (char == '`') '\'' else char
    val editor = caret.editor
    val position = editor.offsetToBufferPosition(offset)
    val path = editor.getPath() ?: return false
    val mark = VimMark(markChar, position.line, position.column, path, editor.extractProtocol())
    return setMark(caret, mark)
  }

  protected open fun createGlobalMark(editor: VimEditor, char: Char, offset: Int): Mark? {
    val markChar = if (char == '`') '\'' else char
    if (!markChar.isGlobalMark()) return null
    if (!markChar.isValid(VimMarkService.Operation.SET, editor.primaryCaret())) return null
    val position = editor.offsetToBufferPosition(offset)
    val path = editor.getPath() ?: return null
    return VimMark(markChar, position.line, position.column, path, editor.extractProtocol())
  }

  override fun setGlobalMark(editor: VimEditor, char: Char, offset: Int): Boolean {
    val mark = createGlobalMark(editor, char, offset) ?: return false
    return setGlobalMark(editor, mark)
  }

  override fun setGlobalMark(editor: VimEditor, mark: Mark): Boolean {
    if (!mark.key.isValid(VimMarkService.Operation.SET, editor.primaryCaret())) return false
    globalMarks[mark.key] = mark
    return true
  }

  override fun setMarkForCaret(caret: ImmutableVimCaret, char: Char, offset: Int): Boolean {
    val markChar = if (char == '`') '\'' else char
    if (!markChar.isValid(VimMarkService.Operation.SET, caret)) return false
    return when {
      markChar.isGlobalMark() -> if (caret.isPrimary) setGlobalMark(caret.editor, char, offset) else false
      markChar.isLocalMark() -> setMark(caret, char, offset)
      else -> false
    }
  }

  override fun setVisualSelectionMarks(editor: VimEditor) {
    if (!editor.inVisualMode) return
    editor.carets()
      .map {
        val startOffset = min(it.vimSelectionStart, it.offset.point)
        val endOffset = max(it.vimSelectionStart, it.offset.point)
        it to TextRange(startOffset, endOffset)
      }
      .forEach { setVisualSelectionMarks(it.first, it.second) }
  }

  override fun setVisualSelectionMarks(caret: ImmutableVimCaret, range: TextRange) {
    setMark(caret, SELECTION_START_MARK, range.startOffset)
    setMark(caret, SELECTION_END_MARK, range.endOffset)
  }

  override fun getVisualSelectionMarks(caret: ImmutableVimCaret): TextRange? {
    return getMarksRange(caret, SELECTION_START_MARK, SELECTION_END_MARK)
  }

  override fun setChangeMarks(caret: ImmutableVimCaret, range: TextRange) {
    setMark(caret, CHANGE_START_MARK, range.startOffset)
    setMark(caret, CHANGE_END_MARK, range.endOffset - 1)
  }

  override fun getChangeMarks(caret: ImmutableVimCaret): TextRange? {
    return getMarksRange(caret, CHANGE_START_MARK, CHANGE_END_MARK)
  }

  private fun getMarksRange(caret: ImmutableVimCaret, startMark: Char, endMark: Char): TextRange? {
    val editor = caret.editor
    val startOffset = getLocalMark(caret, startMark)
      ?.offset(editor) ?: return null
    val endOffset = getLocalMark(caret, endMark)
      ?.offset(editor) ?: return null
    return TextRange(startOffset, endOffset + 1)
  }

  override fun removeMark(editor: VimEditor, char: Char) {
    val markChar = if (char == '`') '\'' else char
    if (markChar.isLocalMark()) {
      editor.carets()
        .forEach { removeLocalMark(it, char) }
    }
    if (markChar.isGlobalMark()) {
      removeGlobalMark(char)
    }
  }

  override fun removeLocalMark(caret: ImmutableVimCaret, char: Char) {
    val markChar = if (char == '`') '\'' else char
    if (!markChar.isLocalMark()) return

    if (caret.isPrimary) {
      val path = caret.editor.getPath() ?: return
      getLocalMarks(path).remove(markChar)
    } else {
      caret.markStorage.removeMark(markChar, caret)
    }
  }

  override fun removeGlobalMark(char: Char) {
    val markChar = if (char == '`') '\'' else char
    if (markChar.isGlobalMark()) {
      globalMarks.remove(markChar)
    }
  }

  override fun updateMarksFromInsert(editor: VimEditor, insertStartOffset: Int, insertLength: Int) {
    val marks = getAllMarksForFile(editor)
    if (marks.isEmpty()) return

    val insertEndOffset = insertStartOffset + insertLength
    val insStart = editor.offsetToBufferPosition(insertStartOffset)
    val insEnd = editor.offsetToBufferPosition(insertEndOffset)
    logger.debug { "mark insert. insStart = $insertStartOffset, insEnd = $insertEndOffset" }
    val lines = insEnd.line - insStart.line
    if (lines == 0) return

    for (mark in marks.filterIsInstance<VimMark>()) {
      logger.debug { "mark = $mark" }
      if (insStart.line < mark.line) {
        mark.line = mark.line + lines
        logger.debug { "Shifting mark by $lines lines" }
      }
    }
  }

  override fun updateMarksFromDelete(editor: VimEditor, delStartOffset: Int, delLength: Int) {
    val marks = getAllMarksForFile(editor)
    if (marks.isEmpty()) return

    val delEndOffset = delStartOffset + delLength - 1
    val delStart = editor.offsetToBufferPosition(delStartOffset)
    val delEnd = editor.offsetToBufferPosition(delEndOffset + 1)
    logger.debug { "mark delete. delStart = $delStart, delEnd = $delEnd" }

    for (mark in marks.filterIsInstance<VimMark>()) {
      logger.debug { "mark = $mark" }
      if (delEnd.line < mark.line) {
        val lines = delEnd.line - delStart.line
        logger.debug { "Shifting mark by $lines lines" }
        mark.line = mark.line - lines
      } else if (delStart.line <= mark.line) {
        val markLineStartOffset = editor.getLineStartOffset(mark.line)
        val markLineEndOffset = editor.getLineEndOffset(mark.line, true)

        val command = editor.vimStateMachine.executingCommand
        // If text is being changed from the start of the mark line (a special case for mark deletion)
        val changeFromMarkLineStart = (command != null && command.type === Command.Type.CHANGE && delStartOffset == markLineStartOffset)
        // If the marked line is completely within the deleted text, remove the mark (except the special case)
        if (delStartOffset <= markLineStartOffset && delEndOffset >= markLineEndOffset && !changeFromMarkLineStart) {
          injector.markService.removeMark(editor, mark.key)
          logger.debug("Removed mark")
        } else if (delStart.line < mark.line) {
          // shift mark
          mark.line = delStart.line
          logger.debug { "Shifting mark to line " + delStart.line }
        } // The deletion only covers part of the marked line so shift the mark only if the deletion begins
        // on a line prior to the marked line (which means the deletion must end on the marked line).
      } // If the deleted text begins before the mark and ends after the mark then it may be shifted or deleted
    }
  }

  override fun editorReleased(editor: VimEditor) {
    setMark(editor.primaryCaret(), LAST_BUFFER_POSITION, editor.primaryCaret().offset.point)
  }

  override fun resetAllMarksForCaret(caret: ImmutableVimCaret) {
    if (caret.isPrimary) {
      filepathToLocalMarks.clear()
    } else {
      caret.markStorage.clear(caret)
    }
  }

  override fun resetAllMarks() {
    for (editor in injector.editorGroup.getAllEditors()) {
      editor.carets().forEach {
        resetAllMarksForCaret(it)
      }
    }
    filepathToLocalMarks.clear()
    globalMarks.clear()
  }

  override fun isValidMark(char: Char, operation: VimMarkService.Operation, isCaretPrimary: Boolean): Boolean {
    return when (operation) {
      VimMarkService.Operation.GET -> {
        char.isLocalMark() || char.isGlobalMark()
      }
      VimMarkService.Operation.SET -> {
        (LOWERCASE_MARKS +
          CHANGE_START_MARK + CHANGE_END_MARK +
          SELECTION_START_MARK + SELECTION_END_MARK +
          BEFORE_JUMP_MARK +
          LAST_CHANGE_MARK +
          LAST_BUFFER_POSITION +
          INSERT_EXIT_MARK
        ).contains(char) ||
        (isCaretPrimary && (UPPERCASE_MARKS + NUMBERED_MARKS).contains(char))
      }
      VimMarkService.Operation.REMOVE -> {
        (LOWERCASE_MARKS +
          CHANGE_START_MARK + CHANGE_END_MARK +
          SELECTION_START_MARK + SELECTION_END_MARK +
          LAST_CHANGE_MARK +
          LAST_BUFFER_POSITION +
          INSERT_EXIT_MARK
          ).contains(char) ||
          (isCaretPrimary && (UPPERCASE_MARKS + NUMBERED_MARKS).contains(char))
      }
      VimMarkService.Operation.SAVE -> {
        isCaretPrimary &&
          (LOWERCASE_MARKS +
            BEFORE_JUMP_MARK +
            LAST_CHANGE_MARK +
            INSERT_EXIT_MARK +
            LAST_BUFFER_POSITION +
            CHANGE_START_MARK + CHANGE_END_MARK
            ).contains(char)
      }
    }
  }

  private fun Char.isValid(operation: VimMarkService.Operation, caret: ImmutableVimCaret): Boolean {
    return isValidMark(this, operation, caret.isPrimary)
  }

  protected fun Char.isGlobalMark(): Boolean {
    return (UPPERCASE_MARKS + NUMBERED_MARKS).contains(this)
  }

  protected fun Char.isLocalMark(): Boolean {
    return (LOWERCASE_MARKS +
      CHANGE_START_MARK + CHANGE_END_MARK +
      SELECTION_START_MARK + SELECTION_END_MARK +
      SENTENCE_START_MARK + SENTENCE_END_MARK +
      PARAGRAPH_START_MARK + PARAGRAPH_END_MARK +
      BEFORE_JUMP_MARK +
      LAST_CHANGE_MARK +
      LAST_BUFFER_POSITION +
      INSERT_EXIT_MARK
      ).contains(this)
  }


  private fun getParagraphMark(editor: VimEditor, caret: ImmutableVimCaret, char: Char): VimMark? {
    val path = editor.getPath() ?: return null
    val count = when (char) {
      PARAGRAPH_START_MARK -> -1
      PARAGRAPH_END_MARK -> 1
      else -> throw IllegalArgumentException("Invalid paragraph mark char")
    }
    var offset = injector.searchHelper.findNextParagraph(editor, caret, count, allowBlanks = false)
    offset = editor.normalizeOffset(offset, false)
    val lp = editor.offsetToBufferPosition(offset)
    return VimMark(char, lp.line, lp.column, path, editor.extractProtocol())
  }

  private fun getSentenceMark(editor: VimEditor, caret: ImmutableVimCaret, char: Char): VimMark? {
    val path = editor.getPath() ?: return null
    val count = when (char) {
      SENTENCE_START_MARK -> -1
      SENTENCE_END_MARK -> 1
      else -> throw IllegalArgumentException("Invalid sentence mark char")
    }
    var offset = injector.searchHelper.findNextSentenceStart(editor, caret, count, countCurrent = false, requireAll = true)
    offset = editor.normalizeOffset(offset, false)
    val lp = editor.offsetToBufferPosition(offset)
    return VimMark(char, lp.line, lp.column, path, editor.extractProtocol())
  }
}

class LocalMarkStorage(var caret: ImmutableVimCaret) {
  private val marks = HashMap<Char, Mark>()

  fun getMarks(): Map<Char, Mark> {
    return marks.toMap()
  }

  fun getMark(char: Char): Mark? {
    if (caret.isPrimary) {
      return injector.markService.getMark(caret, char)
    }
    return marks[char]
  }

  fun setMark(mark: Mark): Boolean {
    // todo check if set is valid for secondary caret
    if (caret.isPrimary) {
      return injector.markService.setMark(caret, mark)
    }
    val markChar = mark.key
    marks[markChar] = mark
    return true
  }

  fun removeMark(char: Char, caret: ImmutableVimCaret) {
    // todo avoid this because of the stackOverflow
    if (caret.isPrimary) {
      val editor = caret.editor
      return injector.markService.removeLocalMark(caret, char)
    } else {
      marks.remove(char)
    }
  }

  fun clear(caret: ImmutableVimCaret) {
    if (caret.isPrimary) {
      injector.markService.resetAllMarksForCaret(caret)
    } else {
      marks.clear()
    }
  }
}
