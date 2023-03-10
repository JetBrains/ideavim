/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.SelectionModel
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor

/**
 * @author Alex Plate
 */

/**
 * Set selection without calling SelectionListener
 */
internal fun SelectionModel.vimSetSystemSelectionSilently(start: Int, end: Int) =
  SelectionVimListenerSuppressor.lock().use { setSelection(start, end) }

/**
 * Set selection without calling SelectionListener
 */
internal fun SelectionModel.vimSetSystemBlockSelectionSilently(start: LogicalPosition, end: LogicalPosition) =
  SelectionVimListenerSuppressor.lock().use { setBlockSelection(start, end) }
