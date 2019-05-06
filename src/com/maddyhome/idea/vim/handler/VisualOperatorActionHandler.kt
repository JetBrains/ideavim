/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.*
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.*

/**
 * @author Alex Plate
 *
 * Base class for visual operation handlers.
 * @see [VisualOperatorActionHandler.SingleExecution] and [VisualOperatorActionHandler.ForEachCaret]
 */
sealed class VisualOperatorActionHandler : EditorActionHandlerBase(false) {

    /**
     * Execute an action for current [caret].
     * The selection offsets and type should be takes from [range] because this [caret] doesn't have this selection
     *   anymore in time of action execution (and editor is in normal mode, not visual).
     *
     * This method is executed once for each caret except case with block selection. If there is block selection,
     *   the method will be executed only once with [Caret#primaryCaret].
     */
    protected abstract fun executeAction(editor: Editor, caret: Caret, context: DataContext, cmd: Command, range: VimSelection): Boolean

    /**
     * This method executes before [executeAction] and only once for all carets.
     * [caretsAndSelections] contains a map of all current carets and corresponding selections.
     *   If there is block selection, only one caret is in [caretsAndSelections].
     */
    protected open fun beforeExecution(editor: Editor, context: DataContext, cmd: Command, caretsAndSelections: Map<Caret, VimSelection>) = true

