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

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.maddyhome.idea.vim.common.TextRange

/**
 * @author Alex Plate
 */

val Caret.visualRangeMarker: RangeMarker
    get() = this.editor.document.createRangeMarker(selectionStart, selectionEnd, true)

val RangeMarker.vimTextRange: TextRange
    get() = TextRange(startOffset, endOffset)

val Caret.visualRange: TextRange
    get() = TextRange(selectionStart, selectionEnd)

val Editor.visualBlockRange: TextRange
    get() = selectionModel.run { TextRange(blockSelectionStarts, blockSelectionEnds) }
