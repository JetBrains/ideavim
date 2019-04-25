package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.SelectionModel
import com.maddyhome.idea.vim.group.SelectionVimListenerSuppressor

/**
 * @author Alex Plate
 */

/**
 * Set selection without calling SelectionListener
 */
fun SelectionModel.vimSetSystemSelectionSilently(start: Int, end: Int) =
        SelectionVimListenerSuppressor.lock().use { setSelection(start, end) }

/**
 * Set selection without calling SelectionListener
 */
fun SelectionModel.vimSetSystemBlockSelectionSilently(start: LogicalPosition, end: LogicalPosition) =
        SelectionVimListenerSuppressor.lock().use { setBlockSelection(start, end) }

/**
 * Set selection without calling SelectionListener
 */
fun Caret.vimSetSystemSelectionSilently(start: Int, end: Int) =
        SelectionVimListenerSuppressor.lock().use { setSelection(start, end) }
