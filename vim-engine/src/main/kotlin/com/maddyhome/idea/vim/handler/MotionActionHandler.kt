/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimCaretListener
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.options.helpers.StrictMode

/**
 * @author Alex Plate
 *
 * Base class for motion handlers.
 * @see [MotionActionHandler.SingleExecution] and [MotionActionHandler.ForEachCaret]
 */
sealed class MotionActionHandler : EditorActionHandlerBase(false) {

  /**
   * Base class for motion handlers.
   * This handler executes an action for each caret. That means that if you have 5 carets, [getOffset] will be
   *   called 5 times.
   * @see [MotionActionHandler.SingleExecution] for only one execution
   */
  abstract class ForEachCaret : MotionActionHandler() {

    /**
     * This method should return new offset for [caret]
     * It executes once for each [caret]. That means that if you have 5 carets, [getOffset] will be
     *   called 5 times.
     * The method executes only once it there is block selection.
     */
    abstract fun getOffset(
      editor: VimEditor,
      caret: VimCaret,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Motion

    /**
     * This method is called before [getOffset] once for each [caret].
     * The method executes only once it there is block selection.
     */
    open fun preOffsetComputation(editor: VimEditor, caret: VimCaret, context: ExecutionContext, cmd: Command): Boolean = true
  }

  /**
   * Base class for motion handlers.
   * This handler executes an action only once for all carets. That means that if you have 5 carets,
   *   [getOffset] will be called 1 time.
   * @see [MotionActionHandler.ForEachCaret] for per-caret execution
   */
  abstract class SingleExecution : MotionActionHandler() {
    /**
     * This method should return new offset for primary caret
     * It executes once for all carets. That means that if you have 5 carets, [getOffset] will be
     *   called 1 time.
     */
    abstract fun getOffset(
      editor: VimEditor,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments
    ): Motion
  }

  abstract val motionType: MotionType

  final override val type: Command.Type = Command.Type.MOTION

  fun getHandlerOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return when (this) {
      is SingleExecution -> getOffset(editor, context, argument, operatorArguments)
      is ForEachCaret -> getOffset(editor, caret, context, argument, operatorArguments)
    }
  }

  final override fun baseExecute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val blockSubmodeActive = editor.inBlockSubMode

    when (this) {
      is SingleExecution -> run {
        val offset = getOffset(editor, context, cmd.argument, operatorArguments)

        when (offset) {
          is Motion.AdjustedOffset -> moveToAdjustedOffset(editor, caret, cmd, offset)
          is Motion.AbsoluteOffset -> moveToAbsoluteOffset(editor, cmd, offset)
          is Motion.Error -> injector.messages.indicateError()
          is Motion.NoMotion -> Unit
        }
      }
      is ForEachCaret -> run {
        when {
          blockSubmodeActive || editor.carets().size == 1 -> {
            val primaryCaret = editor.primaryCaret()
            doExecuteForEach(editor, primaryCaret, context, cmd, operatorArguments)
          }
          else -> {
            try {
              editor.addCaretListener(CaretMergingWatcher)
              editor.forEachCaret { caret ->
                doExecuteForEach(
                  editor,
                  caret,
                  context,
                  cmd,
                  operatorArguments
                )
              }
            } finally {
              editor.removeCaretListener(CaretMergingWatcher)
            }
          }
        }
      }
    }

    return true
  }

  private fun ForEachCaret.doExecuteForEach(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments
  ) {
    if (!preOffsetComputation(editor, caret, context, cmd)) return

    val offset = getOffset(editor, caret, context, cmd.argument, operatorArguments)

    when (offset) {
      is Motion.AdjustedOffset -> moveToAdjustedOffset(editor, caret, cmd, offset)
      is Motion.AbsoluteOffset -> moveToAbsoluteOffset(editor, caret, context, cmd, offset)
      is Motion.Error -> injector.messages.indicateError()
      is Motion.NoMotion -> Unit
    }
  }

  private fun moveToAdjustedOffset(
    editor: VimEditor,
    caret: VimCaret,
    cmd: Command,
    offset: Motion.AdjustedOffset,
  ) {
    // Block selection mode is emulated with multiple carets, but we only ever operate on the primary caret. Changing
    // the selection (by moving the primary caret) can result in IntelliJ invalidating, replacing or adding a new
    // primary caret
    if (editor.inBlockSubMode) {
      StrictMode.assert(caret.isPrimary, "Block selection mode must only operate on primary caret")
    }

    val normalisedOffset = prepareMoveToAbsoluteOffset(editor, cmd, offset)
    StrictMode.assert(normalisedOffset == offset.offset, "Adjusted offset should be normalised by action")

    // Set before moving, so it can be applied during move, especially important for LAST_COLUMN and visual block mode
    caret.vimLastColumn = offset.intendedColumn

    caret.moveToOffset(normalisedOffset)

    // Visual block movement can replace the primary caret when moving the selection up, so reset the last column
    if (editor.inBlockSubMode) {
      editor.primaryCaret().vimLastColumn = offset.intendedColumn
    }
    else {
      // TODO: Remove this - we've already set it before moving the caret, but currently moving the caret sets last col
      caret.vimLastColumn = offset.intendedColumn
    }
  }

  private fun moveToAbsoluteOffset(
    editor: VimEditor,
    cmd: Command,
    offset: Motion.AbsoluteOffset
  ) {
    val normalisedOffset = prepareMoveToAbsoluteOffset(editor, cmd, offset)
    editor.primaryCaret().moveToOffset(normalisedOffset)
  }

  private fun ForEachCaret.moveToAbsoluteOffset(editor: VimEditor,
                                                caret: VimCaret,
                                                context: ExecutionContext,
                                                cmd: Command,
                                                offset: Motion.AbsoluteOffset) {
    val normalisedOffset = prepareMoveToAbsoluteOffset(editor, cmd, offset)
    caret.moveToOffset(normalisedOffset)
  }

  private fun prepareMoveToAbsoluteOffset(editor: VimEditor,
                                          cmd: Command,
                                          offset: Motion.AbsoluteOffset): Int {
    var resultOffset = offset.offset
    if (resultOffset < 0) {
      logger.error("Offset is less than 0. $resultOffset. ${this.javaClass.name}")
    }
    if (CommandFlags.FLAG_SAVE_JUMP in cmd.flags) {
      injector.markGroup.saveJumpLocation(editor)
    }

    // TODO: This should be normalised by the action
    if (!editor.isEndAllowed) {
      resultOffset = injector.engineEditorHelper.normalizeOffset(editor, resultOffset, false)
    }
    return resultOffset
  }

  private object CaretMergingWatcher : VimCaretListener {
    override fun caretRemoved(caret: VimCaret?) {
      caret ?: return
      val editor = caret.editor
      val caretToDelete = caret
      if (editor.inVisualMode) {
        for (vimCaret in editor.carets()) {
          val curCaretStart = vimCaret.selectionStart
          val curCaretEnd = vimCaret.selectionEnd
          val caretStartBetweenCur = caretToDelete.selectionStart in curCaretStart until curCaretEnd
          val caretEndBetweenCur = caretToDelete.selectionEnd in curCaretStart + 1..curCaretEnd
          if (caretStartBetweenCur || caretEndBetweenCur) {
            // Okay, caret is being removed because of merging
            val vimSelectionStart = caretToDelete.vimSelectionStart
            vimCaret.vimSelectionStart = vimSelectionStart
          }
        }
      }
    }
  }

  companion object {
    val logger = vimLogger<MotionActionHandler>()
  }
}
