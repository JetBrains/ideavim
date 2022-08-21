/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.action.change.VimRepeater
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.group.visual.VimBlockSelection
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.group.visual.VimSimpleSelection
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.group.visual.VisualOperation
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.inRepeatMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.vimStateMachine

/**
 * @author Alex Plate
 *
 * Base class for visual operation handlers.
 *
 * Use subclasses of this handler:
 *  - [VisualOperatorActionHandler.SingleExecution]
 *  - [VisualOperatorActionHandler.ForEachCaret]
 */
sealed class VisualOperatorActionHandler : EditorActionHandlerBase(false) {
  /**
   * Base class for visual operation handlers.
   * This handler executes an action for each caret. That means that if you have 5 carets,
   *   [executeAction] will be called 5 times.
   * @see [VisualOperatorActionHandler.SingleExecution] for only one execution.
   */
  abstract class ForEachCaret : VisualOperatorActionHandler() {

    /**
     * Execute an action for current [caret].
     * The selection offsets and type should be takes from [range] because this [caret] doesn't have this selection
     *   anymore in time of action execution (and editor is in normal mode, not visual).
     *
     * This method is executed once for each caret except case with block selection. If there is block selection,
     *   the method will be executed only once with [Caret#primaryCaret].
     */
    abstract fun executeAction(
      editor: VimEditor,
      caret: VimCaret,
      context: ExecutionContext,
      cmd: Command,
      range: VimSelection,
      operatorArguments: OperatorArguments,
    ): Boolean

    /**
     * This method executes before [executeAction] and only once for all carets.
     * [caretsAndSelections] contains a map of all current carets and corresponding selections.
     *   If there is block selection, only one caret is in [caretsAndSelections].
     */
    open fun beforeExecution(
      editor: VimEditor,
      context: ExecutionContext,
      cmd: Command,
      caretsAndSelections: Map<VimCaret, VimSelection>,
    ) = true

    /**
     * This method executes after [executeAction] and only once for all carets.
     * [res] has true if ALL executions of [executeAction] returned true.
     */
    open fun afterExecution(editor: VimEditor, context: ExecutionContext, cmd: Command, res: Boolean) {}
  }

  /**
   * Base class for visual operation handlers.
   * This handler executes an action only once for all carets. That means that if you have 5 carets,
   *   [executeForAllCarets] will be called 1 time.
   * @see [VisualOperatorActionHandler.ForEachCaret] for per-caret execution
   */
  abstract class SingleExecution : VisualOperatorActionHandler() {
    /**
     * Execute an action
     * [caretsAndSelections] contains a map of all current carets and corresponding selections.
     *   If there is block selection, only one caret is in [caretsAndSelections].
     *
     * This method is executed once for all carets.
     */
    abstract fun executeForAllCarets(
      editor: VimEditor,
      context: ExecutionContext,
      cmd: Command,
      caretsAndSelections: Map<VimCaret, VimSelection>,
      operatorArguments: OperatorArguments,
    ): Boolean
  }

