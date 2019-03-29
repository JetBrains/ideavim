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

package com.maddyhome.idea.vim.group

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.ex.DocumentEx
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.group.motion.VisualMotionGroup
import com.maddyhome.idea.vim.group.motion.vimSetSelectionSilently
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.ui.ExEntryPanel
import java.awt.event.MouseEvent

/**
 * @author Alex Plate
 */

/**
 * Base class for listener suppressors.
 * Children of this class have an ability to suppress editor listeners
 *
 * E.g.
 * ```
 *  CaretVimListenerSuppressor.lock()
 *  caret.moveToOffset(10) // vim's caret listener will not be executed
 *  CaretVimListenerSuppressor.unlock()
 * ````
 *
 *  Locks can be nested:
 * ```
 *      CaretVimListenerSuppressor.lock()
 *      moveCaret(caret) // vim's caret listener will not be executed
 *      CaretVimListenerSuppressor.unlock()
 *
 *  fun moveCaret(caret: Caret) {
 *      CaretVimListenerSuppressor.lock()
 *      caret.moveToOffset(10)
 *      CaretVimListenerSuppressor.unlock()
 *  }
 * ```
 */
sealed class VimListenerSuppressor {
    private var caretListenerSuppressor = 0

    fun lock() {
        if (logger.isDebugEnabled) {
            val methodName = Throwable().stackTrace[1].methodName
            val className = Throwable().stackTrace[1].className.takeLastWhile { it != '.' }
            logger.debug("${this.javaClass.simpleName}: lock by $className#$methodName. State before lock: $caretListenerSuppressor")
        }
        caretListenerSuppressor++
    }

    fun unlock() {
        if (logger.isDebugEnabled) {
            val methodName = Throwable().stackTrace[1].methodName
            val className = Throwable().stackTrace[1].className.takeLastWhile { it != '.' }
            logger.debug("${this.javaClass.simpleName}: unlock by $className#$methodName. State before unlock: $caretListenerSuppressor")
        }
        caretListenerSuppressor--
    }

    val isNotLocked: Boolean
        get() = caretListenerSuppressor == 0

    companion object {
        val logger = Logger.getInstance(VimListenerSuppressor::class.java)
    }
}

object SelectionVimListenerSuppressor : VimListenerSuppressor()

object VimListenerManager {

    val logger = Logger.getInstance(VimListenerManager::class.java)

    fun addEditorListeners(editor: Editor) {
        val eventFacade = EventFacade.getInstance()
        eventFacade.addEditorMouseListener(editor, EditorMouseHandler)
        eventFacade.addEditorMouseMotionListener(editor, EditorMouseHandler)
        eventFacade.addEditorSelectionListener(editor, EditorSelectionHandler)
    }

    fun removeEditorListeners(editor: Editor) {
        val eventFacade = EventFacade.getInstance()
        eventFacade.removeEditorMouseListener(editor, EditorMouseHandler)
        eventFacade.removeEditorMouseMotionListener(editor, EditorMouseHandler)
        eventFacade.removeEditorSelectionListener(editor, EditorSelectionHandler)
    }

    private object EditorSelectionHandler : SelectionListener {
        private var myMakingChanges = false

        /**
         * This event is executed for each caret using [com.intellij.openapi.editor.CaretModel.runForEachCaret]
         */
        override fun selectionChanged(selectionEvent: SelectionEvent) {
            val editor = selectionEvent.editor
            val document = editor.document

            if (SelectionVimListenerSuppressor.isNotLocked) {
                logger.debug("Adjust non vim selection change")
                VisualMotionGroup.controlNonVimSelectionChange(editor)
            }

            if (myMakingChanges || document is DocumentEx && document.isInEventsHandling) {
                return
            }

            myMakingChanges = true
            try {
                // Synchronize selections between editors
                val newRange = selectionEvent.newRange
                for (e in EditorFactory.getInstance().getEditors(document)) {
                    if (e != editor) {
                        e.selectionModel
                                .vimSetSelectionSilently(newRange.startOffset, newRange.endOffset)
                    }
                }
            } finally {
                myMakingChanges = false
            }
        }
    }

    private object EditorMouseHandler : EditorMouseListener, EditorMouseMotionListener {
        private var mouseDragging = false

        override fun mouseDragged(e: EditorMouseEvent) {
            if (!mouseDragging) {
                logger.debug("Mouse dragging")
                SelectionVimListenerSuppressor.lock()
                mouseDragging = true
                ChangeGroup.resetCursor(e.editor, true)
            }
        }

        override fun mouseReleased(event: EditorMouseEvent) {
            if (mouseDragging) {
                logger.debug("Release mouse after dragging")
                VisualMotionGroup.controlNonVimSelectionChange(event.editor)
                SelectionVimListenerSuppressor.unlock()
                mouseDragging = false
            }
        }

        override fun mouseClicked(event: EditorMouseEvent) {
            if (!VimPlugin.isEnabled()) return
            logger.debug("Mouse clicked")

            if (event.area == EditorMouseEventArea.EDITING_AREA) {
                VimPlugin.getMotion()
                val editor = event.editor
                if (ExEntryPanel.getInstance().isActive) {
                    ExEntryPanel.getInstance().deactivate(false)
                }

                ExOutputModel.getInstance(editor).clear()

                val caretModel = editor.caretModel
                if (CommandState.getInstance(editor).subMode != CommandState.SubMode.NONE) {
                    caretModel.removeSecondaryCarets()
                }

                // TODO: 2019-03-22 Multi?
                caretModel.primaryCaret.vimLastColumn = caretModel.visualPosition.column
                if (event.mouseEvent.clickCount == 1) {
                    if (CommandState.inVisualMode(editor)) {
                        VisualMotionGroup.exitVisual(editor)
                    } else if (CommandState.getInstance(editor).mode == CommandState.Mode.SELECT) {
                        VisualMotionGroup.exitSelectMode(editor)
                    }
                }

                if (!CommandState.inInsertMode(editor)) {
                    caretModel.runForEachCaret { caret ->
                        val lineEnd = EditorHelper.getLineEndForOffset(editor, caret.offset)
                        val lineStart = EditorHelper.getLineStartForOffset(editor, caret.offset)
                        if (caret.offset == lineEnd && lineEnd != lineStart) {
                            MotionGroup.moveCaret(editor, caret, caret.offset - 1)
                        }
                    }
                }
            } else if (event.area != EditorMouseEventArea.ANNOTATIONS_AREA &&
                    event.area != EditorMouseEventArea.FOLDING_OUTLINE_AREA &&
                    event.mouseEvent.button != MouseEvent.BUTTON3) {
                VimPlugin.getMotion()
                if (ExEntryPanel.getInstance().isActive) {
                    ExEntryPanel.getInstance().deactivate(false)
                }

                ExOutputModel.getInstance(event.editor).clear()
            }
        }
    }
}
