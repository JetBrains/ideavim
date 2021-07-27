/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.SelectionModel
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor

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
