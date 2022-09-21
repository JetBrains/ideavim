package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.VimStateMachine.Companion.getInstance
import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.OperatedRange
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inSingleCommandMode
import com.maddyhome.idea.vim.helper.usesVirtualSpace
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_END
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_START
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_INSERTED_TEXT_REGISTER
import java.awt.event.KeyEvent
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
  protected var repeatCharsCount = 0

  @JvmField
  protected var lastStrokes: MutableList<Any>? = null

  @JvmField
  protected var oldOffset = -1

  // Workaround for VIM-1546. Another solution is highly appreciated.
  var tabAction = false

  @JvmField
  protected var vimDocumentListener: ChangesListener? = null

  @JvmField
  protected var lastLower = true

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
    operatorArguments: OperatorArguments
  ): Boolean {
    val endOffset = injector.motion.getOffsetOfHorizontalMotion(editor, caret, count, true)
    if (endOffset != -1) {
      val res = deleteText(
        editor,
        TextRange(caret.offset.point, endOffset),
        SelectionType.CHARACTER_WISE,
        caret,
        operatorArguments
      )
      val pos = caret.offset.point
      val norm = injector.engineEditorHelper.normalizeOffset(editor, caret.getLogicalPosition().line, pos, isChange)
      if (norm != pos ||
        editor.offsetToVisualPosition(norm) !==
        injector.engineEditorHelper.inlayAwareOffsetToVisualPosition(editor, norm)
      ) {
        injector.motion.moveCaret(editor, caret, norm)
      }
      // Always move the caret. Our position might or might not have changed, but an inlay might have been moved to our
      // location, or deleting the character(s) might have caused us to scroll sideways in long files. Moving the caret
      // will make sure it's in the right place, and visible
      val offset = injector.engineEditorHelper.normalizeOffset(
        editor,
        caret.getLogicalPosition().line,
        caret.offset.point,
        isChange
      )
      injector.motion.moveCaret(editor, caret, offset)
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
   * @return true if able to delete the text, false if not
   */
  protected fun deleteText(
    editor: VimEditor,
    range: TextRange,
    type: SelectionType?,
    caret: VimCaret,
    operatorArguments: OperatorArguments,
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
    if (type == null ||
      operatorArguments.mode.inInsertMode || caret.registerStorage.storeText(caret, editor, updatedRange, type, true) ||
      caret != editor.primaryCaret() // sticky tape for VIM-2703 todo remove in the next release
    ) {
      val startOffsets = updatedRange.startOffsets
      val endOffsets = updatedRange.endOffsets
      for (i in updatedRange.size() - 1 downTo 0) {
        editor.deleteString(TextRange(startOffsets[i], endOffsets[i]))
      }
      if (type != null) {
        val start = updatedRange.startOffset
        if (editor.primaryCaret() == caret) {
          injector.markGroup.setMark(editor, MARK_CHANGE_POS, start)
          injector.markGroup.setChangeMarks(editor, TextRange(start, start + 1))
        }
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
  override fun insertText(editor: VimEditor, caret: VimCaret, offset: Int, str: String) {
    (editor as MutableVimEditor).insertText(Offset(offset), str)
    caret.moveToInlayAwareOffset(offset + str.length)

    injector.markGroup.setMark(editor, MARK_CHANGE_POS, offset)
  }

  override fun insertText(editor: VimEditor, caret: VimCaret, str: String) {
    insertText(editor, caret, caret.offset.point, str)
  }

  open fun insertText(editor: VimEditor, caret: VimCaret, start: VimLogicalPosition, str: String) {
    insertText(editor, caret, editor.logicalPositionToOffset(start), str)
  }

  /**
   * This repeats the previous insert count times
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
              injector.actionExecutor.executeAction(lastStroke, context)
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
        val logicalLine = caret.getLogicalPosition().line
        val position = editor.logicalPositionToOffset(VimLogicalPosition(logicalLine, repeatColumn, false))
        for (i in 0 until repeatLines) {
          if (repeatAppend &&
            (repeatColumn < VimMotionGroupBase.LAST_COLUMN) &&
            (injector.engineEditorHelper.getVisualLineLength(editor, visualLine + i) < repeatColumn)
          ) {
            val pad = injector.engineEditorHelper.pad(editor, context, logicalLine + i, repeatColumn)
            if (pad.isNotEmpty()) {
              val offset = editor.getLineEndOffset(logicalLine + i)
              insertText(editor, caret, offset, pad)
            }
          }
          val updatedCount = if (started) (if (i == 0) count else count + 1) else count
          if (repeatColumn >= VimMotionGroupBase.LAST_COLUMN) {
            caret.moveToOffset(injector.motion.moveCaretToLineEnd(editor, logicalLine + i, true))
            repeatInsertText(editor, context, updatedCount, operatorArguments)
          } else if (injector.engineEditorHelper.getVisualLineLength(editor, visualLine + i) >= repeatColumn) {
            val visualPosition = VimVisualPosition(visualLine + i, repeatColumn, false)
            val inlaysCount = injector.engineEditorHelper.amountOfInlaysBeforeVisualPosition(editor, visualPosition)
            caret.moveToVisualPosition(VimVisualPosition(visualLine + i, repeatColumn + inlaysCount, false))
            repeatInsertText(editor, context, updatedCount, operatorArguments)
          }
        }
        injector.motion.moveCaret(editor, caret, position)
      } else {
        repeatInsertText(editor, context, count, operatorArguments)
        val position = injector.motion.getOffsetOfHorizontalMotion(editor, caret, -1, false)
        injector.motion.moveCaret(editor, caret, position)
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
        val positionCaretActions: MutableList<EditorActionHandlerBase> = java.util.ArrayList()
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
    initInsert(editor, context, VimStateMachine.Mode.INSERT)
  }

  override fun insertAfterLineEnd(editor: VimEditor, context: ExecutionContext) {
    for (caret in editor.nativeCarets()) {
      caret.moveToOffset(injector.motion.moveCaretToLineEnd(editor, caret))
    }
    initInsert(editor, context, VimStateMachine.Mode.INSERT)
  }

  /**
   * Begin insert after the cursor position
   * @param editor  The editor to insert into
   * @param context The data context
   */
  override fun insertAfterCursor(editor: VimEditor, context: ExecutionContext) {
    for (caret in editor.nativeCarets()) {
      caret.moveToOffset(injector.motion.getOffsetOfHorizontalMotion(editor, caret, 1, true))
    }
    initInsert(editor, context, VimStateMachine.Mode.INSERT)
  }

  /**
   * Begin insert before the start of the current line
   * @param editor  The editor to insert into
   * @param context The data context
   */
  override fun insertLineStart(editor: VimEditor, context: ExecutionContext) {
    for (caret in editor.nativeCarets()) {
      caret.moveToOffset(injector.motion.moveCaretToLineStart(editor, caret))
    }
    initInsert(editor, context, VimStateMachine.Mode.INSERT)
  }

  /**
   * Begin insert before the first non-blank on the current line
   *
   * @param editor The editor to insert into
   */
  override fun insertBeforeFirstNonBlank(editor: VimEditor, context: ExecutionContext) {
    for (caret in editor.nativeCarets()) {
      caret.moveToOffset(injector.motion.moveCaretToLineStartSkipLeading(editor, caret))
    }
    initInsert(editor, context, VimStateMachine.Mode.INSERT)
  }

  /**
   * Begin insert/replace mode
   * @param editor  The editor to insert into
   * @param context The data context
   * @param mode    The mode - indicate insert or replace
   */
  override fun initInsert(editor: VimEditor, context: ExecutionContext, mode: VimStateMachine.Mode) {
    val state = getInstance(editor)
    for (caret in editor.nativeCarets()) {
      caret.vimInsertStart = editor.createLiveMarker(caret.offset, caret.offset)
      if (caret == editor.primaryCaret()) {
        injector.markGroup.setMark(editor, MARK_CHANGE_START, caret.offset.point)
      }
    }
    val cmd = state.executingCommand
    if (cmd != null && state.isDotRepeatInProgress) {
      state.pushModes(mode, VimStateMachine.SubMode.NONE)
      if (mode === VimStateMachine.Mode.REPLACE) {
        editor.insertMode = false
      }
      if (cmd.flags.contains(CommandFlags.FLAG_NO_REPEAT_INSERT)) {
        val commandState = getInstance(editor)
        repeatInsert(
          editor, context, 1, false,
          OperatorArguments(false, 1, commandState.mode, commandState.subMode)
        )
      } else {
        val commandState = getInstance(editor)
        repeatInsert(
          editor, context, cmd.count, false,
          OperatorArguments(false, cmd.count, commandState.mode, commandState.subMode)
        )
      }
      if (mode === VimStateMachine.Mode.REPLACE) {
        editor.insertMode = true
      }
      state.popModes()
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
      oldOffset = editor.currentCaret().offset.point
      editor.insertMode = mode === VimStateMachine.Mode.INSERT
      state.pushModes(mode, VimStateMachine.SubMode.NONE)
    }
    notifyListeners(editor)
  }

  override fun runEnterAction(editor: VimEditor, context: ExecutionContext) {
    val state = getInstance(editor)
    if (!state.isDotRepeatInProgress) {
      // While repeating the enter action has been already executed because `initInsert` repeats the input
      val action = injector.nativeActionManager.enterAction
      if (action != null) {
        strokes.add(action)
        injector.actionExecutor.executeAction(action, context)
      }
    }
  }

  override fun runEnterAboveAction(editor: VimEditor, context: ExecutionContext) {
    val state = getInstance(editor)
    if (!state.isDotRepeatInProgress) {
      // While repeating the enter action has been already executed because `initInsert` repeats the input
      val action = injector.nativeActionManager.createLineAboveCaret
      if (action != null) {
        strokes.add(action)
        injector.actionExecutor.executeAction(action, context)
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
    var offset = editor.primaryCaret().offset.point
    val markGroup = injector.markGroup
    markGroup.setMark(editor, '^', offset)
    markGroup.setMark(editor, MARK_CHANGE_END, offset)
    if (getInstance(editor).mode === VimStateMachine.Mode.REPLACE) {
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
    lastStrokes = java.util.ArrayList(strokes)
    if (context != null) {
      injector.changeGroup.repeatInsert(editor, context, if (cnt == 0) 0 else cnt - 1, true, operatorArguments)
    }
    if (getInstance(editor).mode === VimStateMachine.Mode.INSERT) {
      updateLastInsertedTextRegister()
    }

    // The change pos '.' mark is the offset AFTER processing escape, and after switching to overtype
    offset = editor.primaryCaret().offset.point
    markGroup.setMark(editor, MARK_CHANGE_POS, offset)
    getInstance(editor).popModes()
    exitAllSingleCommandInsertModes(editor)
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

  private fun exitAllSingleCommandInsertModes(editor: VimEditor) {
    while (editor.inSingleCommandMode) {
      editor.vimStateMachine.popModes()
      if (editor.inInsertMode) {
        editor.vimStateMachine.popModes()
      }
    }
  }

  /**
   * Processes the Enter key by running the first successful action registered for "ENTER" keystroke.
   *
   *
   * If this is REPLACE mode we need to turn off OVERWRITE before and then turn OVERWRITE back on after sending the
   * "ENTER" key.
   *
   * @param editor  The editor to press "Enter" in
   * @param context The data context
   */
  override fun processEnter(editor: VimEditor, context: ExecutionContext) {
    if (editor.vimStateMachine.mode === VimStateMachine.Mode.REPLACE) {
      editor.insertMode = true
    }
    val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    val actions = injector.keyGroup.getActions(editor, enterKeyStroke)
    for (action in actions) {
      if (injector.actionExecutor.executeAction(action, context)) {
        break
      }
    }
    if (editor.vimStateMachine.mode === VimStateMachine.Mode.REPLACE) {
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
    toSwitch: VimStateMachine.Mode,
  ) {
    if (toSwitch === VimStateMachine.Mode.INSERT) {
      initInsert(editor, context, VimStateMachine.Mode.INSERT)
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
    getInstance(editor).pushModes(VimStateMachine.Mode.INSERT_NORMAL, VimStateMachine.SubMode.NONE)
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
  override fun deleteEndOfLine(editor: VimEditor, caret: VimCaret, count: Int, operatorArguments: OperatorArguments): Boolean {
    val initialOffset = caret.offset.point
    val offset = injector.motion.moveCaretToLineEndOffset(editor, caret, count - 1, true)
    val lineStart = injector.motion.moveCaretToLineStart(editor, caret)
    var startOffset = initialOffset
    if (offset == initialOffset && offset != lineStart) startOffset-- // handle delete from virtual space
    if (offset != -1) {
      val rangeToDelete = TextRange(startOffset, offset)
      editor.nativeCarets().filter { it != caret && rangeToDelete.contains(it.offset.point) }
        .forEach { editor.removeCaret(it) }
      val res = deleteText(editor, rangeToDelete, SelectionType.CHARACTER_WISE, caret, operatorArguments)
      if (usesVirtualSpace) {
        injector.motion.moveCaret(editor, caret, startOffset)
      } else {
        val pos = injector.motion.getOffsetOfHorizontalMotion(editor, caret, -1, false)
        if (pos != -1) {
          injector.motion.moveCaret(editor, caret, pos)
        }
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
    operatorArguments: OperatorArguments
  ): Boolean {
    var myCount = count
    if (myCount < 2) myCount = 2
    val lline = caret.getLogicalPosition().line
    val total = editor.lineCount()
    return if (lline + myCount > total) {
      false
    } else deleteJoinNLines(editor, caret, lline, myCount, spaces, operatorArguments)
  }

  /**
   * This processes all "regular" keystrokes entered while in insert/replace mode
   *
   * @param editor  The editor the character was typed into
   * @param context The data context
   * @param key     The user entered keystroke
   * @return true if this was a regular character, false if not
   */
  override fun processKey(
    editor: VimEditor,
    context: ExecutionContext,
    key: KeyStroke,
  ): Boolean {
    logger.debug { "processKey($key)" }
    if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
      type(editor, context, key.keyChar)
      return true
    }

    // Shift-space
    if (key.keyCode == 32 && key.modifiers and KeyEvent.SHIFT_DOWN_MASK != 0) {
      type(editor, context, ' ')
      return true
    }
    return false
  }

  override fun processKeyInSelectMode(
    editor: VimEditor,
    context: ExecutionContext,
    key: KeyStroke,
  ): Boolean {
    var res: Boolean
    SelectionVimListenerSuppressor.lock().use {
      res = processKey(editor, context, key)
      editor.exitSelectModeNative(false)
      KeyHandler.getInstance().reset(editor)
      if (isPrintableChar(key.keyChar) || activeTemplateWithLeftRightMotion(editor, key)) {
        injector.changeGroup.insertBeforeCursor(editor, context)
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
  override fun deleteLine(editor: VimEditor, caret: VimCaret, count: Int, operatorArguments: OperatorArguments): Boolean {
    val start = injector.motion.moveCaretToLineStart(editor, caret)
    val offset =
      min(injector.motion.moveCaretToLineEndOffset(editor, caret, count - 1, true) + 1, editor.fileSize().toInt())

    if (logger.isDebug()) {
      logger.debug("start=$start")
      logger.debug("offset=$offset")
    }
    if (offset != -1) {
      val res = deleteText(editor, TextRange(start, offset), SelectionType.LINE_WISE, caret, operatorArguments)
      if (res && caret.offset.point >= editor.fileSize() && caret.offset.point != 0) {
        injector.motion.moveCaret(
          editor, caret,
          injector.motion.moveCaretToLineStartSkipLeadingOffset(
            editor,
            caret, -1
          )
        )
      }
      return res
    }
    return false
  }

  override fun joinViaIdeaByCount(editor: VimEditor, context: ExecutionContext, count: Int): Boolean {
    val executions = if (count > 1) count - 1 else 1
    val allowedExecution = editor.nativeCarets().any { caret: VimCaret ->
      val lline = caret.getLogicalPosition().line
      val total = editor.lineCount()
      lline + count <= total
    }
    if (!allowedExecution) return false
    for (i in 0 until executions) {
      val joinLinesAction = injector.nativeActionManager.joinLines
      if (joinLinesAction != null) {
        injector.actionExecutor.executeAction(joinLinesAction, context)
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
    operatorArguments: OperatorArguments
  ): Boolean {
    val startLine = editor.offsetToLogicalPosition(range.startOffset).line
    val endLine = editor.offsetToLogicalPosition(range.endOffset).line
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
      caret.setNativeSelection(
        first.offset,
        second.offset
      )
    }
    val joinLinesAction = injector.nativeActionManager.joinLines
    if (joinLinesAction != null) {
      injector.actionExecutor.executeAction(joinLinesAction, context)
    }
    editor.nativeCarets().forEach { caret: VimCaret ->
      caret.removeNativeSelection()
      val (line, column) = caret.getVisualPosition()
      if (line < 1) return@forEach
      val newVisualPosition = VimVisualPosition(line - 1, column, false)
      caret.moveToVisualPosition(newVisualPosition)
    }
  }

  override fun getDeleteRangeAndType(
    editor: VimEditor,
    caret: VimCaret,
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
      val start = editor.offsetToLogicalPosition(range.startOffset)
      val end = editor.offsetToLogicalPosition(range.endOffset)
      if (start.line != end.line) {
        if (!injector.engineEditorHelper.anyNonWhitespace(editor, range.startOffset, -1) &&
          !injector.engineEditorHelper.anyNonWhitespace(editor, range.endOffset, 1)
        ) {
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
   * @return true if able to delete the text, false if not
   */
  override fun deleteRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType?,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean {

    // Update the last column before we delete, or we might be retrieving the data for a line that no longer exists
    caret.vimLastColumn = caret.inlayAwareVisualColumn
    val removeLastNewLine = removeLastNewLine(editor, range, type)
    val res = deleteText(editor, range, type, caret, operatorArguments)
    if (removeLastNewLine) {
      val textLength = editor.fileSize().toInt()
      editor.deleteString(TextRange(textLength - 1, textLength))
    }
    if (res) {
      var pos = injector.engineEditorHelper.normalizeOffset(editor, range.startOffset, isChange)
      if (type === SelectionType.LINE_WISE) {
        pos = injector.motion
          .moveCaretToLineWithStartOfLineOption(
            editor, editor.offsetToLogicalPosition(pos).line,
            caret
          )
      }
      injector.motion.moveCaret(editor, caret, pos)
    }
    return res
  }

  private fun removeLastNewLine(editor: VimEditor, range: TextRange, type: SelectionType?): Boolean {
    var endOffset = range.endOffset
    val fileSize = editor.fileSize().toInt()
    if (endOffset > fileSize) {
      check(
        !injector.optionService.isSet(
          OptionScope.GLOBAL,
          OptionConstants.ideastrictmodeName,
          OptionConstants.ideastrictmodeName
        )
      ) { "Incorrect offset. File size: $fileSize, offset: $endOffset" }
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
  override fun changeEndOfLine(editor: VimEditor, caret: VimCaret, count: Int, operatorArguments: OperatorArguments): Boolean {
    val res = deleteEndOfLine(editor, caret, count, operatorArguments)
    if (res) {
      caret.moveToOffset(injector.motion.moveCaretToLineEnd(editor, caret))
      editor.vimChangeActionSwitchMode = VimStateMachine.Mode.INSERT
    }
    return res
  }

  /**
   * Delete count characters and then enter insert mode
   *
   * @param editor The editor to change
   * @param caret  The caret to be moved
   * @param count  The number of characters to change
   * @return true if able to delete count characters, false if not
   */
  override fun changeCharacters(editor: VimEditor, caret: VimCaret, operatorArguments: OperatorArguments): Boolean {
    val count = operatorArguments.count1
    val len = injector.engineEditorHelper.getLineLength(editor)
    val col = caret.getLogicalPosition().column
    if (col + count >= len) {
      return changeEndOfLine(editor, caret, 1, operatorArguments)
    }
    val res = deleteCharacter(editor, caret, count, true, operatorArguments)
    if (res) {
      editor.vimChangeActionSwitchMode = VimStateMachine.Mode.INSERT
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
   * @param startLine The starting logical line
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
    // start my moving the cursor to the very end of the first line
    injector.motion.moveCaret(editor, caret, injector.motion.moveCaretToLineEnd(editor, startLine, true))
    for (i in 1 until count) {
      val start = injector.motion.moveCaretToLineEnd(editor, caret)
      val trailingWhitespaceStart = injector.motion.moveCaretToLineEndSkipLeadingOffset(
        editor,
        caret, 0
      )
      val hasTrailingWhitespace = start != trailingWhitespaceStart + 1
      caret.moveToOffset(start)
      val offset: Int = if (spaces) {
        injector.motion.moveCaretToLineStartSkipLeadingOffset(editor, caret, 1)
      } else {
        injector.motion.moveCaretToLineStart(editor, caret.getLogicalPosition().line + 1)
      }
      deleteText(editor, TextRange(caret.offset.point, offset), null, caret, operatorArguments)
      if (spaces && !hasTrailingWhitespace) {
        insertText(editor, caret, " ")
        injector.motion.moveCaret(
          editor, caret, injector.motion.getOffsetOfHorizontalMotion(editor, caret, -1, true)
        )
      }
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
      val firstStart = editor.offsetToLogicalPosition(startOffsets[0])
      val lastStart = editor.offsetToLogicalPosition(startOffsets[range.size() - 1])
      return lastStart.line - firstStart.line + 1
    }
  }
}

fun OperatedRange.toType() = when (this) {
  is OperatedRange.Characters -> SelectionType.CHARACTER_WISE
  is OperatedRange.Lines -> SelectionType.LINE_WISE
  is OperatedRange.Block -> SelectionType.BLOCK_WISE
}
