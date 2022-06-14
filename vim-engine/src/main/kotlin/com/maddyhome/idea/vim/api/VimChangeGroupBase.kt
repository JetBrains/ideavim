package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.CommandState.Companion.getInstance
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.OperatedRange
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_START
import kotlin.math.abs

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
  override fun deleteCharacter(editor: VimEditor, caret: VimCaret, count: Int, isChange: Boolean): Boolean {
    val endOffset = injector.motion.getOffsetOfHorizontalMotion(editor, caret, count, true)
    if (endOffset != -1) {
      val res = deleteText(editor, TextRange(caret.offset.point, endOffset), SelectionType.CHARACTER_WISE)
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
      editor.inInsertMode || injector.registerGroup.storeText(editor, updatedRange, type, true)
    ) {
      val startOffsets = updatedRange.startOffsets
      val endOffsets = updatedRange.endOffsets
      for (i in updatedRange.size() - 1 downTo 0) {
        editor.deleteString(TextRange(startOffsets[i], endOffsets[i]))
      }
      if (type != null) {
        val start = updatedRange.startOffset
        injector.markGroup.setMark(editor, MARK_CHANGE_POS, start)
        injector.markGroup.setChangeMarks(editor, TextRange(start, start + 1))
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
  protected fun repeatInsert(
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
          if (repeatAppend
            && (repeatColumn < VimMotionGroupBase.LAST_COLUMN)
            && (injector.engineEditorHelper.getVisualLineLength(editor, visualLine + i) < repeatColumn)
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
    initInsert(editor, context, CommandState.Mode.INSERT)
  }

  override fun insertAfterLineEnd(editor: VimEditor, context: ExecutionContext) {
    for (caret in editor.nativeCarets()) {
      caret.moveToOffset(injector.motion.moveCaretToLineEnd(editor, caret))
    }
    initInsert(editor, context, CommandState.Mode.INSERT)
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
    initInsert(editor, context, CommandState.Mode.INSERT)
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
    initInsert(editor, context, CommandState.Mode.INSERT)
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
    initInsert(editor, context, CommandState.Mode.INSERT)
  }

  /**
   * Begin insert/replace mode
   * @param editor  The editor to insert into
   * @param context The data context
   * @param mode    The mode - indicate insert or replace
   */
  override fun initInsert(editor: VimEditor, context: ExecutionContext, mode: CommandState.Mode) {
    val state = getInstance(editor)
    for (caret in editor.nativeCarets()) {
      caret.vimInsertStart = editor.createLiveMarker(caret.offset, caret.offset)
      if (caret == editor.primaryCaret()) {
        injector.markGroup.setMark(editor, MARK_CHANGE_START, caret.offset.point)
      }
    }
    val cmd = state.executingCommand
    if (cmd != null && state.isDotRepeatInProgress) {
      state.pushModes(mode, CommandState.SubMode.NONE)
      if (mode === CommandState.Mode.REPLACE) {
        editor.setInsertMode(false)
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
      if (mode === CommandState.Mode.REPLACE) {
        editor.setInsertMode(true)
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
      editor.setInsertMode(mode === CommandState.Mode.INSERT)
      state.pushModes(mode, CommandState.SubMode.NONE)
    }
    notifyListeners(editor)
  }

  companion object {
    private const val MAX_REPEAT_CHARS_COUNT = 10000
  }
}

fun OperatedRange.toType() = when (this) {
  is OperatedRange.Characters -> SelectionType.CHARACTER_WISE
  is OperatedRange.Lines -> SelectionType.LINE_WISE
  is OperatedRange.Block -> SelectionType.BLOCK_WISE
}
