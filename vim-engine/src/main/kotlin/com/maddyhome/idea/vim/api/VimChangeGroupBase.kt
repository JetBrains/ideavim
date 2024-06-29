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
import com.maddyhome.idea.vim.common.OperatedRange
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.handler.Motion.AbsoluteOffset
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.helper.usesVirtualSpace
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_END
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_START
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_INSERTED_TEXT_REGISTER
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.toReturnTo
import org.jetbrains.annotations.NonNls
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke
import kotlin.math.abs
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

  // Workaround for VIM-1546. Another solution is highly appreciated.
  var tabAction: Boolean = false

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
    caret: VimCaret,
    count: Int,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val endOffset = injector.motion.getHorizontalMotion(editor, caret, count, true)
    if (endOffset is AbsoluteOffset) {
      val res = deleteText(
        editor,
        TextRange(caret.offset, endOffset.offset),
        SelectionType.CHARACTER_WISE,
        caret,
        operatorArguments,
      )
      val pos = caret.offset
      val norm = editor.normalizeOffset(caret.getBufferPosition().line, pos, isChange)
      if (norm != pos ||
        editor.offsetToVisualPosition(norm) !==
        injector.engineEditorHelper.inlayAwareOffsetToVisualPosition(editor, norm)
      ) {
        caret.moveToOffset(norm)
      }
      // Always move the caret. Our position might or might not have changed, but an inlay might have been moved to our
      // location, or deleting the character(s) might have caused us to scroll sideways in long files. Moving the caret
      // will make sure it's in the right place, and visible
      val offset = editor.normalizeOffset(
        caret.getBufferPosition().line,
        caret.offset,
        isChange,
      )
      caret.moveToOffset(offset)
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
    range: TextRange,
    type: SelectionType?,
    caret: VimCaret,
    operatorArguments: OperatorArguments,
    saveToRegister: Boolean = true,
  ): Boolean {
    var updatedRange = range
    // Fix for https://youtrack.jetbrains.net/issue/VIM-35
    if (!range.normalize(editor.fileSize().toInt())) {
      updatedRange = if (range.startOffset == range.endOffset && range.startOffset == editor.fileSize()
          .toInt() && range.startOffset != 0
      ) {
        TextRange(range.startOffset - 1, range.endOffset)
      } else {
        return false
      }
    }
    val mode = operatorArguments.mode
    if (type == null ||
      (mode == Mode.INSERT || mode == Mode.REPLACE) ||
      !saveToRegister ||
      caret.registerStorage.storeText(editor, updatedRange, type, true)
    ) {
      val startOffsets = updatedRange.startOffsets
      val endOffsets = updatedRange.endOffsets
      for (i in updatedRange.size() - 1 downTo 0) {
        val (newRange, _) = editor.search(
          startOffsets[i] to endOffsets[i],
          editor,
          LineDeleteShift.NL_ON_END
        ) ?: continue
        editor.deleteString(TextRange(newRange.first, newRange.second))
      }
      if (type != null) {
        val start = updatedRange.startOffset
        injector.markService.setMark(caret, MARK_CHANGE_POS, start)
        injector.markService.setChangeMarks(caret, TextRange(start, start + 1))
      }
      return true
    }
    return false
  }

  /**
   * Inserts text into the document
   *
   * @param editor The editor to insert into
   * @param caret  The caret to start insertion in
   * @param str    The text to insert
   */
  override fun insertText(editor: VimEditor, caret: VimCaret, offset: Int, str: String): VimCaret {
    (editor as MutableVimEditor).insertText(caret, offset, str)
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
    operatorArguments: OperatorArguments,
  ) {
    val myLastStrokes = lastStrokes ?: return
    for (caret in editor.nativeCarets()) {
      for (i in 0 until count) {
        for (lastStroke in myLastStrokes) {
          when (lastStroke) {
            is NativeAction -> {
              injector.actionExecutor.executeAction(editor, lastStroke, context)
              strokes.add(lastStroke)
            }

            is EditorActionHandlerBase -> {
              injector.actionExecutor.executeVimAction(editor, lastStroke, context, operatorArguments)
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
    operatorArguments: OperatorArguments,
  ) {
    for (caret in editor.nativeCarets()) {
      if (repeatLines > 0) {
        val visualLine = caret.getVisualPosition().line
        val bufferLine = caret.getBufferPosition().line
        val position = editor.bufferPositionToOffset(BufferPosition(bufferLine, repeatColumn, false))
        for (i in 0 until repeatLines) {
          if (repeatAppend &&
            (repeatColumn < VimMotionGroupBase.LAST_COLUMN) &&
            (editor.getVisualLineLength(visualLine + i) < repeatColumn)
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
            repeatInsertText(editor, context, updatedCount, operatorArguments)
          } else if (editor.getVisualLineLength(visualLine + i) >= repeatColumn) {
            val visualPosition = VimVisualPosition(visualLine + i, repeatColumn, false)
            val inlaysCount = injector.engineEditorHelper.amountOfInlaysBeforeVisualPosition(editor, visualPosition)
            caret.moveToVisualPosition(VimVisualPosition(visualLine + i, repeatColumn + inlaysCount, false))
            repeatInsertText(editor, context, updatedCount, operatorArguments)
          }
        }
        caret.moveToOffset(position)
      } else {
        repeatInsertText(editor, context, count, operatorArguments)
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
      if (repeatCharsCount > Companion.MAX_REPEAT_CHARS_COUNT) {
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
      if (!tabAction && newFragmentLength > 1 && newFragment.trim { it <= ' ' }.isEmpty()) {
        return
      }
      tabAction = false
      strokes.addAll(getAdjustCaretActions(change))
      if (oldFragmentLength > 0) {
        val editorDelete = injector.nativeActionManager.deleteAction
        if (editorDelete != null) {
          for (i in 0 until oldFragmentLength) {
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
        for (i in 0 until count) {
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
  override fun insertBeforeCursor(editor: VimEditor, context: ExecutionContext) {
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
  override fun insertAfterCursor(editor: VimEditor, context: ExecutionContext) {
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
    for (caret in editor.nativeCarets()) {
      caret.vimInsertStart = editor.createLiveMarker(caret.offset, caret.offset)
      injector.markService.setMark(caret, MARK_CHANGE_START, caret.offset)
    }
    val cmd = state.executingCommand
    if (cmd != null && state.isDotRepeatInProgress) {
      editor.mode = mode
      if (mode == Mode.REPLACE) {
        editor.insertMode = false
      }
      if (cmd.flags.contains(CommandFlags.FLAG_NO_REPEAT_INSERT)) {
        val commandState = injector.vimState
        repeatInsert(
          editor,
          context,
          1,
          false,
          OperatorArguments(false, 1, commandState.mode),
        )
      } else {
        val commandState = injector.vimState
        repeatInsert(
          editor,
          context,
          cmd.count,
          false,
          OperatorArguments(false, cmd.count, commandState.mode),
        )
      }
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
      oldOffset = editor.currentCaret().offset
      editor.insertMode = mode == Mode.INSERT
      editor.mode = mode
    }
    notifyListeners(editor)
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
          val nanoTime = System.nanoTime()
          editor.forEachCaret { undo.startInsertSequence(it, it.offset, nanoTime) }
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
    repeatInsertText(editor, context, 1, operatorArguments)
    if (exit) {
      editor.exitInsertMode(context, operatorArguments)
    }
  }

  /**
   * Terminate insert/replace mode after the user presses Escape or Ctrl-C
   *
   *
   * DEPRECATED. Please, don't use this function directly. Use ModeHelper.exitInsertMode in file ModeExtensions.kt
   */
  override fun processEscape(editor: VimEditor, context: ExecutionContext?, operatorArguments: OperatorArguments) {
    // Get the offset for marks before we exit insert mode - switching from insert to overtype subtracts one from the
    // column offset.
    val markGroup = injector.markService
    markGroup.setMark(editor, VimMarkService.INSERT_EXIT_MARK)
    markGroup.setMark(editor, MARK_CHANGE_END)
    if (editor.mode is Mode.REPLACE) {
      editor.insertMode = true
    }
    var cnt = if (lastInsert != null) lastInsert!!.count else 0
    if (lastInsert != null && lastInsert!!.flags.contains(CommandFlags.FLAG_NO_REPEAT_INSERT)) {
      cnt = 1
    }
    if (vimDocument != null && vimDocumentListener != null) {
      vimDocument!!.removeChangeListener(vimDocumentListener!!)
      vimDocumentListener = null
    }
    lastStrokes = ArrayList(strokes)
    if (context != null) {
      injector.changeGroup.repeatInsert(editor, context, if (cnt == 0) 0 else cnt - 1, true, operatorArguments)
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
    editor.mode = Mode.NORMAL(returnTo = editor.mode.toReturnTo)
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
      val res = deleteText(editor, rangeToDelete, SelectionType.CHARACTER_WISE, caret, operatorArguments)
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
    caret: VimCaret,
    count: Int,
    spaces: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean {
    var myCount = count
    if (myCount < 2) myCount = 2
    val lline = caret.getBufferPosition().line
    val total = editor.lineCount()
    return if (lline + myCount > total) {
      false
    } else {
      deleteJoinNLines(editor, caret, lline, myCount, spaces, operatorArguments)
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
      processResultBuilder.addExecutionStep { _, lambdaEditor, lambdaContext -> type(lambdaEditor, lambdaContext, key.keyChar) }
      return true
    }

    // Shift-space
    if (key.keyCode == 32 && key.modifiers and KeyEvent.SHIFT_DOWN_MASK != 0) {
      processResultBuilder.addExecutionStep { _, lambdaEditor, lambdaContext -> type(lambdaEditor, lambdaContext, ' ') }
      return true
    }
    return false
  }

  override fun processKeyInSelectMode(
    editor: VimEditor,
    key: KeyStroke,
    processResultBuilder: KeyProcessResult.KeyProcessResultBuilder
  ): Boolean {
    var res: Boolean
    SelectionVimListenerSuppressor.lock().use {
      res = processKey(editor, key, processResultBuilder)
      processResultBuilder.addExecutionStep { _, lambdaEditor, lambdaContext ->
        lambdaEditor.exitSelectModeNative(false)
        KeyHandler.getInstance().reset(lambdaEditor)
        if (isPrintableChar(key.keyChar) || activeTemplateWithLeftRightMotion(lambdaEditor, key)) {
          injector.changeGroup.insertBeforeCursor(lambdaEditor, lambdaContext)
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
      val res = deleteText(editor, TextRange(start, offset), SelectionType.LINE_WISE, caret, operatorArguments)
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
    for (i in 0 until executions) {
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
    caret: VimCaret,
    range: TextRange,
    spaces: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val startLine = editor.offsetToBufferPosition(range.startOffset).line
    val endLine = editor.offsetToBufferPosition(range.endOffset).line
    var count = endLine - startLine + 1
    if (count < 2) count = 2
    return deleteJoinNLines(editor, caret, startLine, count, spaces, operatorArguments)
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
    val range = injector.motion.getMotionRange(editor, caret, context, argument, operatorArguments) ?: return null

    // Delete motion commands that are not linewise become linewise if all the following are true:
    // 1) The range is across multiple lines
    // 2) There is only whitespace before the start of the range
    // 3) There is only whitespace after the end of the range
    var type: SelectionType = if (argument.motion.isLinewiseMotion()) {
      SelectionType.LINE_WISE
    } else {
      SelectionType.CHARACTER_WISE
    }
    val motion = argument.motion
    if (!isChange && !motion.isLinewiseMotion()) {
      val start = editor.offsetToBufferPosition(range.startOffset)
      val end = editor.offsetToBufferPosition(range.endOffset)
      if (start.line != end.line) {
        if (!editor.anyNonWhitespace(range.startOffset, -1) && !editor.anyNonWhitespace(range.endOffset, 1)) {
          type = SelectionType.LINE_WISE
        }
      }
    }
    return Pair(range, type)
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
    caret: VimCaret,
    range: TextRange,
    type: SelectionType?,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
    saveToRegister: Boolean,
  ): Boolean {
    val intendedColumn = caret.vimLastColumn

    val removeLastNewLine = removeLastNewLine(editor, range, type)
    val res = deleteText(editor, range, type, caret, operatorArguments, saveToRegister)
    var processedCaret = editor.findLastVersionOfCaret(caret) ?: caret
    if (removeLastNewLine) {
      val textLength = editor.fileSize().toInt()
      editor.deleteString(TextRange(textLength - 1, textLength))
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
    caret: VimCaret,
    count: Int,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val res = deleteEndOfLine(editor, caret, count, operatorArguments)
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
  override fun changeCharacters(editor: VimEditor, caret: VimCaret, operatorArguments: OperatorArguments): Boolean {
    val count = operatorArguments.count1
    // TODO  is it correct to use primary caret? There is a caret as an argument
    val len = editor.lineLength(editor.primaryCaret().getBufferPosition().line)
    val col = caret.getBufferPosition().column
    if (col + count >= len) {
      return changeEndOfLine(editor, caret, 1, operatorArguments)
    }
    val res = deleteCharacter(editor, caret, count, true, operatorArguments)
    if (res) {
      editor.vimChangeActionSwitchMode = Mode.INSERT
    }
    return res
  }

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
    caret: VimCaret,
    startLine: Int,
    count: Int,
    spaces: Boolean,
    operatorArguments: OperatorArguments,
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
      deleteText(editor, TextRange(startOffset, endOffset), null, caret, operatorArguments)
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
    (editor as MutableVimEditor).replaceString(start, end, str)

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
    val motion = argument.motion
    val id = motion.action.id
    var kludge = false
    val bigWord = id == VIM_MOTION_BIG_WORD_RIGHT
    val chars = editor.text()
    val offset = caret.offset
    val fileSize = editor.fileSize().toInt()
    if (fileSize > 0 && offset < fileSize) {
      val charType = charType(editor, chars[offset], bigWord)
      if (charType !== CharacterHelper.CharacterType.WHITESPACE) {
        val lastWordChar = offset >= fileSize - 1 || charType(editor, chars[offset + 1], bigWord) !== charType
        if (wordMotions.contains(id) && lastWordChar && motion.count == 1) {
          val res = deleteCharacter(editor, caret, 1, true, operatorArguments)
          if (res) {
            editor.vimChangeActionSwitchMode = Mode.INSERT
          }
          return res
        }
        when (id) {
          VIM_MOTION_WORD_RIGHT -> {
            kludge = true
            motion.action = injector.actionExecutor.findVimActionOrDie(VIM_MOTION_WORD_END_RIGHT)
          }

          VIM_MOTION_BIG_WORD_RIGHT -> {
            kludge = true
            motion.action = injector.actionExecutor.findVimActionOrDie(VIM_MOTION_BIG_WORD_END_RIGHT)
          }

          VIM_MOTION_CAMEL_RIGHT -> {
            kludge = true
            motion.action = injector.actionExecutor.findVimActionOrDie(VIM_MOTION_CAMEL_END_RIGHT)
          }
        }
      }
    }
    if (kludge) {
      val cnt = operatorArguments.count1 * motion.count
      val pos1 = injector.searchHelper.findNextWordEnd(editor, offset, cnt, bigWord, false)
      val pos2 = injector.searchHelper.findNextWordEnd(editor, pos1, -cnt, bigWord, false)
      if (logger.isDebug()) {
        logger.debug("pos=$offset")
        logger.debug("pos1=$pos1")
        logger.debug("pos2=$pos2")
        logger.debug("count=" + operatorArguments.count1)
        logger.debug("arg.count=" + motion.count)
      }
      if (pos2 == offset) {
        if (operatorArguments.count1 > 1) {
          count0--
        } else if (motion.count > 1) {
          motion.rawCount = motion.count - 1
        } else {
          motion.flags = EnumSet.noneOf(CommandFlags::class.java)
        }
      }
    }
    val (first, second) = getDeleteRangeAndType(
      editor,
      caret,
      context,
      argument,
      true,
      operatorArguments.withCount0(count0),
    ) ?: return false
    return changeRange(
      editor,
      caret,
      first,
      second,
      context,
      operatorArguments,
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
   * @param operatorArguments
   * @return true if able to delete the range, false if not
   */
  override fun changeRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
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
    val res = deleteRange(editor, caret, range, type, true, operatorArguments)
    val updatedCaret = editor.findLastVersionOfCaret(caret) ?: caret
    if (res) {
      if (type === SelectionType.LINE_WISE) {
        // Please don't use `getDocument().getText().isEmpty()` because it converts CharSequence into String
        if (editor.fileSize() == 0L) {
          insertBeforeCursor(editor, context)
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
      insertBeforeCursor(editor, context)
    }
    return true
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
}

fun OperatedRange.toType(): SelectionType = when (this) {
  is OperatedRange.Characters -> SelectionType.CHARACTER_WISE
  is OperatedRange.Lines -> SelectionType.LINE_WISE
  is OperatedRange.Block -> SelectionType.BLOCK_WISE
}
