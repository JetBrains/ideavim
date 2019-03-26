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
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
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

object CaretVimListenerSuppressor : VimListenerSuppressor()
object SelectionVimListenerSuppressor : VimListenerSuppressor()

object VimListenerManager {

    val logger = Logger.getInstance(VimListenerManager::class.java)

    fun addEditorListeners(editor: Editor) {
        val eventFacade = EventFacade.getInstance()
        eventFacade.addEditorMouseListener(editor, EditorMouseHandler)
        eventFacade.addEditorMouseMotionListener(editor, EditorMouseHandler)
        eventFacade.addEditorSelectionListener(editor, EditorSelectionHandler)
        eventFacade.addCaretListener(editor, VimCaretListener)
    }

    fun removeEditorListeners(editor: Editor) {
        val eventFacade = EventFacade.getInstance()
        eventFacade.removeEditorMouseListener(editor, EditorMouseHandler)
        eventFacade.removeEditorMouseMotionListener(editor, EditorMouseHandler)
        eventFacade.removeEditorSelectionListener(editor, EditorSelectionHandler)
        eventFacade.removeCaretListener(editor, VimCaretListener)
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
                CaretVimListenerSuppressor.lock()
                logger.debug("Adjust non vim selection change")
                VisualMotionGroup.controlNonVimSelectionChange(editor)
                CaretVimListenerSuppressor.unlock()
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

        override fun mousePressed(event: EditorMouseEvent) {
            CaretVimListenerSuppressor.lock()
        }

        override fun mouseDragged(e: EditorMouseEvent) {
            if (!mouseDragging) {
                logger.debug("Mouse dragging")
                SelectionVimListenerSuppressor.lock()
                mouseDragging = true
            }
        }

        override fun mouseReleased(event: EditorMouseEvent) {
            if (mouseDragging) {
                logger.debug("Release mouse after dragging")
                // TODO: 2019-03-22 Docs about lead selectino
                val editor = event.editor
                if (event.area == EditorMouseEventArea.EDITING_AREA) {
                    editor.caretModel.runForEachCaret(this::extendSelectionToCaret)
                }
                VisualMotionGroup.controlNonVimSelectionChange(editor)
                SelectionVimListenerSuppressor.unlock()
                mouseDragging = false
            }
            CaretVimListenerSuppressor.unlock()
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

                if (!CommandState.inInsertMode(editor)) {
                    caretModel.runForEachCaret { caret ->
                        if (caret.offset == EditorHelper.getLineEndForOffset(editor, caret.offset)) {
                            MotionGroup.moveCaret(editor, caret, caret.offset - 1)
                        }
                    }
                }

                // TODO: 2019-03-22 Multi?
                caretModel.primaryCaret.vimLastColumn = caretModel.visualPosition.column
                if (event.mouseEvent.clickCount == 1 && CommandState.inVisualMode(editor)) {
                    VisualMotionGroup.exitVisual(editor)
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

        private fun extendSelectionToCaret(caret: Caret?) {
            if (caret != null &&
                    caret.selectionEnd == caret.offset &&
                    !VisualMotionGroup.exclusiveSelection &&
                    caret.offset < caret.editor.document.textLength) {
                CaretVimListenerSuppressor.lock()
                logger.debug("Extend selection to caret")
                caret.vimSetSelectionSilently(caret.selectionStart, caret.selectionEnd + 1)
                CaretVimListenerSuppressor.unlock()
            }
        }
    }

    private object VimCaretListener : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) {
            if (CaretVimListenerSuppressor.isNotLocked) {
                putCaretAtEndOfSelection(event.caret)
            }
        }

        private fun putCaretAtEndOfSelection(caret: Caret?) {
            if (caret != null &&
                    caret.selectionEnd == caret.offset &&
                    !VisualMotionGroup.exclusiveSelection &&
                    caret.selectionStart != caret.selectionEnd &&
                    caret.selectionEnd > 0) {
                CaretVimListenerSuppressor.lock()
                logger.debug("Put caret at the end of selection")
                caret.moveToOffset(caret.selectionEnd - 1)
                CaretVimListenerSuppressor.unlock()
                return
            }
            if (caret != null &&
                    caret.offset != caret.selectionStart &&
                    caret.offset != caret.selectionEnd - VisualMotionGroup.selectionAdj &&
                    caret.selectionStart != caret.selectionEnd &&
                    caret.selectionEnd > 0) {
                CaretVimListenerSuppressor.lock()
                logger.debug("Put caret at the end of selection")
                caret.moveToOffset(caret.selectionEnd - VisualMotionGroup.selectionAdj)
                CaretVimListenerSuppressor.unlock()
                return
            }
        }
    }
}
