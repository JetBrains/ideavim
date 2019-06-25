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

@file:JvmName("UserDataManager")
@file:Suppress("ObjectPropertyName")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.ui.ExOutputPanel

/**
 * @author Alex Plate
 */

//region Vim selection start ----------------------------------------------------
/**
 * Caret's offset when entering visual mode
 */
var Caret.vimSelectionStart: Int
  get() = _vimSelectionStart ?: throw AssertionError("Trying to access selection start, but it's not set")
  set(value) {
    _vimSelectionStart = value
  }

fun Caret.vimSelectionStartClear() {
  this._vimSelectionStart = null
}

private var Caret._vimSelectionStart: Int? by userDataCaretToEditor()
//endregion ----------------------------------------------------

//region Vim last column
private val LAST_COLUMN: Key<Int> = Key.create("lastColumn")
var Caret.vimLastColumn: Int
  get() = getUserData(LAST_COLUMN) ?: visualPosition.column
  set(value) = if (editor.inBlockSubMode) {
    editor.caretModel.primaryCaret.putUserData(LAST_COLUMN, value)
  } else {
    putUserData(LAST_COLUMN, value)
  }
//endregion

var Caret.vimLastVisualOperatorRange: VisualChange? by userDataCaretToEditor()
var Caret.vimInsertStart: RangeMarker by userDataOr { (this as Caret).editor.document.createRangeMarker(this.offset, this.offset) }


//------------------ Editor
fun unInitializeEditor(editor: Editor) {
  editor.vimLastSelectionType = null
  editor.vimCommandState = null
  editor.vimMorePanel = null
  editor.vimExOutput = null
  editor.vimLastHighlighters = null
}

var Editor.vimLastSearch: String? by userData()
var Editor.vimLastHighlighters: Collection<RangeHighlighter>? by userData()
/***
 * @see :help visualmode()
 */
var Editor.vimLastSelectionType: SelectionType? by userData()
var Editor.vimCommandState: CommandState? by userData()
var Editor.vimChangeGroup: Boolean by userDataOr { false }
var Editor.vimMotionGroup: Boolean by userDataOr { false }
var Editor.vimEditorGroup: Boolean by userDataOr { false }
var Editor.vimLineNumbersShown: Boolean by userDataOr { false }
var Editor.vimMorePanel: ExOutputPanel? by userData()
var Editor.vimExOutput: ExOutputModel? by userData()
var Editor.vimTestInputModel: TestInputModel? by userData()
/**
 * Checks whether a keeping visual mode visual operator action is performed on editor.
 */
var Editor.vimKeepingVisualOperatorAction: Boolean by userDataOr { false }
var Editor.vimChangeActionSwitchMode: CommandState.Mode? by userData()