    /**
     * This method executes after [executeAction] and only once for all carets.
     * [res] has true if ALL executions of [executeAction] returned true.
     */
    protected open fun afterExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}

    final override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        logger.debug("execute, cmd=$cmd")

        EditorData.setChangeSwitchMode(editor, null)

        val selections = editor.collectSelections() ?: return false

        val commandWrapper = VisualStartFinishWrapper(editor, cmd)
        commandWrapper.start()

        if (!beforeExecution(editor, context, cmd, selections)) return false

        val res = Ref.create(true)
        when {
            selections.keys.isEmpty() -> return false
            selections.keys.size == 1 -> res.set(executeAction(editor, selections.keys.first(), context, cmd, selections.values.first()))
            else -> editor.caretModel.runForEachCaret({ caret ->
                val range = selections.getValue(caret)
                val loopRes = executeAction(editor, caret, context, cmd, range)
                res.set(loopRes and res.get())
            }, true)
        }

        afterExecution(editor, context, cmd, res.get())

        commandWrapper.finish(res.get())

        EditorData.getChangeSwitchMode(editor)?.let {
            VimPlugin.getChange().processPostChangeModeSwitch(editor, context, it)
        }

        return res.get()
    }

    final override fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
        return super.execute(editor, caret, context, cmd)
    }

    private fun Editor.collectSelections(): Map<Caret, VimSelection>? {

        if (CommandState.inVisualBlockMode(this)) {
            val primaryCaret = caretModel.primaryCaret
            return mapOf(primaryCaret to VimBlockSelection(
                    primaryCaret.vimSelectionStart,
                    primaryCaret.offset,
                    this, primaryCaret.vimLastColumn >= MotionGroup.LAST_COLUMN))
        }

        return this.caretModel.allCarets.associateWith { caret ->
            if (CommandState.inVisualMode(this)) {
                val subMode = CommandState.getInstance(this).subMode
                VimSimpleSelection.createWithNative(
                        caret.vimSelectionStart,
                        caret.offset,
                        caret.selectionStart,
                        caret.selectionEnd,
                        SelectionType.fromSubMode(subMode),
                        this
                )
            } else {
                val offsets = VimPlugin.getMark().getVisualSelectionMarks(this) ?: return null
                val lastSelectionType = EditorData.getLastSelectionType(this) ?: return null
                VimSelection.create(offsets.startOffset, offsets.endOffset, lastSelectionType, this)
            }
        }
    }

    protected class VisualStartFinishWrapper(private val editor: Editor, private val cmd: Command) {
        private lateinit var lastMode: CommandState.SubMode
        private var wasRepeat: Boolean = false

        private fun startForCaret(caret: Caret) {
            if (CommandState.getInstance(editor).mode == CommandState.Mode.REPEAT) {
                caret.vimPreviousLastColumn = caret.vimLastColumn
                val range = caret.vimLastVisualOperatorRange
                VimPlugin.getVisualMotion().toggleVisual(editor, 1, 1, CommandState.SubMode.NONE)
                if (range != null && range.columns == MotionGroup.LAST_COLUMN) {
                    caret.vimLastColumn = MotionGroup.LAST_COLUMN
                }
            }

            var change: VisualChange? = null
            if (CommandState.getInstance(editor).mode == CommandState.Mode.VISUAL) {
                if (!wasRepeat) {
                    change = VimPlugin.getVisualMotion()
                            .getVisualOperatorRange(editor, caret, cmd.flags)
                }
                logger.debug("change=$change")
            }
            caret.vimVisualChange = change
        }

        fun start() {
            logger.debug("start")
            wasRepeat = CommandState.getInstance(editor).mode == CommandState.Mode.REPEAT
            EditorData.setKeepingVisualOperatorAction(editor, CommandFlags.FLAG_EXIT_VISUAL !in cmd.flags)

            for (caret in editor.caretModel.allCarets) {
                startForCaret(caret)
            }

            // If this is a mutli key change then exit visual now
            if (CommandFlags.FLAG_MULTIKEY_UNDO in cmd.flags) {
                logger.debug("multikey undo - exit visual")
                VimPlugin.getVisualMotion().exitVisual(editor)
            } else if (CommandFlags.FLAG_FORCE_LINEWISE in cmd.flags) {
                lastMode = CommandState.getInstance(editor).subMode
                if (lastMode != CommandState.SubMode.VISUAL_LINE && CommandFlags.FLAG_FORCE_VISUAL in cmd.flags) {
                    VimPlugin.getVisualMotion().toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_LINE)
                }
            }

            if (CommandFlags.FLAG_EXIT_VISUAL in cmd.flags) {
                VimPlugin.getVisualMotion().exitVisual(editor)
            }
        }

        private fun finishForCaret(caret: Caret, res: Boolean) {
            if (CommandFlags.FLAG_MULTIKEY_UNDO !in cmd.flags && CommandFlags.FLAG_EXPECT_MORE !in cmd.flags) {
                if (wasRepeat) {
                    caret.vimLastColumn = caret.vimPreviousLastColumn
                }
            }

            if (res) {
                caret.vimVisualChange?.let {
                    caret.vimLastVisualOperatorRange = it
                }
            }
        }

        fun finish(res: Boolean) {
            logger.debug("finish")

            if (CommandFlags.FLAG_FORCE_LINEWISE in cmd.flags) {
                if (this::lastMode.isInitialized && lastMode != CommandState.SubMode.VISUAL_LINE && CommandFlags.FLAG_FORCE_VISUAL in cmd.flags) {
                    VimPlugin.getVisualMotion().toggleVisual(editor, 1, 0, lastMode)
                }
            }

            if (CommandFlags.FLAG_MULTIKEY_UNDO !in cmd.flags && CommandFlags.FLAG_EXPECT_MORE !in cmd.flags) {
                logger.debug("not multikey undo - exit visual")
                VimPlugin.getVisualMotion().exitVisual(editor)
            }

            if (res) {
                CommandState.getInstance(editor).saveLastChangeCommand(cmd)
            }

            for (caret in editor.caretModel.allCarets) {
                finishForCaret(caret, res)
            }

            EditorData.setKeepingVisualOperatorAction(editor, false)
        }
    }

    /**
     * Base class for visual operation handlers.
     * This handler executes an action for each caret. That means that if you have 5 carets,
     *   [executeAction] will be called 5 times.
     * @see [VisualOperatorActionHandler.SingleExecution] for only one execution
     */
    abstract class ForEachCaret : VisualOperatorActionHandler()

    /**
     * Base class for visual operation handlers.
     * This handler executes an action only once for all carets. That means that if you have 5 carets,
     *   [executeAction] will be called 1 time.
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
        abstract fun executeForAllCarets(editor: Editor, context: DataContext, cmd: Command, caretsAndSelections: Map<Caret, VimSelection>): Boolean

        final override fun executeAction(editor: Editor, caret: Caret, context: DataContext, cmd: Command, range: VimSelection) = true

        final override fun beforeExecution(editor: Editor, context: DataContext, cmd: Command, caretsAndSelections: Map<Caret, VimSelection>): Boolean {
            return executeForAllCarets(editor, context, cmd, caretsAndSelections)
        }

        final override fun afterExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}
    }

    companion object {
        val logger = Logger.getInstance(VisualOperatorActionHandler::class.java.name)
    }
}
