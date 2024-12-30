/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimCaretListener
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inBlockSelection
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.undo.VimKeyBasedUndoService
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService

/**
 * @author Alex Plate
 *
 * Base class for motion handlers.
 * @see [MotionActionHandler.SingleExecution] and [MotionActionHandler.ForEachCaret]
 */
sealed class MotionActionHandler : EditorActionHandlerBase(false) {
  /**
   * By default, we unfold collapsed regions after caret movement inside the fold
   */
  open val keepFold: Boolean = false

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
      caret: ImmutableVimCaret,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Motion
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
      operatorArguments: OperatorArguments,
    ): Motion
  }

  /**
   * Support for commands that can be executed either once or for each caret depending on some circumstances
   * TODO this class should not exist at all, changes to command execution are required
   */
  abstract class AmbiguousExecution : MotionActionHandler() {
    abstract fun getMotionActionHandler(argument: Argument?): MotionActionHandler

    final override fun process(cmd: Command) {
      super.process(cmd)
    }

    final override fun postExecute(
      editor: VimEditor,
      context: ExecutionContext,
      cmd: Command,
      operatorArguments: OperatorArguments
    ) {
      super.postExecute(editor, context, cmd, operatorArguments)
    }
  }

  abstract val motionType: MotionType

  final override val type: Command.Type = Command.Type.MOTION

  fun getHandlerOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val handler = if (this is AmbiguousExecution) this.getMotionActionHandler(argument) else this
    return when (handler) {
      is SingleExecution -> handler.getOffset(editor, context, argument, operatorArguments)
      is ForEachCaret -> handler.getOffset(editor, caret, context, argument, operatorArguments)
      is AmbiguousExecution -> throw RuntimeException("Ambiguous handler cannot hold another ambiguous handler")
    }
  }

  final override fun baseExecute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val blockSelectionActive = editor.inBlockSelection

    val handler = if (this is AmbiguousExecution) this.getMotionActionHandler(cmd.argument) else this
    when (handler) {
      is SingleExecution -> run {
        if (editor.mode == Mode.INSERT) {
          val undo = injector.undo
          when (undo) {
            is VimKeyBasedUndoService -> undo.setMergeUndoKey()
            is VimTimestampBasedUndoService -> {
              val nanoTime = System.nanoTime()
              editor.forEachCaret { undo.endInsertSequence(it, it.offset, nanoTime) }
            }
          }
        }
        val offset = handler.getOffset(editor, context, cmd.argument, operatorArguments)

        // In this scenario, caret is the primary caret
        when (offset) {
          is Motion.AdjustedOffset -> moveToAdjustedOffset(editor, caret, cmd, offset)
          is Motion.AbsoluteOffset -> moveToAbsoluteOffset(editor, caret, cmd, offset)
          is Motion.Error -> injector.messages.indicateError()
          is Motion.NoMotion -> Unit
        }
      }
      is ForEachCaret -> run {
        when {
          blockSelectionActive || editor.carets().size == 1 -> {
            val primaryCaret = editor.primaryCaret()
            handler.doExecuteForEach(editor, primaryCaret, context, cmd, operatorArguments)
          }
          else -> {
            try {
              editor.addCaretListener(CaretMergingWatcher)
              editor.forEachCaret { caret ->
                handler.doExecuteForEach(
                  editor,
                  caret,
                  context,
                  cmd,
                  operatorArguments,
                )
              }
            } finally {
              editor.removeCaretListener(CaretMergingWatcher)
            }
          }
        }
      }
      is AmbiguousExecution -> throw RuntimeException("Ambiguous handler cannot hold another ambiguous handler")
    }

    return true
  }

  private fun ForEachCaret.doExecuteForEach(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ) {
    if (editor.mode == Mode.INSERT) {
      val undo = injector.undo
      when (undo) {
        is VimKeyBasedUndoService -> undo.setMergeUndoKey()
        is VimTimestampBasedUndoService -> undo.endInsertSequence(caret, caret.offset, System.nanoTime())
      }
    }
    val offset = getOffset(editor, caret, context, cmd.argument, operatorArguments)
    when (offset) {
      is Motion.AdjustedOffset -> moveToAdjustedOffset(editor, caret, cmd, offset)
      is Motion.AbsoluteOffset -> moveToAbsoluteOffset(editor, caret, cmd, offset)
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
    // Block selection mode is emulated with multiple carets. We should only be operating on the primary caret. Note
    // that moving the primary caret to modify the selection can cause IntelliJ to invalidate, replace or add a new
    // primary caret
    if (editor.inBlockSelection) {
      StrictMode.assert(caret.isPrimary, "Block selection mode must only operate on primary caret")
    }

    val normalisedOffset = prepareMoveToAbsoluteOffset(editor, cmd, offset)
    StrictMode.assert(normalisedOffset == offset.offset, "Adjusted offset should be normalised by action")

    // Set before moving, so it can be applied during move, especially important for LAST_COLUMN and visual block mode
    caret.vimLastColumn = offset.intendedColumn

    // In Fleet the caret is immutable, so we get a new version of the caret in order to update it
    // The whole caret movement system should be actually rewritten to restrict the iteration with carets at any moment
    //   of time. In this way we would have a better integration with Fleet.
    val caretAfterMove = caret.moveToOffset(normalisedOffset)

    // We've moved the caret, so reset the intended column. Visual block movement can replace the primary caret when
    // moving the selection up, so make sure we've got a valid caret
    val validCaret = if (editor.inBlockSelection) editor.primaryCaret() else caretAfterMove
    validCaret.vimLastColumn = offset.intendedColumn
  }

  private fun moveToAbsoluteOffset(editor: VimEditor, caret: VimCaret, cmd: Command, offset: Motion.AbsoluteOffset) {
    val normalisedOffset = prepareMoveToAbsoluteOffset(editor, cmd, offset)
    caret.moveToOffset(normalisedOffset)
  }

  private fun prepareMoveToAbsoluteOffset(
    editor: VimEditor,
    cmd: Command,
    offset: Motion.AbsoluteOffset,
  ): Int {
    var resultOffset = offset.offset
    if (resultOffset < 0) {
      logger.error("Offset is less than 0. $resultOffset. ${this.javaClass.name}")
    }
    if (CommandFlags.FLAG_SAVE_JUMP in cmd.flags) {
      injector.jumpService.saveJumpLocation(editor)
    }

    // TODO: This should be normalised by the action
    if (!editor.isEndAllowed) {
      resultOffset = editor.normalizeOffset(resultOffset, false)
    }

    val foldRegion = editor.getFoldRegionAtOffset(resultOffset)
    if (foldRegion != null && !foldRegion.isExpanded) {
      if (keepFold) {
        resultOffset = foldRegion.startOffset
      } else {
        foldRegion.isExpanded = true
      }
    }

    return resultOffset
  }

  override fun postExecute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments
  ) {
    // If we're in single-execution Visual mode, return to Select. See `:help v_CTRL-O`
    if ((editor.mode as? Mode.VISUAL)?.isSelectPending == true) {
      injector.visualMotionGroup.processSingleVisualCommand(editor)
    }
  }

  private object CaretMergingWatcher : VimCaretListener {
    override fun caretRemoved(caret: ImmutableVimCaret?) {
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
    val logger: VimLogger = vimLogger<MotionActionHandler>()
  }
}