  final override fun baseExecute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments
  ): Boolean {
    logger.info("Execute visual command $cmd")

    editor.vimChangeActionSwitchMode = null

    val selections = editor.collectSelections() ?: return false
    logger.debug { "Count of selection segments: ${selections.size}" }
    logger.debug { selections.values.joinToString("\n") { vimSelection -> "Caret: $vimSelection" } }

    val commandWrapper = VisualStartFinishWrapper(editor, cmd, this)
    commandWrapper.start()

    val res = arrayOf(true)
    when (this) {
      is SingleExecution -> {
        res[0] = executeForAllCarets(editor, context, cmd, selections, operatorArguments)
      }
      is ForEachCaret -> {
        logger.debug("Calling 'before execution'")
        if (!beforeExecution(editor, context, cmd, selections)) {
          logger.debug("Before execution block returned false. Stop further processing")
          return false
        }

        when {
          selections.keys.isEmpty() -> return false
          selections.keys.size == 1 -> res[0] =
            executeAction(
              editor,
              selections.keys.first(),
              context,
              cmd,
              selections.values.first(),
              operatorArguments
            )
          else -> editor.forEachNativeCaret(
            { currentCaret ->
              val range = selections.getValue(currentCaret)
              val loopRes = executeAction(editor, currentCaret, context, cmd, range, operatorArguments)
              res[0] = loopRes and res[0]
            },
            true
          )
        }

        logger.debug("Calling 'after execution'")
        afterExecution(editor, context, cmd, res[0])
      }
    }

    commandWrapper.finish(res[0])

    editor.vimChangeActionSwitchMode?.let {
      injector.changeGroup.processPostChangeModeSwitch(editor, context, it)
    }

    return res[0]
  }

  private fun VimEditor.collectSelections(): Map<VimCaret, VimSelection>? {
    return when {
      !this.inVisualMode && this.inRepeatMode -> {
        if (this.vimLastSelectionType == SelectionType.BLOCK_WISE) {
          val primaryCaret = primaryCaret()
          val range = primaryCaret.vimLastVisualOperatorRange ?: return null
          val end = VisualOperation.calculateRange(this, range, 1, primaryCaret)
          mapOf(
            primaryCaret to VimBlockSelection(
              primaryCaret.offset.point,
              end,
              this, range.columns >= VimMotionGroupBase.LAST_COLUMN
            )
          )
        } else {
          val carets = mutableMapOf<VimCaret, VimSelection>()
          this.nativeCarets().forEach { caret ->
            val range = caret.vimLastVisualOperatorRange ?: return@forEach
            val end = VisualOperation.calculateRange(this, range, 1, caret)
            carets += caret to VimSelection.create(caret.offset.point, end, range.type, this)
          }
          carets.toMap()
        }
      }
      this.inBlockSubMode -> {
        val primaryCaret = primaryCaret()
        mapOf(
          primaryCaret to VimBlockSelection(
            primaryCaret.vimSelectionStart,
            primaryCaret.offset.point,
            this, primaryCaret.vimLastColumn >= VimMotionGroupBase.LAST_COLUMN
          )
        )
      }
      else -> this.nativeCarets().associateWith { caret ->
        val subMode = this.vimStateMachine.subMode
        VimSimpleSelection.createWithNative(
          caret.vimSelectionStart,
          caret.offset.point,
          caret.selectionStart,
          caret.selectionEnd,
          SelectionType.fromSubMode(subMode),
          this
        )
      }
    }
  }

  private class VisualStartFinishWrapper(
    private val editor: VimEditor,
    private val cmd: Command,
    private val visualOperatorActionHandler: VisualOperatorActionHandler
  ) {
    private val visualChanges = mutableMapOf<VimCaret, VisualChange?>()

    fun start() {
      logger.debug("Preparing visual command")
      editor.vimKeepingVisualOperatorAction = CommandFlags.FLAG_EXIT_VISUAL !in cmd.flags

      editor.forEachCaret {
        val change =
          if (this@VisualStartFinishWrapper.editor.inVisualMode && !this@VisualStartFinishWrapper.editor.inRepeatMode) {
            VisualOperation.getRange(this@VisualStartFinishWrapper.editor, it, this@VisualStartFinishWrapper.cmd.flags)
          } else null
        this@VisualStartFinishWrapper.visualChanges[it] = change
      }
      logger.debug { visualChanges.values.joinToString("\n") { "Caret: $visualChanges" } }

      // If this is a mutli key change then exit visual now
      if (CommandFlags.FLAG_MULTIKEY_UNDO in cmd.flags || CommandFlags.FLAG_EXIT_VISUAL in cmd.flags) {
        logger.debug("Exit visual before command executing")
        editor.exitVisualModeNative()
      }
    }

    fun finish(res: Boolean) {
      logger.debug("Finish visual command. Result: $res")

      if (visualOperatorActionHandler.id != "VimVisualOperatorAction" ||
        injector.keyGroup.operatorFunction?.postProcessSelection() != false
      ) {
        if (CommandFlags.FLAG_MULTIKEY_UNDO !in cmd.flags && CommandFlags.FLAG_EXPECT_MORE !in cmd.flags) {
          logger.debug("Not multikey undo - exit visual")
          editor.exitVisualModeNative()
        }
      }

      if (res) {
        VimRepeater.saveLastChange(cmd)
        VimRepeater.repeatHandler = false
        editor.forEachCaret { caret ->
          val visualChange = visualChanges[caret]
          if (visualChange != null) {
            caret.vimLastVisualOperatorRange = visualChange
          }
        }
        editor.forEachCaret({ it.vimLastColumn = it.getVisualPosition().column })
      }

      editor.vimKeepingVisualOperatorAction = false
    }
  }

  private companion object {
    val logger = vimLogger<VisualOperatorActionHandler>()
  }
}
