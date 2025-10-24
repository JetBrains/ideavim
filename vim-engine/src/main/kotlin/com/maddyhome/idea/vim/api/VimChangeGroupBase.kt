/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.Motion.AbsoluteOffset
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.helper.NumberType
import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.helper.usesVirtualSpace
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_END
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_START
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_INSERTED_TEXT_REGISTER
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.undo.VimKeyBasedUndoService
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService
import com.maddyhome.idea.vim.vimscript.model.commands.SortOption
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.awt.event.KeyEvent
import java.math.BigInteger
import java.util.*
import javax.swing.KeyStroke
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class VimChangeGroupBase : VimChangeGroup {
  private var repeatLines: Int = 0
  private var repeatColumn: Int = 0
  private var repeatAppend: Boolean = false

  @JvmField
  protected val strokes: MutableList<Any> = ArrayList()

  @JvmField
  protected var repeatCharsCount: Int = 0

  @JvmField
  protected var lastStrokes: MutableList<Any>? = null

  @JvmField
  protected var oldOffset: Int = -1

  @JvmField
  protected var vimDocumentListener: ChangesListener? = null

  @JvmField
  protected var lastLower: Boolean = true

  @JvmField
  protected var vimDocument: VimDocument? = null

  @JvmField
  protected var lastInsert: Command? = null

  override fun setInsertRepeat(lines: Int, column: Int, append: Boolean) {
    repeatLines = lines
    repeatColumn = column
    repeatAppend = append
  }

  /**
   * Deletes count character after the caret from the editor
   *
   * @param editor The editor to remove characters from
   * @param caret  The caret on which the operation is performed
   * @param count  The numbers of characters to delete.
   * @return true if able to delete, false if not
   */
  override fun deleteCharacter(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val endOffset = injector.motion.getHorizontalMotion(editor, caret, count, true)
    if (endOffset is AbsoluteOffset) {
      val res = deleteText(
        editor,
        context,
        TextRange(caret.offset, endOffset.offset),
        SelectionType.CHARACTER_WISE,
        caret,
      )
      val pos = min(caret.offset, endOffset.offset)
      val norm = editor.normalizeOffset(editor.offsetToBufferPosition(pos).line, pos, isChange)
      val newCaret = if (norm != pos ||
        editor.offsetToVisualPosition(norm) !==
        injector.engineEditorHelper.inlayAwareOffsetToVisualPosition(editor, norm)
      ) {
        caret.moveToOffset(norm)
      } else {
        caret
      }
      // Always move the caret. Our position might or might not have changed, but an inlay might have been moved to our
      // location, or deleting the character(s) might have caused us to scroll sideways in long files. Moving the caret
      // will make sure it's in the right place, and visible
      val offset = editor.normalizeOffset(
        newCaret.getBufferPosition().line,
        newCaret.offset,
        isChange,
      )
      newCaret.moveToOffset(offset)
      return res
    }
    return false
  }

  /**
   * Delete text from the document. This will fail if being asked to store the deleted text into a read-only
   * register.
   *
   *
   * End offset of range is exclusive
   *
   *
   * delete new TextRange(1, 5)
   * 0123456789
   * Hello, xyz
   * .||||....
   *
   *
   * end <= text.length
   *
   * @param editor The editor to delete from
   * @param range  The range to delete
   * @param type   The type of deletion
   * @param saveToRegister True if deleted text should be saved to register
   * @return true if able to delete the text, false if not
   */
  protected fun deleteText(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    type: SelectionType?,
    caret: VimCaret,
    saveToRegister: Boolean = true,
  ): Boolean {
    var updatedRange = range

    // Fix for https://youtrack.jetbrains.net/issue/VIM-35
    if (!range.normalize(editor.fileSize().toInt())) {
      updatedRange = if (range.startOffset == range.endOffset
        && range.startOffset == editor.fileSize().toInt()
        && range.startOffset != 0
      ) {
        TextRange(range.startOffset - 1, range.endOffset)
      } else {
        return false
      }
    }

    val isInsertMode = editor.mode == Mode.INSERT || editor.mode == Mode.REPLACE
    val shouldYank = type != null && !isInsertMode && saveToRegister
    if (shouldYank && !caret.registerStorage.storeText(editor, context, updatedRange, type, isDelete = true)) {
      return false
    }

    val startOffsets = updatedRange.startOffsets
    val endOffsets = updatedRange.endOffsets
    for (i in updatedRange.size() - 1 downTo 0) {
      val (newRange, _) = editor.search(
        startOffsets[i] to endOffsets[i],
        editor,
        LineDeleteShift.NL_ON_END
      ) ?: continue
      injector.application.runWriteAction {
        editor.deleteString(TextRange(newRange.first, newRange.second))
      }
    }
    if (type != null) {
      val start = updatedRange.startOffset
      injector.markService.setMark(caret, MARK_CHANGE_POS, start)
      injector.markService.setChangeMarks(caret, TextRange(start, start + 1))
    }
    return true
  }

  /**
   * Inserts text into the document
   *
   * @param editor The editor to insert into
   * @param caret  The caret to start insertion in
   * @param str    The text to insert
   */
  override fun insertText(editor: VimEditor, caret: VimCaret, offset: Int, str: String): VimCaret {
    injector.application.runWriteAction {
      (editor as MutableVimEditor).insertText(caret, offset, str)
    }
    val newCaret = caret.moveToInlayAwareOffset(offset + str.length)

    injector.markService.setMark(newCaret, MARK_CHANGE_POS, offset)
    return newCaret
  }

  override fun insertText(editor: VimEditor, caret: VimCaret, str: String): VimCaret {
    return insertText(editor, caret, caret.offset, str)
  }

  open fun insertText(editor: VimEditor, caret: VimCaret, start: BufferPosition, str: String) {
    insertText(editor, caret, editor.bufferPositionToOffset(start), str)
  }

  /**
   * This repeats the previous insert count times
   *
   * Be aware that this function may call for `runForEachCaret` function because it calls for intellij actions
   *   and these actions may call for this function.
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param count   The number of times to repeat the previous insert
   */
  protected open fun repeatInsertText(
    editor: VimEditor,
    context: ExecutionContext,
    count: Int,
  ) {
    val myLastStrokes = lastStrokes ?: return
    for (caret in editor.nativeCarets()) {
      repeat(count) {
        for (lastStroke in myLastStrokes) {
          when (lastStroke) {
            is NativeAction -> {
              injector.actionExecutor.executeAction(editor, lastStroke, context)
              strokes.add(lastStroke)
            }

            is EditorActionHandlerBase -> {
              injector.actionExecutor.executeVimAction(editor, lastStroke, context, OperatorArguments(0, editor.mode))
              strokes.add(lastStroke)
            }

            is CharArray -> {
              insertText(editor, caret, String(lastStroke))
            }

            else -> {
              throw RuntimeException("Unexpected stroke type: ${lastStroke.javaClass} $lastStroke")
            }
          }
        }
      }
    }
  }

  /**
   * This repeats the previous insert count times
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param count   The number of times to repeat the previous insert
   */
  override fun repeatInsert(
    editor: VimEditor,
    context: ExecutionContext,
    count: Int,
    started: Boolean,
  ) {
    for (caret in editor.nativeCarets()) {
      if (repeatLines > 0) {
        val visualLine = caret.getVisualPosition().line
        val bufferLine = caret.getBufferPosition().line
        val position = editor.bufferPositionToOffset(BufferPosition(bufferLine, repeatColumn, false))
        for (i in 0 until repeatLines) {
          if (repeatAppend &&
            (repeatColumn < VimMotionGroupBase.LAST_COLUMN) &&
            (injector.engineEditorHelper.getVisualLineLength(editor, visualLine + i) < repeatColumn)
          ) {
            val pad = injector.engineEditorHelper.pad(editor, bufferLine + i, repeatColumn)
            if (pad.isNotEmpty()) {
              val offset = editor.getLineEndOffset(bufferLine + i)
              insertText(editor, caret, offset, pad)
            }
          }
          val updatedCount = if (started) (if (i == 0) count else count + 1) else count
          if (repeatColumn >= VimMotionGroupBase.LAST_COLUMN) {
            caret.moveToOffset(injector.motion.moveCaretToLineEnd(editor, bufferLine + i, true))
            repeatInsertText(editor, context, updatedCount)
          } else if (injector.engineEditorHelper.getVisualLineLength(editor, visualLine + i) >= repeatColumn) {
            val visualPosition = VimVisualPosition(visualLine + i, repeatColumn, false)
            val inlaysCount = injector.engineEditorHelper.amountOfInlaysBeforeVisualPosition(editor, visualPosition)
            caret.moveToVisualPosition(VimVisualPosition(visualLine + i, repeatColumn + inlaysCount, false))
            repeatInsertText(editor, context, updatedCount)
          }
        }
        caret.moveToOffset(position)
      } else {
        repeatInsertText(editor, context, count)
        val position = injector.motion.getHorizontalMotion(editor, caret, -1, false)
        caret.moveToMotion(position)
      }
    }
    repeatLines = 0
    repeatColumn = 0
    repeatAppend = false
  }

  protected inner class VimChangesListener : ChangesListener {
    override fun documentChanged(change: ChangesListener.Change) {
      val newFragment = change.newFragment
      val oldFragment = change.oldFragment
      val newFragmentLength = newFragment.length
      val oldFragmentLength = oldFragment.length

      // Repeat buffer limits
      if (repeatCharsCount > MAX_REPEAT_CHARS_COUNT) {
        return
      }

      // <Enter> is added to strokes as an action during processing in order to indent code properly in the repeat
      // command
      if (newFragment.startsWith("\n") && newFragment.trim { it <= ' ' }.isEmpty()) {
        strokes.addAll(getAdjustCaretActions(change))
        oldOffset = -1
        return
      }

      // Ignore multi-character indents as they should be inserted automatically while repeating <Enter> actions
      if (newFragmentLength > 1 && newFragment.trim { it <= ' ' }.isEmpty()) {
        return
      }
      strokes.addAll(getAdjustCaretActions(change))
      if (oldFragmentLength > 0) {
        val editorDelete = injector.nativeActionManager.deleteAction
        if (editorDelete != null) {
          repeat(oldFragmentLength) {
            strokes.add(editorDelete)
          }
        }
      }
      if (newFragmentLength > 0) {
        strokes.add(newFragment.toCharArray())
      }
      repeatCharsCount += newFragmentLength
      oldOffset = change.offset + newFragmentLength
    }

    private fun getAdjustCaretActions(change: ChangesListener.Change): List<EditorActionHandlerBase> {
      val delta: Int = change.offset - oldOffset
      if (oldOffset >= 0 && delta != 0) {
        val positionCaretActions: MutableList<EditorActionHandlerBase> = ArrayList()
        val motionName = if (delta < 0) "VimMotionLeftAction" else "VimMotionRightAction"
        val action = injector.actionExecutor.findVimAction(motionName)!!
        val count = abs(delta)
        repeat(count) {
          positionCaretActions.add(action)
        }
        return positionCaretActions
      }
      return emptyList()
    }
  }

  /**
   * Begin insert before the cursor position
   * @param editor  The editor to insert into
   * @param context The data context
   */
  override fun insertBeforeCaret(editor: VimEditor, context: ExecutionContext) {
    initInsert(editor, context, Mode.INSERT)
  }

  override fun insertAfterLineEnd(editor: VimEditor, context: ExecutionContext) {
    for (caret in editor.nativeCarets()) {
      caret.moveToOffset(injector.motion.moveCaretToCurrentLineEnd(editor, caret))
    }
    initInsert(editor, context, Mode.INSERT)
  }

  /**
   * Begin insert after the cursor position
   * @param editor  The editor to insert into
   * @param context The data context
   */
  override fun insertAfterCaret(editor: VimEditor, context: ExecutionContext) {
    for (caret in editor.nativeCarets()) {
      caret.moveToMotion(injector.motion.getHorizontalMotion(editor, caret, 1, true))
    }
    initInsert(editor, context, Mode.INSERT)
  }

  /**
   * Begin insert before the start of the current line
   * @param editor  The editor to insert into
   * @param context The data context
   */
  override fun insertLineStart(editor: VimEditor, context: ExecutionContext) {
    for (caret in editor.nativeCarets()) {
      caret.moveToOffset(injector.motion.moveCaretToCurrentLineStart(editor, caret))
    }
    initInsert(editor, context, Mode.INSERT)
  }

  /**
   * Begin insert before the first non-blank on the current line
   *
   * @param editor The editor to insert into
   */
  override fun insertBeforeFirstNonBlank(editor: VimEditor, context: ExecutionContext) {
    for (caret in editor.nativeCarets()) {
      caret.moveToOffset(injector.motion.moveCaretToCurrentLineStartSkipLeading(editor, caret))
    }
    initInsert(editor, context, Mode.INSERT)
  }

  /**
   * Begin insert/replace mode
   * @param editor  The editor to insert into
   * @param context The data context
   * @param mode    The mode - indicate insert or replace
   */
  override fun initInsert(editor: VimEditor, context: ExecutionContext, mode: Mode) {
    val state = injector.vimState
    injector.application.runReadAction {
      for (caret in editor.nativeCarets()) {
        caret.vimInsertStart = editor.createLiveMarker(caret.offset, caret.offset)
        injector.markService.setMark(caret, MARK_CHANGE_START, caret.offset)
      }
    }
    val cmd = state.executingCommand
    if (cmd != null && state.isDotRepeatInProgress) {
      editor.mode = mode
      if (mode == Mode.REPLACE) {
        editor.insertMode = false
      }
      val count = if (cmd.flags.contains(CommandFlags.FLAG_NO_REPEAT_INSERT)) 1 else cmd.count
      repeatInsert(editor, context, count, false)
      if (mode == Mode.REPLACE) {
        editor.insertMode = true
      }
      editor.mode = Mode.NORMAL()
    } else {
      lastInsert = cmd
      strokes.clear()
      repeatCharsCount = 0
      val myVimDocument = vimDocument
      if (myVimDocument != null && vimDocumentListener != null) {
        myVimDocument.removeChangeListener(vimDocumentListener!!)
      }
      vimDocument = editor.document
      val myChangeListener = VimChangesListener()
      vimDocumentListener = myChangeListener
      vimDocument!!.addChangeListener(myChangeListener)
      injector.application.runReadAction {
        oldOffset = editor.currentCaret().offset
      }
      editor.insertMode = mode == Mode.INSERT
      editor.mode = mode
    }
  }

  override fun runEnterAction(editor: VimEditor, context: ExecutionContext) {
    val state = injector.vimState
    if (!state.isDotRepeatInProgress) {
      // While repeating the enter action has been already executed because `initInsert` repeats the input
      val action = injector.nativeActionManager.enterAction
      if (action != null) {
        // We use enter action for `o`, `O` commands. If we want to undo the added \n and indent, we should record start of insert
        if (editor.mode == Mode.INSERT) {
          val undo = injector.undo
          when (undo) {
            is VimKeyBasedUndoService -> undo.setInsertNonMergeUndoKey()
            is VimTimestampBasedUndoService -> {
              val nanoTime = System.nanoTime()
              editor.forEachCaret { undo.startInsertSequence(it, it.offset, nanoTime) }
            }
          }
        }
        strokes.add(action)
        injector.actionExecutor.executeAction(editor, action, context)
      }
    }
  }

  override fun runEnterAboveAction(editor: VimEditor, context: ExecutionContext) {
    val state = injector.vimState
    if (!state.isDotRepeatInProgress) {
      // While repeating the enter action has been already executed because `initInsert` repeats the input
      val action = injector.nativeActionManager.createLineAboveCaret
      if (action != null) {
        strokes.add(action)
        injector.actionExecutor.executeAction(editor, action, context)
      }
    }
  }

  /**
   * Inserts previously inserted text
   * @param editor  The editor to insert into
   * @param context The data context
   * @param exit    true if insert mode should be exited after the insert, false should stay in insert mode
   */
  override fun insertPreviousInsert(
    editor: VimEditor,
    context: ExecutionContext,
    exit: Boolean,
    operatorArguments: OperatorArguments,
  ) {
    repeatInsertText(editor, context, 1)
    if (exit) {
      editor.exitInsertMode(context)
    }
  }

  /**
   * Terminate insert/replace mode after the user presses Escape or Ctrl-C
   *
   *
   * DEPRECATED. Please, don't use this function directly. Use ModeHelper.exitInsertMode in file ModeExtensions.kt
   */
  override fun processEscape(editor: VimEditor, context: ExecutionContext?) {
    // Get the offset for marks before we exit insert mode - switching from insert to overtype subtracts one from the
    // column offset.
    val markGroup = injector.markService
    markGroup.setMark(editor, VimMarkService.INSERT_EXIT_MARK)
    markGroup.setMark(editor, MARK_CHANGE_END)
    if (editor.mode is Mode.REPLACE) {
      editor.insertMode = true
    }
    val repeatCount0 = lastInsert?.let {
      // How many times do we want to *repeat* the insert? For a simple insert or change action, this is count-1. But if
      // the command is an operator+motion, then the count applies to the motion, not the insert/change. I.e., `2cw`
      // changes two words, rather than inserting the change twice. This is the only place where we need to know who the
      // count applies to
      if (CommandFlags.FLAG_NO_REPEAT_INSERT in it.flags || it.action.argumentType == Argument.Type.MOTION) {
        0
      } else {
        it.count - 1
      }
    } ?: 0
    if (vimDocument != null && vimDocumentListener != null) {
      vimDocument!!.removeChangeListener(vimDocumentListener!!)
      vimDocumentListener = null
    }
    lastStrokes = ArrayList(strokes)
    if (context != null) {
      injector.changeGroup.repeatInsert(editor, context, repeatCount0, true)
    }
    if (editor.mode is Mode.INSERT) {
      updateLastInsertedTextRegister()
    }

    // The change pos '.' mark is the offset AFTER processing escape, and after switching to overtype
    markGroup.setMark(editor, MARK_CHANGE_POS)
    editor.mode = Mode.NORMAL()
  }

  private fun updateLastInsertedTextRegister() {
    val textToPutRegister = StringBuilder()
    if (lastStrokes != null) {
      for (lastStroke in lastStrokes!!) {
        if (lastStroke is CharArray) {
          textToPutRegister.append(String(lastStroke))
        }
      }
    }
    injector.registerGroup.storeTextSpecial(LAST_INSERTED_TEXT_REGISTER, textToPutRegister.toString())
  }

  /**
   * Processes the Enter key by running the first successful action registered for "ENTER" keystroke.
   *
   * If this is REPLACE mode we need to turn off OVERWRITE before and then turn OVERWRITE back on after sending the
   * "ENTER" key.
   *
   * @param editor  The editor to press "Enter" in
   * @param context The data context
   */
  override fun processEnter(editor: VimEditor, context: ExecutionContext) {
    if (editor.mode is Mode.REPLACE) {
      editor.insertMode = true
    }
    val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    val actions = injector.keyGroup.getActions(editor, enterKeyStroke)
    for (action in actions) {
      if (injector.actionExecutor.executeAction(editor, action, context)) {
        break
      }
    }
    if (editor.mode is Mode.REPLACE) {
      editor.insertMode = false
    }
  }

  /**
   * Performs a mode switch after change action
   * @param editor   The editor to switch mode in
   * @param context  The data context
   * @param toSwitch The mode to switch to
   */
  override fun processPostChangeModeSwitch(
    editor: VimEditor,
    context: ExecutionContext,
    toSwitch: Mode,
  ) {
    if (toSwitch == Mode.INSERT) {
      initInsert(editor, context, Mode.INSERT)
    }
  }

  /**
   * This processes all keystrokes in Insert/Replace mode that were converted into Commands. Some of these
   * commands need to be saved off so the inserted/replaced text can be repeated properly later if needed.
   *
   * @param editor The editor the command was executed in
   * @param cmd    The command that was executed
   */
  override fun processCommand(editor: VimEditor, cmd: Command) {
    // return value never used here
    if (CommandFlags.FLAG_SAVE_STROKE in cmd.flags) {
      strokes.add(cmd.action)
    } else if (CommandFlags.FLAG_CLEAR_STROKES in cmd.flags) {
      clearStrokes(editor)
    }
  }

  /**
   * While in INSERT or REPLACE mode the user can enter a single NORMAL mode command and then automatically
   * return to INSERT or REPLACE mode.
   *
   * @param editor The editor to put into NORMAL mode for one command
   */
  override fun processSingleCommand(editor: VimEditor) {
    editor.mode = Mode.NORMAL(editor.mode)
    clearStrokes(editor)
  }

  /**
   * Delete from the cursor to the end of count - 1 lines down
   *
   * @param editor The editor to delete from
   * @param caret  VimCaret on the position to start
   * @param count  The number of lines affected
   * @return true if able to delete the text, false if not
   */
  override fun deleteEndOfLine(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val initialOffset = caret.offset
    val offset = injector.motion.moveCaretToRelativeLineEnd(editor, caret, count - 1, true)
    val lineStart = injector.motion.moveCaretToCurrentLineStart(editor, caret)
    var startOffset = initialOffset
    if (offset == initialOffset && offset != lineStart) startOffset-- // handle delete from virtual space
    if (offset != -1) {
      val rangeToDelete = TextRange(startOffset, offset)
      editor.nativeCarets().filter { it != caret && rangeToDelete.contains(it.offset) }
        .forEach { editor.removeCaret(it) }
      val res = deleteText(editor, context, rangeToDelete, SelectionType.CHARACTER_WISE, caret)
      if (editor.usesVirtualSpace) {
        caret.moveToOffset(startOffset)
      } else {
        val pos = injector.motion.getHorizontalMotion(editor, caret, -1, false)
        caret.moveToMotion(pos)
      }
      return res
    }
    return false
  }

  /**
   * Joins count lines together starting at the cursor. No count or a count of one still joins two lines.
   *
   * @param editor The editor to join the lines in
   * @param caret  The caret in the first line to be joined.
   * @param count  The number of lines to join
   * @param spaces If true the joined lines will have one space between them and any leading space on the second line
   * will be removed. If false, only the newline is removed to join the lines.
   * @return true if able to join the lines, false if not
   */
  override fun deleteJoinLines(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    spaces: Boolean,
  ): Boolean {
    var myCount = count
    if (myCount < 2) myCount = 2
    val lline = caret.getBufferPosition().line
    val total = editor.lineCount()
    return if (lline + myCount > total) {
      false
    } else {
      deleteJoinNLines(editor, context, caret, lline, myCount, spaces)
    }
  }

  /**
   * This processes all "regular" keystrokes entered while in insert/replace mode
   *
   * @param editor  The editor the character was typed into
   * @param key     The user entered keystroke
   * @return true if this was a regular character, false if not
   */
  override fun processKey(
    editor: VimEditor,
    key: KeyStroke,
    processResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.debug { "processKey($key)" }
    if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
      editor.replaceMask?.recordChangeAtCaret(editor)
      processResultBuilder.addExecutionStep { _, e, c ->
        type(e, c, key.keyChar)
      }
      return true
    } else if (key.keyCode == injector.parser.plugKeyStroke.keyCode || key.keyCode == injector.parser.actionKeyStroke.keyCode) {
      // <Plug> and <Action> are fake keystrokes that can never be typed. If a failed mapping is replaying it as part of
      // Insert or Select mode, we need to replace it with the name of the key as text.
      processResultBuilder.addExecutionStep { _, e, c ->
        type(e, c, injector.parser.toKeyNotation(key))
      }
      return true
    }

    // Shift-space
    if (key.keyCode == 32 && key.modifiers and KeyEvent.SHIFT_DOWN_MASK != 0) {
      editor.replaceMask?.recordChangeAtCaret(editor)
      processResultBuilder.addExecutionStep { _, e, c ->
        type(e, c, ' ')
      }
      return true
    }
    return false
  }

  override fun processKeyInSelectMode(
    editor: VimEditor,
    key: KeyStroke,
    processResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    var res: Boolean
    SelectionVimListenerSuppressor.lock().use {
      res = processKey(editor, key, processResultBuilder)
      processResultBuilder.addExecutionStep { _, lambdaEditor, lambdaContext ->
        lambdaEditor.exitSelectModeNative(false)
        KeyHandler.getInstance().reset(lambdaEditor)
        if (isPrintableChar(key.keyChar) || activeTemplateWithLeftRightMotion(lambdaEditor, key)) {
          injector.changeGroup.insertBeforeCaret(lambdaEditor, lambdaContext)
        }
      }
    }
    return res
  }

  /**
   * Deletes count lines including the current line
   *
   * @param editor The editor to remove the lines from
   * @param count  The number of lines to delete
   * @return true if able to delete the lines, false if not
   */
  override fun deleteLine(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val start = injector.motion.moveCaretToCurrentLineStart(editor, caret)
    val offset =
      min(injector.motion.moveCaretToRelativeLineEnd(editor, caret, count - 1, true) + 1, editor.fileSize().toInt())

    if (logger.isDebug()) {
      logger.debug("start=$start")
      logger.debug("offset=$offset")
    }
    if (offset != -1) {
      val res = deleteText(editor, context, TextRange(start, offset), SelectionType.LINE_WISE, caret)
      if (res && caret.offset >= editor.fileSize() && caret.offset != 0) {
        caret.moveToOffset(
          injector.motion.moveCaretToRelativeLineStartSkipLeading(
            editor,
            caret,
            -1,
          ),
        )
      }
      return res
    }
    return false
  }

  override fun joinViaIdeaByCount(editor: VimEditor, context: ExecutionContext, count: Int): Boolean {
    val executions = if (count > 1) count - 1 else 1
    val allowedExecution = editor.nativeCarets().any { caret: ImmutableVimCaret ->
      val lline = caret.getBufferPosition().line
      val total = editor.lineCount()
      lline + count <= total
    }
    if (!allowedExecution) return false
    repeat(executions) {
      val joinLinesAction = injector.nativeActionManager.joinLines
      if (joinLinesAction != null) {
        injector.actionExecutor.executeAction(editor, joinLinesAction, context)
      }
    }
    return true
  }

  /**
   * Joins all the lines selected by the current visual selection.
   *
   * @param editor The editor to join the lines in
   * @param caret  The caret to be moved after joining
   * @param range  The range of the visual selection
   * @param spaces If true the joined lines will have one space between them and any leading space on the second line
   * will be removed. If false, only the newline is removed to join the lines.
   * @return true if able to join the lines, false if not
   */
  override fun deleteJoinRange(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    range: TextRange,
    spaces: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val startLine = editor.offsetToBufferPosition(range.startOffset).line
    val endLine = editor.offsetToBufferPosition(range.endOffset).line
    var count = endLine - startLine + 1
    if (count < 2) count = 2
    return deleteJoinNLines(editor, context, caret, startLine, count, spaces)
  }

  override fun joinViaIdeaBySelections(
    editor: VimEditor,
    context: ExecutionContext,
    caretsAndSelections: Map<VimCaret, VimSelection>,
  ) {
    caretsAndSelections.forEach { (caret: VimCaret, range: VimSelection) ->
      if (!caret.isValid) return@forEach
      val (first, second) = range.getNativeStartAndEnd()
      caret.setSelection(
        first,
        second,
      )
    }
    val joinLinesAction = injector.nativeActionManager.joinLines
    if (joinLinesAction != null) {
      injector.actionExecutor.executeAction(editor, joinLinesAction, context)
    }
    editor.nativeCarets().forEach { caret: VimCaret ->
      caret.removeSelection()
      val (line, column) = caret.getVisualPosition()
      if (line < 1) return@forEach
      val newVisualPosition = VimVisualPosition(line - 1, column, false)
      caret.moveToVisualPosition(newVisualPosition)
    }
  }

  override fun getDeleteRangeAndType(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
  ): Pair<TextRange, SelectionType>? {
    check(argument is Argument.Motion) { "Unexpected argument: $argument" }

    val range = injector.motion.getMotionRange(editor, caret, context, argument, operatorArguments) ?: return null
    var motionType = argument.getMotionType()

    // Delete motion commands that are not linewise become linewise if all the following are true:
    // 1) The range is across multiple lines
    // 2) There is only whitespace before the start of the range
    // 3) There is only whitespace after the end of the range
    if (!isChange && motionType != SelectionType.LINE_WISE) {
      val start = editor.offsetToBufferPosition(range.startOffset)
      val end = editor.offsetToBufferPosition(range.endOffset)
      if (start.line != end.line
        && !editor.anyNonWhitespace(range.startOffset, -1)
        && !editor.anyNonWhitespace(range.endOffset, 1)
      ) {
        motionType = SelectionType.LINE_WISE
      }
    }
    return Pair(range, motionType)
  }

  /**
   * Delete the range of text.
   *
   * @param editor   The editor to delete the text from
   * @param caret    The caret to be moved after deletion
   * @param range    The range to delete
   * @param type     The type of deletion
   * @param isChange Is from a change action
   * @param saveToRegister True if deleted text should be saved to register
   * @return true if able to delete the text, false if not
   */
  override fun deleteRange(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType?,
    isChange: Boolean,
    saveToRegister: Boolean,
  ): Boolean {
    val intendedColumn = caret.vimLastColumn

    val removeLastNewLine = removeLastNewLine(editor, range, type)
    val res = deleteText(editor, context, range, type, caret, saveToRegister)
    var processedCaret = editor.findLastVersionOfCaret(caret) ?: caret
    if (removeLastNewLine) {
      val textLength = editor.fileSize().toInt()
      injector.application.runWriteAction {
        editor.deleteString(TextRange(textLength - 1, textLength))
      }
      processedCaret = editor.findLastVersionOfCaret(caret) ?: caret
    }

    if (res) {
      var pos = editor.normalizeOffset(range.startOffset, isChange)
      processedCaret = if (type === SelectionType.LINE_WISE) {
        // Reset the saved intended column cache, which has been invalidated by the caret moving due to deleted text.
        // This value will be used to reposition the caret if 'startofline' is false
        val updated = processedCaret.setVimLastColumnAndGetCaret(intendedColumn)
        pos = injector.motion
          .moveCaretToLineWithStartOfLineOption(
            editor,
            editor.offsetToBufferPosition(pos).line,
            caret,
          )
        updated
      } else {
        caret
      }
      processedCaret = processedCaret.moveToOffset(pos)

      // Ensure the intended column cache is invalidated - it will only happen automatically if the caret actually moves
      // If 'startofline' is true and we've just deleted text, it's likely we haven't moved
      processedCaret.resetLastColumn()
    }
    return res
  }

  private fun removeLastNewLine(editor: VimEditor, range: TextRange, type: SelectionType?): Boolean {
    var endOffset = range.endOffset
    val fileSize = editor.fileSize().toInt()
    if (endOffset > fileSize) {
      StrictMode.fail("Incorrect offset. File size: $fileSize, offset: $endOffset")
      endOffset = fileSize
    }
    return (type === SelectionType.LINE_WISE) && range.startOffset != 0 && editor.text()[endOffset - 1] != '\n' && endOffset == fileSize
  }

  /**
   * Delete from the cursor to the end of count - 1 lines down and enter insert mode
   *
   * @param editor The editor to change
   * @param caret  The caret to perform action on
   * @param count  The number of lines to change
   * @return true if able to delete count lines, false if not
   */
  override fun changeEndOfLine(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val res = deleteEndOfLine(editor, context, caret, count, operatorArguments)
    if (res) {
      caret.moveToOffset(injector.motion.moveCaretToCurrentLineEnd(editor, caret))
      editor.vimChangeActionSwitchMode = Mode.INSERT
    }
    return res
  }

  /**
   * Delete count characters and then enter insert mode
   *
   * @param editor The editor to change
   * @param caret  The caret to be moved
   * @return true if able to delete count characters, false if not
   */
  override fun changeCharacters(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val count = operatorArguments.count1
    // TODO  is it correct to use primary caret? There is a caret as an argument
    val len = editor.lineLength(editor.primaryCaret().getBufferPosition().line)
    val col = caret.getBufferPosition().column
    if (col + count >= len) {
      return changeEndOfLine(editor, context, caret, 1, operatorArguments)
    }
    val res = deleteCharacter(editor, context, caret, count, true, operatorArguments)
    if (res) {
      editor.vimChangeActionSwitchMode = Mode.INSERT
    }
    return res
  }

  protected abstract fun reformatCode(editor: VimEditor, start: Int, end: Int)

  /**
   * Clears all the keystrokes from the current insert command
   *
   * @param editor The editor to clear strokes from.
   */
  protected fun clearStrokes(editor: VimEditor) {
    strokes.clear()
    repeatCharsCount = 0
    for (caret in editor.nativeCarets()) {
      caret.vimInsertStart = editor.createLiveMarker(caret.offset, caret.offset)
    }
  }

  /**
   * This does the actual joining of the lines
   *
   * @param editor    The editor to join the lines in
   * @param caret     The caret on the starting line (to be moved)
   * @param startLine The starting buffer line
   * @param count     The number of lines to join including startLine
   * @param spaces    If true the joined lines will have one space between them and any leading space on the second line
   * will be removed. If false, only the newline is removed to join the lines.
   * @return true if able to join the lines, false if not
   */
  private fun deleteJoinNLines(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    startLine: Int,
    count: Int,
    spaces: Boolean,
  ): Boolean {
    // Don't move the caret until we've successfully deleted text. If we're on the last line, we don't want to move the
    // caret and then be unable to delete
    for (i in 1 until count) {
      val startOffset = injector.motion.moveCaretToLineEnd(editor, startLine, true)
      val trailingWhitespaceStart = injector.motion.moveCaretToLineEndSkipTrailing(editor, startLine)
      val hasTrailingWhitespace = startOffset > (trailingWhitespaceStart + 1) // + newline
      val endOffset: Int = if (spaces) {
        editor.getLeadingCharacterOffset(editor.normalizeLine(startLine + 1))
      } else {
        editor.getLineStartOffset(editor.normalizeLine(startLine + 1))
      }
      if (endOffset <= startOffset) {
        return i > 1
      }
      // Note that caret isn't moved here; it's only used for register + mark storage
      deleteText(editor, context, TextRange(startOffset, endOffset), null, caret)
      if (spaces && !hasTrailingWhitespace) {
        insertText(editor, caret, startOffset, " ")
      }
      caret.moveToOffset(startOffset)
    }
    return true
  }

  private fun isPrintableChar(c: Char): Boolean {
    val block = Character.UnicodeBlock.of(c)
    return !Character.isISOControl(c) &&
      (c != KeyEvent.CHAR_UNDEFINED) &&
      (block != null) &&
      block !== Character.UnicodeBlock.SPECIALS
  }

  private fun activeTemplateWithLeftRightMotion(editor: VimEditor, keyStroke: KeyStroke): Boolean {
    return injector.templateManager.getTemplateState(editor) != null &&
      (keyStroke.keyCode == KeyEvent.VK_LEFT || keyStroke.keyCode == KeyEvent.VK_RIGHT)
  }

  /**
   * Replace text in the editor
   *
   * @param editor The editor to replace text in
   * @param start  The start offset to change
   * @param end    The end offset to change
   * @param str    The new text
   */
  override fun replaceText(editor: VimEditor, caret: VimCaret, start: Int, end: Int, str: String) {
    injector.application.runWriteAction {
      (editor as MutableVimEditor).replaceString(start, end, str)
    }

    val newEnd = start + str.length
    injector.markService.setChangeMarks(caret, TextRange(start, newEnd))
    injector.markService.setMark(caret, VimMarkService.LAST_CHANGE_MARK, newEnd)
  }

  /**
   * Inserts a new line above the caret position
   *
   * @param editor The editor to insert into
   * @param caret  The caret to insert above
   * @param col    The column to indent to
   */
  protected open fun insertNewLineAbove(editor: VimEditor, caret: VimCaret, col: Int) {
    if (editor.isOneLineMode()) return
    var firstLiner = false
    var newCaret = if (caret.getVisualPosition().line == 0) {
      val newCaret = caret.moveToOffset(
        injector.motion.moveCaretToCurrentLineStart(
          editor,
          caret,
        ),
      )
      firstLiner = true
      newCaret
    } else {
      // TODO: getVerticalMotionOffset returns a visual line, not the expected logical line
      // Also address the unguarded upcast
      val motion = injector.motion.getVerticalMotionOffset(editor, caret, -1)
      val updated = caret.moveToOffset((motion as AbsoluteOffset).offset)
      updated.moveToOffset(injector.motion.moveCaretToCurrentLineEnd(editor, updated))
    }
    editor.vimChangeActionSwitchMode = Mode.INSERT
    newCaret = insertText(
      editor,
      newCaret,
      "\n${editor.createIndentBySize(col)}",
    )
    if (firstLiner) {
      // TODO: getVerticalMotionOffset returns a visual line, not the expected logical line
      // Also address the unguarded upcast
      val motion = injector.motion.getVerticalMotionOffset(editor, newCaret, -1)
      newCaret.moveToOffset((motion as AbsoluteOffset).offset)
    }
  }

  override fun changeMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): Boolean {
    var count0 = operatorArguments.count0
    // Vim treats cw as ce and cW as cE if cursor is on a non-blank character
    var motionArgument = argument as? Argument.Motion ?: return false
    val id = motionArgument.motion.id
    var kludge = false
    val bigWord = id == VIM_MOTION_BIG_WORD_RIGHT
    val chars = editor.text()
    val offset = caret.offset
    val fileSize = editor.fileSize().toInt()
    if (fileSize > 0 && offset < fileSize) {
      val charType = charType(editor, chars[offset], bigWord)
      if (charType !== CharacterHelper.CharacterType.WHITESPACE) {
        val lastWordChar = offset >= fileSize - 1 || charType(editor, chars[offset + 1], bigWord) !== charType
        if (wordMotions.contains(id) && lastWordChar && operatorArguments.count1 == 1) {
          val res = deleteCharacter(editor, context, caret, 1, true, operatorArguments)
          if (res) {
            editor.vimChangeActionSwitchMode = Mode.INSERT
          }
          return res
        }
        when (id) {
          VIM_MOTION_WORD_RIGHT -> {
            kludge = true
            motionArgument = Argument.Motion(
              injector.actionExecutor.findVimActionOrDie(VIM_MOTION_WORD_END_RIGHT) as MotionActionHandler,
              motionArgument.argument
            )
          }

          VIM_MOTION_BIG_WORD_RIGHT -> {
            kludge = true
            motionArgument = Argument.Motion(
              injector.actionExecutor.findVimActionOrDie(VIM_MOTION_BIG_WORD_END_RIGHT) as MotionActionHandler,
              motionArgument.argument
            )
          }

          VIM_MOTION_CAMEL_RIGHT -> {
            kludge = true
            motionArgument = Argument.Motion(
              injector.actionExecutor.findVimActionOrDie(VIM_MOTION_CAMEL_END_RIGHT) as MotionActionHandler,
              motionArgument.argument
            )
          }
        }
      }
    }
    if (kludge) {
      val pos1 = injector.searchHelper.findNextWordEnd(editor, offset, operatorArguments.count1, bigWord, false)
      val pos2 = injector.searchHelper.findNextWordEnd(editor, pos1, -operatorArguments.count1, bigWord, false)
      if (logger.isDebug()) {
        logger.debug("pos=$offset")
        logger.debug("pos1=$pos1")
        logger.debug("pos2=$pos2")
        logger.debug("count=" + operatorArguments.count1)
      }
      if (pos2 == offset && operatorArguments.count1 > 1) {
        count0--
      }
    }
    val (first, second) = getDeleteRangeAndType(
      editor,
      caret,
      context,
      motionArgument,
      true,
      operatorArguments.copy(count0 = count0),
    ) ?: return false
    return changeRange(
      editor,
      caret,
      first,
      second,
      context,
    )
  }

  /**
   * Inserts a new line below the caret position
   *
   * @param editor The editor to insert into
   * @param caret  The caret to insert after
   * @param col    The column to indent to
   */
  private fun insertNewLineBelow(editor: VimEditor, caret: VimCaret, col: Int) {
    if (editor.isOneLineMode()) return
    val newCaret = caret.moveToOffset(injector.motion.moveCaretToCurrentLineEnd(editor, caret))
    editor.vimChangeActionSwitchMode = Mode.INSERT
    insertText(editor, newCaret, "\n${editor.createIndentBySize(col)}")
  }

  /**
   * Deletes the range of text and enters insert mode
   *
   * @param editor            The editor to change
   * @param caret             The caret to be moved after range deletion
   * @param range             The range to change
   * @param type              The type of the range
   * @return true if able to delete the range, false if not
   */
  override fun changeRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType,
    context: ExecutionContext,
  ): Boolean {
    var col = 0
    var lines = 0
    if (type === SelectionType.BLOCK_WISE) {
      lines = getLinesCountInVisualBlock(editor, range)
      col = editor.offsetToBufferPosition(range.startOffset).column
      if (caret.vimLastColumn == VimMotionGroupBase.LAST_COLUMN) {
        col = VimMotionGroupBase.LAST_COLUMN
      }
    }
    val after = range.endOffset >= editor.fileSize()
    val lp = editor.offsetToBufferPosition(injector.motion.moveCaretToCurrentLineStartSkipLeading(editor, caret))
    val res = deleteRange(editor, context, caret, range, type, true)
    val updatedCaret = editor.findLastVersionOfCaret(caret) ?: caret
    if (res) {
      if (type === SelectionType.LINE_WISE) {
        // Please don't use `getDocument().getText().isEmpty()` because it converts CharSequence into String
        if (editor.fileSize() == 0L) {
          insertBeforeCaret(editor, context)
        } else if (after && !editor.endsWithNewLine()) {
          insertNewLineBelow(editor, updatedCaret, lp.column)
        } else {
          insertNewLineAbove(editor, updatedCaret, lp.column)
        }
      } else {
        if (type === SelectionType.BLOCK_WISE) {
          setInsertRepeat(lines, col, false)
        }
        editor.vimChangeActionSwitchMode = Mode.INSERT
      }
    } else {
      insertBeforeCaret(editor, context)
    }
    return true
  }

  override fun reformatCodeMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val range = injector.motion.getMotionRange(
      editor, caret, context, argument,
      operatorArguments
    )
    return range != null && reformatCodeRange(editor, caret, range)
  }

  override fun reformatCodeSelection(editor: VimEditor, caret: VimCaret, range: VimSelection) {
    val textRange = range.toVimTextRange(true)
    reformatCodeRange(editor, caret, textRange)
  }

  private fun reformatCodeRange(editor: VimEditor, caret: VimCaret, range: TextRange): Boolean {
    val starts = range.startOffsets
    val ends = range.endOffsets
    val firstLine = editor.offsetToBufferPosition(range.startOffset).line
    for (i in ends.indices.reversed()) {
      val startOffset = editor.getLineStartForOffset(starts[i])
      val offset = ends[i] - if (startOffset == ends[i]) 0 else 1
      val endOffset = editor.getLineEndForOffset(offset)
      reformatCode(editor, startOffset, endOffset)
    }
    val newOffset = injector.motion.moveCaretToLineStartSkipLeading(editor, firstLine)
    caret.moveToOffset(newOffset)
    return true
  }

  override fun autoIndentMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ) {
    val range = injector.motion.getMotionRange(editor, caret, context, argument, operatorArguments)
    if (range != null) {
      autoIndentRange(
        editor, caret, context,
        TextRange(range.startOffset, range.endOffsetInclusive)
      )
    }
  }

  override fun indentLines(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    lines: Int,
    dir: Int,
    operatorArguments: OperatorArguments,
  ) {
    val start = caret.offset
    val end = injector.motion.moveCaretToRelativeLineEnd(editor, caret, lines - 1, true)
    indentRange(editor, caret, context, TextRange(start, end), 1, dir, operatorArguments)
  }

  override fun indentMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    dir: Int,
    operatorArguments: OperatorArguments,
  ) {
    val range = injector.motion.getMotionRange(editor, caret, context, argument, operatorArguments)
    if (range != null) {
      indentRange(editor, caret, context, range, 1, dir, operatorArguments)
    }
  }

  /**
   * Sort range of text with a given comparator
   *
   * @param editor         The editor to replace text in
   * @param range          The range to sort
   * @param lineComparator The comparator to use to sort
   * @param sortOptions     The option to sort the range
   * @return true if able to sort the text, false if not
   */
  override fun sortRange(
    editor: VimEditor, caret: VimCaret, range: LineRange, lineComparator: Comparator<String>,
    sortOptions: SortOption,
  ): Boolean {
    val startLine = range.startLine
    val endLine = range.endLine
    val count = range.size
    if (count < 2) {
      return false
    }
    val startOffset = editor.getLineStartOffset(startLine)
    val endOffset = editor.getLineEndOffset(endLine)

    val selectedText = editor.getText(startOffset, endOffset)
    val lines = selectedText.split("\n")
    val modifiedLines = sortOptions.pattern?.let {
      if (sortOptions.sortOnPattern) {
        extractPatternFromLines(editor, lines, startLine, it)
      } else {
        deletePatternFromLines(editor, lines, startLine, it)
      }
    } ?: lines
    val sortedLines = lines.zip(modifiedLines)
      .sortedWith { l1, l2 -> lineComparator.compare(l1.second, l2.second) }
      .map { it.first }
      .toMutableList()

    if (sortOptions.unique) {
      val iterator = sortedLines.iterator()
      var previous: String? = null
      while (iterator.hasNext()) {
        val current = iterator.next()
        if (current == previous || sortOptions.ignoreCase && current.equals(previous, ignoreCase = true)) {
          iterator.remove()
        } else {
          previous = current
        }
      }
    }
    if (sortedLines.isEmpty()) {
      return false
    }
    replaceText(editor, caret, startOffset, endOffset, sortedLines.joinToString("\n"))
    return true
  }

  private fun extractPatternFromLines(
    editor: VimEditor,
    lines: List<String>,
    startLine: Int,
    pattern: String,
  ): List<String> {
    val regex = VimRegex(pattern)
    return lines.mapIndexed { i: Int, line: String ->
      val result = regex.findInLine(editor, startLine + i, 0)
      when (result) {
        is VimMatchResult.Success -> result.value
        is VimMatchResult.Failure -> line
      }
    }
  }

  private fun deletePatternFromLines(
    editor: VimEditor,
    lines: List<String>,
    startLine: Int,
    pattern: String,
  ): List<String> {
    val regex = VimRegex(pattern)
    return lines.mapIndexed { i: Int, line: String ->
      val result = regex.findInLine(editor, startLine + i, 0)
      when (result) {
        is VimMatchResult.Success -> line.substring(result.value.length, line.length)
        is VimMatchResult.Failure -> line
      }
    }
  }

  override fun changeNumber(editor: VimEditor, caret: VimCaret, count: Int): Boolean {
    val nf: List<String> = injector.options(editor).nrformats
    val alpha = nf.contains("alpha")
    val hex = nf.contains("hex")
    val octal = nf.contains("octal")
    val range = findNumberUnderCursor(editor, caret, alpha, hex, octal)
    if (range == null) {
      logger.debug("no number on line")
      return false
    }
    val newNumber = changeNumberInRange(editor, range, count, alpha, hex, octal)
    return if (newNumber == null) {
      false
    } else {
      replaceText(editor, caret, range.first.startOffset, range.first.endOffset, newNumber)
      caret.moveToInlayAwareOffset(range.first.startOffset + newNumber.length - 1)
      true
    }
  }

  private fun changeNumberInRange(
    editor: VimEditor,
    range: Pair<TextRange, NumberType>,
    count: Int,
    alpha: Boolean,
    hex: Boolean,
    octal: Boolean,
  ): String? {
    val text = editor.getText(range.first)
    val numberType = range.second
    if (logger.isDebug()) {
      logger.debug("found range $range")
      logger.debug("text=$text")
    }
    var number = text
    if (text.isEmpty()) {
      return null
    }
    var ch = text[0]
    if (hex && NumberType.HEX == numberType) {
      if (!text.lowercase(Locale.getDefault()).startsWith(HEX_START)) {
        throw RuntimeException("Hex number should start with 0x: $text")
      }
      for (i in text.length - 1 downTo 2) {
        val index = "abcdefABCDEF".indexOf(text[i])
        if (index >= 0) {
          lastLower = index < 6
          break
        }
      }
      var num = BigInteger(text.substring(2), 16)
      num = num.add(BigInteger.valueOf(count.toLong()))
      if (num.compareTo(BigInteger.ZERO) < 0) {
        num = BigInteger(MAX_HEX_INTEGER, 16).add(BigInteger.ONE).add(num)
      }
      number = num.toString(16)
      number = number.padStart(text.length - 2, '0')
      if (!lastLower) {
        number = number.uppercase(Locale.getDefault())
      }
      number = text.substring(0, 2) + number
    } else if (octal && NumberType.OCT == numberType && text.length > 1) {
      if (!text.startsWith("0")) throw RuntimeException("Oct number should start with 0: $text")
      var num = BigInteger(text, 8).add(BigInteger.valueOf(count.toLong()))
      if (num.compareTo(BigInteger.ZERO) < 0) {
        num = BigInteger("1777777777777777777777", 8).add(BigInteger.ONE).add(num)
      }
      number = num.toString(8)
      number = "0" + number.padStart(text.length - 1, '0')
    } else if (alpha && NumberType.ALPHA == numberType) {
      if (!Character.isLetter(ch)) throw RuntimeException("Not alpha number : $text")
      ch += count.toChar().code
      if (Character.isLetter(ch)) {
        number = ch.toString()
      }
    } else if (NumberType.DEC == numberType) {
      if (ch != '-' && !Character.isDigit(ch)) throw RuntimeException("Not dec number : $text")
      var pad = ch == '0'
      var len = text.length
      if (ch == '-' && text[1] == '0') {
        pad = true
        len--
      }
      var num = BigInteger(text)
      num = num.add(BigInteger.valueOf(count.toLong()))
      number = num.toString()
      if (!octal && pad) {
        var neg = false
        if (number[0] == '-') {
          neg = true
          number = number.substring(1)
        }
        number = number.padStart(len, '0')
        if (neg) {
          number = "-$number"
        }
      }
    }
    return number
  }

  /**
   * Perform increment and decrement for numbers in visual mode
   *
   *
   * Flag [avalanche] marks if increment (or decrement) should be performed in avalanche mode
   * (for v_g_Ctrl-A and v_g_Ctrl-X commands)
   *
   * @return true
   */
  override fun changeNumberVisualMode(
    editor: VimEditor,
    caret: VimCaret,
    selectedRange: TextRange,
    count: Int,
    avalanche: Boolean,
  ): Boolean {

    val nf: List<String> = injector.options(editor).nrformats
    val alpha = nf.contains("alpha")
    val hex = nf.contains("hex")
    val octal = nf.contains("octal")
    val numberRanges = findNumbersInRange(editor, selectedRange, alpha, hex, octal)
    val newNumbers: MutableList<String?> = ArrayList()
    for (i in numberRanges.indices) {
      val numberRange = numberRanges[i]
      val iCount = if (avalanche) (i + 1) * count else count
      val newNumber = changeNumberInRange(editor, numberRange, iCount, alpha, hex, octal)
      newNumbers.add(newNumber)
    }
    for (i in newNumbers.indices.reversed()) {
      // Replace text bottom up. In other direction ranges will be desynchronized after inc numbers like 99
      val (first) = numberRanges[i]
      val newNumber = newNumbers[i]
      replaceText(editor, caret, first.startOffset, first.endOffset, newNumber!!)
    }
    caret.moveToInlayAwareOffset(selectedRange.startOffset)
    return true
  }

  private fun findNumberUnderCursor(
    editor: VimEditor,
    caret: VimCaret,
    alpha: Boolean,
    hex: Boolean,
    octal: Boolean,
  ): Pair<TextRange, NumberType>? {
    val lline = caret.getBufferPosition().line
    val text = editor.getLineText(lline).lowercase(Locale.getDefault())
    val startLineOffset = editor.getLineStartOffset(lline)
    val posOnLine = caret.offset - startLineOffset

    val numberTextRange = findNumberInText(text, posOnLine, alpha, hex, octal) ?: return null

    return Pair(
      TextRange(
        numberTextRange.first.startOffset + startLineOffset,
        numberTextRange.first.endOffset + startLineOffset
      ),
      numberTextRange.second
    )
  }

  fun findNumbersInRange(
    editor: VimEditor,
    textRange: TextRange,
    alpha: Boolean,
    hex: Boolean,
    octal: Boolean,
  ): List<Pair<TextRange, NumberType>> {
    val result: MutableList<Pair<TextRange, NumberType>> = ArrayList()


    for (i in 0 until textRange.size()) {
      val startOffset = textRange.startOffsets[i]
      val end = textRange.endOffsets[i]
      val text: String = editor.getText(startOffset, end)
      val textChunks = text.split("\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      var chunkStart = 0
      for (chunk in textChunks) {
        val number = findNumberInText(chunk, 0, alpha, hex, octal)

        if (number != null) {
          result.add(
            Pair(
              TextRange(
                number.first.startOffset + startOffset + chunkStart,
                number.first.endOffset + startOffset + chunkStart
              ),
              number.second
            )
          )
        }
        chunkStart += 1 + chunk.length
      }
    }
    return result
  }

  /**
   * Search for number in given text from start position
   *
   * @param textInRange    - text to search in
   * @param startPosOnLine - start offset to search
   * @return - text range with number
   */
  protected fun findNumberInText(
    textInRange: String,
    startPosOnLine: Int,
    alpha: Boolean,
    hex: Boolean,
    octal: Boolean,
  ): Pair<TextRange, NumberType>? {
    if (logger.isDebug()) {
      logger.debug("text=$textInRange")
    }

    var pos = startPosOnLine
    val lineEndOffset = textInRange.length

    while (true) {
      // Skip over current whitespace if any
      while (pos < lineEndOffset && !isNumberChar(textInRange[pos], alpha, hex, octal, true)) {
        pos++
      }

      if (logger.isDebug()) logger.debug("pos=$pos")
      if (pos >= lineEndOffset) {
        logger.debug("no number char on line")
        return null
      }

      val isHexChar = "abcdefABCDEF".indexOf(textInRange[pos]) >= 0

      if (hex) {
        // Ox and OX handling
        if (textInRange[pos] == '0' && pos < lineEndOffset - 1 && "xX".indexOf(textInRange[pos + 1]) >= 0) {
          pos += 2
        } else if ("xX".indexOf(textInRange[pos]) >= 0 && pos > 0 && textInRange[pos - 1] == '0') {
          pos++
        }

        logger.debug("checking hex")
        val range = findRange(textInRange, pos, false, true, false, false)
        val start = range.first
        val end = range.second

        // Ox and OX
        if (start >= 2 && textInRange.substring(start - 2, start).equals("0x", ignoreCase = true)) {
          logger.debug("found hex")
          return Pair(TextRange(start - 2, end), NumberType.HEX)
        }

        if (!isHexChar || alpha) {
          break
        } else {
          pos++
        }
      } else {
        break
      }
    }

    if (octal) {
      logger.debug("checking octal")
      val range = findRange(textInRange, pos, false, false, true, false)
      val start = range.first
      val end = range.second

      if (end - start == 1 && textInRange[start] == '0') {
        return Pair(TextRange(start, end), NumberType.DEC)
      }
      if (textInRange[start] == '0' && end > start &&
        !(start > 0 && isNumberChar(textInRange[start - 1], false, false, false, true))
      ) {
        logger.debug("found octal")
        return Pair(TextRange(start, end), NumberType.OCT)
      }
    }

    if (alpha) {
      if (logger.isDebug()) logger.debug("checking alpha for " + textInRange[pos])
      if (isNumberChar(textInRange[pos], true, false, false, false)) {
        if (logger.isDebug()) logger.debug("found alpha at $pos")
        return Pair(TextRange(pos, pos + 1), NumberType.ALPHA)
      }
    }

    val range = findRange(textInRange, pos, false, false, false, true)
    var start = range.first
    val end = range.second
    if (start > 0 && textInRange[start - 1] == '-') {
      start--
    }

    return Pair(TextRange(start, end), NumberType.DEC)
  }

  /**
   * Searches for digits block that matches parameters
   */
  private fun findRange(
    text: String,
    pos: Int,
    alpha: Boolean,
    hex: Boolean,
    octal: Boolean,
    decimal: Boolean,
  ): Pair<Int, Int> {
    var end = pos
    while (end < text.length && isNumberChar(text[end], alpha, hex, octal, decimal || octal)) {
      end++
    }
    var start = pos
    while (start >= 0 && isNumberChar(text[start], alpha, hex, octal, decimal || octal)) {
      start--
    }
    if (start < end &&
      (start == -1 ||
        0 <= start && start < text.length &&
        !isNumberChar(text[start], alpha, hex, octal, decimal || octal))
    ) {
      start++
    }
    if (octal) {
      for (i in start until end) {
        if (!isNumberChar(text[i], false, false, true, false)) return Pair(0, 0)
      }
    }
    return Pair(start, end)
  }

  private fun isNumberChar(ch: Char, alpha: Boolean, hex: Boolean, octal: Boolean, decimal: Boolean): Boolean {
    return if (alpha && ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))) {
      true
    } else if (octal && (ch >= '0' && ch <= '7')) {
      true
    } else if (hex && ((ch >= '0' && ch <= '9') || "abcdefABCDEF".indexOf(ch) >= 0)) {
      true
    } else {
      decimal && (ch >= '0' && ch <= '9')
    }
  }

  /**
   * Changes the case of all the characters in the range
   *
   * @param editor The editor to change
   * @param caret  The caret to be moved
   * @param range  The range to change
   * @param type   The case change type (TOGGLE, UPPER, LOWER)
   * @return true if able to delete the text, false if not
   */
  override fun changeCaseRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    type: VimChangeGroup.ChangeCaseType,
  ): Boolean {
    val starts = range.startOffsets
    val ends = range.endOffsets
    for (i in ends.indices.reversed()) {
      changeCase(editor, caret, starts[i], ends[i], type)
    }
    caret.moveToOffset(range.startOffset)
    return true
  }

  /**
   * Changes the case of all the character moved over by the motion argument.
   *
   * @param editor   The editor to change
   * @param caret    The caret on which motion pretends to be performed
   * @param context  The data context
   * @param type     The case change type (TOGGLE, UPPER, LOWER)
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  override fun changeCaseMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext?,
    type: VimChangeGroup.ChangeCaseType,
    argument: Argument.Motion,
    operatorArguments: OperatorArguments,
  ): Boolean {
    var range = injector.motion.getMotionRange(
      editor, caret, context!!, argument, operatorArguments
    )
    if (range == null) return false

    // If the motion is linewise, we need to adjust range.startOffset to match the observed Vim behavior
    if (argument.isLinewiseMotion()) {
      val pos = editor.offsetToBufferPosition(range.startOffset)
      // The leftmost non-whitespace character OR the current caret position, whichever is closer to the left
      val start = editor.getLeadingCharacterOffset(pos.line).coerceAtMost(caret.offset)
      range = TextRange(start, range.endOffset)
    }
    return changeCaseRange(editor, caret, range, type)
  }

  /**
   * Toggles the case of count characters
   *
   * @param editor The editor to change
   * @param caret  The caret on which the operation is performed
   * @param count  The number of characters to change
   * @return true if able to change count characters
   */
  override fun changeCaseToggleCharacter(editor: VimEditor, caret: VimCaret, count: Int): Boolean {
    val allowWrap = injector.options(editor).whichwrap.contains("~")
    var motion = injector.motion.getHorizontalMotion(editor, caret, count, true, allowWrap)
    if (motion is Motion.Error) return false
    changeCase(editor, caret, caret.offset, (motion as AbsoluteOffset).offset, VimChangeGroup.ChangeCaseType.TOGGLE)
    motion = injector.motion.getHorizontalMotion(
      editor,
      caret,
      count,
      false,
      allowWrap
    ) // same but without allow end because we can change till end, but can't move caret there
    if (motion is AbsoluteOffset) {
      caret.moveToOffset(editor.normalizeOffset(motion.offset, false))
    }
    return true
  }

  /**
   * This performs the actual case change.
   *
   * @param editor The editor to change
   * @param start  The start offset to change
   * @param end    The end offset to change
   * @param type   The type of change (TOGGLE, UPPER, LOWER)
   */
  private fun changeCase(
    editor: VimEditor,
    caret: VimCaret,
    start: Int,
    end: Int,
    type: VimChangeGroup.ChangeCaseType,
  ) {
    var (newStart, newEnd) = if (start > end) end to start else start to end
    newEnd = editor.normalizeOffset(newEnd, true)
    val changedText = buildString {
      for (i in newStart until newEnd) {
        append(changeCase(editor.text()[i], type))
      }
    }
    replaceText(editor, caret, newStart, newEnd, changedText)
  }

  override fun indentRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: TextRange,
    count: Int,
    dir: Int,
    operatorArguments: OperatorArguments,
  ) {
    if (logger.isDebug()) {
      logger.debug("count=$count")
    }

    // Remember the current caret column
    val intendedColumn = caret.vimLastColumn
    val indentConfig = editor.indentConfig
    val sline = editor.offsetToBufferPosition(range.startOffset).line
    val endLogicalPosition = editor.offsetToBufferPosition(range.endOffset)
    val eline = if (endLogicalPosition.column == 0) max((endLogicalPosition.line - 1).toDouble(), 0.0)
      .toInt() else endLogicalPosition.line
    if (range.isMultiple) {
      val from = editor.offsetToBufferPosition(range.startOffset).column
      if (dir == 1) {
        // Right shift blockwise selection
        val indent = indentConfig.createIndentByDepth(count)
        for (l in sline..eline) {
          val len = editor.lineLength(l)
          if (len > from) {
            val spos = BufferPosition(l, from, false)
            insertText(editor, caret, spos, indent)
          }
        }
      } else {
        // Left shift blockwise selection
        val chars = editor.text()
        for (l in sline..eline) {
          val len = editor.lineLength(l)
          if (len > from) {
            val spos = BufferPosition(l, from, false)
            val epos = BufferPosition(l, from + indentConfig.getIndentSize(count) - 1, false)
            val wsoff = editor.bufferPositionToOffset(spos)
            val weoff = editor.bufferPositionToOffset(epos)
            var pos: Int
            pos = wsoff
            while (pos <= weoff) {
              if (charType(editor, chars[pos], false) !== CharacterHelper.CharacterType.WHITESPACE) {
                break
              }
              pos++
            }
            if (pos > wsoff) {
              deleteText(editor, context, TextRange(wsoff, pos), null, caret, true)
            }
          }
        }
      }
    } else {
      // Shift non-blockwise selection
      for (l in sline..eline) {
        val soff = editor.getLineStartOffset(l)
        val eoff = editor.getLineEndOffset(l, true)
        val woff = injector.motion.moveCaretToLineStartSkipLeading(editor, l)
        val col = editor.offsetToBufferPosition(woff).column
        val limit = max(0.0, (col + dir * indentConfig.getIndentSize(count)).toDouble())
          .toInt()
        if (col > 0 || soff != eoff) {
          val indent = indentConfig.createIndentBySize(limit)
          replaceText(editor, caret, soff, woff, indent)
        }
      }
    }
    if (editor.mode != Mode.INSERT) {
      if (!range.isMultiple) {
        // The caret has moved, so reset the intended column before trying to get the expected offset
        val newCaret = caret.setVimLastColumnAndGetCaret(intendedColumn)
        val offset = injector.motion.moveCaretToLineWithStartOfLineOption(editor, sline, caret)
        newCaret.moveToOffset(offset)
      } else {
        caret.moveToOffset(range.startOffset)
      }
    }
  }

  override fun initBlockInsert(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    append: Boolean,
  ): Boolean {
    val lines = getLinesCountInVisualBlock(editor, range)
    val startPosition = editor.offsetToBufferPosition(range.startOffset)
    // Note that when called, we're likely to have moved from Visual (block) to Normal, which means all secondary carets
    // will have been removed. Even if not, this would move them all to the same location, which would remove them and
    // leave only the primary caret.
    for (caret in editor.carets()) {
      val line = startPosition.line
      var column = startPosition.column
      if (append) {
        column += range.maxLength
        if (caret.vimLastColumn == VimMotionGroupBase.LAST_COLUMN) {
          column = VimMotionGroupBase.LAST_COLUMN
        }
      }
      val lineLength = editor.lineLength(line)
      if (column < VimMotionGroupBase.LAST_COLUMN && lineLength < column) {
        val pad = injector.engineEditorHelper.pad(editor, line, column)
        val offset = editor.getLineEndOffset(line)
        insertText(editor, caret, offset, pad)
      }
      caret.moveToInlayAwareOffset(editor.bufferPositionToOffset(BufferPosition(line, column)))
      setInsertRepeat(lines, column, append)
    }
    insertBeforeCaret(editor, context)
    return true
  }

  override fun reset() {
    strokes.clear()
    repeatCharsCount = 0
    if (lastStrokes != null) {
      lastStrokes!!.clear()
    }
  }

  override fun saveStrokes(newStrokes: String?) {
    val chars = newStrokes!!.toCharArray()
    strokes.add(chars)
  }

  @TestOnly
  override fun resetRepeat() {
    setInsertRepeat(0, 0, false)
  }

  companion object {
    private const val MAX_REPEAT_CHARS_COUNT = 10000
    private val logger = vimLogger<VimChangeGroupBase>()

    /**
     * Counts number of lines in the visual block.
     *
     *
     * The result includes empty and short lines which does not have explicit start position (caret).
     *
     * @param editor The editor the block was selected in
     * @param range  The range corresponding to the selected block
     * @return total number of lines
     */
    fun getLinesCountInVisualBlock(editor: VimEditor, range: TextRange): Int {
      val startOffsets = range.startOffsets
      if (startOffsets.isEmpty()) return 0
      val firstStart = editor.offsetToBufferPosition(startOffsets[0])
      val lastStart = editor.offsetToBufferPosition(startOffsets[range.size() - 1])
      return lastStart.line - firstStart.line + 1
    }

    const val HEX_START: @NonNls String = "0x"
    const val VIM_MOTION_BIG_WORD_RIGHT: String = "VimMotionBigWordRightAction"
    const val VIM_MOTION_WORD_RIGHT: String = "VimMotionWordRightAction"
    const val VIM_MOTION_CAMEL_RIGHT: String = "VimMotionCamelRightAction"
    const val VIM_MOTION_WORD_END_RIGHT: String = "VimMotionWordEndRightAction"
    const val VIM_MOTION_BIG_WORD_END_RIGHT: String = "VimMotionBigWordEndRightAction"
    const val VIM_MOTION_CAMEL_END_RIGHT: String = "VimMotionCamelEndRightAction"
    const val MAX_HEX_INTEGER: @NonNls String = "ffffffffffffffff"
    val wordMotions: Set<String> =
      setOf(VIM_MOTION_WORD_RIGHT, VIM_MOTION_BIG_WORD_RIGHT, VIM_MOTION_CAMEL_RIGHT)
  }

  /**
   * Changes the case of the supplied character based on the supplied change type
   *
   * @param ch   The character to change
   * @param type One of `CASE_TOGGLE`, `CASE_UPPER`, or `CASE_LOWER`
   * @return The character with changed case or the original if not a letter
   */
  private fun changeCase(ch: Char, type: VimChangeGroup.ChangeCaseType): Char = when (type) {
    VimChangeGroup.ChangeCaseType.TOGGLE -> when {
      Character.isLowerCase(ch) -> Character.toUpperCase(ch)
      Character.isUpperCase(ch) -> Character.toLowerCase(ch)
      else -> ch
    }

    VimChangeGroup.ChangeCaseType.LOWER -> Character.toLowerCase(ch)
    VimChangeGroup.ChangeCaseType.UPPER -> Character.toUpperCase(ch)
  }
}
