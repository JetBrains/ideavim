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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:JvmName("UserDataManager")
@file:Suppress("ObjectPropertyName")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.group.visual.vimLeadSelectionOffset
import com.maddyhome.idea.vim.ui.ExOutputPanel
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * @author Alex Plate
 */

//region Vim selection start ----------------------------------------------------
/**
 * Caret's offset when entering visual mode
 */
var Caret.vimSelectionStart: Int
  get() {
    val selectionStart = _vimSelectionStart
    if (selectionStart == null) {
      vimSelectionStart = vimLeadSelectionOffset
      return vimLeadSelectionOffset
    }
    return selectionStart
  }
  set(value) {
    _vimSelectionStart = value
  }

fun Caret.vimSelectionStartClear() {
  this._vimSelectionStart = null
}

private var Caret._vimSelectionStart: Int? by userDataCaretToEditor()
//endregion ----------------------------------------------------

var Caret.vimLastColumn: Int by userDataCaretToEditorOr { (this as Caret).visualPosition.column }
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
var Editor.vimIncsearchCurrentMatchOffset: Int? by userData()
/***
 * @see :help visualmode()
 */
var Editor.vimLastSelectionType: SelectionType? by userData()
var Editor.vimCommandState: CommandState? by userData()
var Editor.vimChangeGroup: Boolean by userDataOr { false }
var Editor.vimMotionGroup: Boolean by userDataOr { false }
var Editor.vimEditorGroup: Boolean by userDataOr { false }
var Editor.vimLineNumbersInitialState: Boolean by userDataOr { false }
var Editor.vimHasRelativeLineNumbersInstalled: Boolean by userDataOr { false }
var Editor.vimMorePanel: ExOutputPanel? by userData()
var Editor.vimExOutput: ExOutputModel? by userData()
var Editor.vimTestInputModel: TestInputModel? by userData()
/**
 * Checks whether a keeping visual mode visual operator action is performed on editor.
 */
var Editor.vimKeepingVisualOperatorAction: Boolean by userDataOr { false }
var Editor.vimChangeActionSwitchMode: CommandState.Mode? by userData()


/**
 * Function for delegated properties.
 * The property will be delegated to UserData and has nullable type.
 */
private fun <T> userData(): ReadWriteProperty<UserDataHolder, T?> = object : UserDataReadWriteProperty<UserDataHolder, T?>() {
  override fun getValue(thisRef: UserDataHolder, property: KProperty<*>): T? {
    return thisRef.getUserData(getKey(property))
  }

  override fun setValue(thisRef: UserDataHolder, property: KProperty<*>, value: T?) {
    thisRef.putUserData(getKey(property), value)
  }
}

/**
 * Function for delegated properties.
 * The property will be saved to caret if this caret is not primary
 *   and to caret and editor otherwise.
 * In case of primary caret getter uses value stored in caret. If it's null, then the value from editor
 * Has nullable type.
 */
private fun <T> userDataCaretToEditor(): ReadWriteProperty<Caret, T?> = object : UserDataReadWriteProperty<Caret, T?>() {
  override fun getValue(thisRef: Caret, property: KProperty<*>): T? {
    return if (thisRef == thisRef.editor.caretModel.primaryCaret) {
      thisRef.getUserData(getKey(property)) ?: thisRef.editor.getUserData(getKey(property))
    } else {
      thisRef.getUserData(getKey(property))
    }
  }

  override fun setValue(thisRef: Caret, property: KProperty<*>, value: T?) {
    if (thisRef == thisRef.editor.caretModel.primaryCaret) {
      thisRef.editor.putUserData(getKey(property), value)
    }
    thisRef.putUserData(getKey(property), value)
  }
}

/**
 * Function for delegated properties.
 * The property will be saved to caret if this caret is not primary
 *   and to caret and editor otherwise.
 * In case of primary caret getter uses value stored in caret. If it's null, then the value from editor
 * Has not nullable type.
 */
private fun <T> userDataCaretToEditorOr(default: UserDataHolder.() -> T): ReadWriteProperty<Caret, T> = object : UserDataReadWriteProperty<Caret, T>() {
  override fun getValue(thisRef: Caret, property: KProperty<*>): T {
    val res = if (thisRef == thisRef.editor.caretModel.primaryCaret) {
      thisRef.getUserData(getKey(property)) ?: thisRef.editor.getUserData(getKey(property))
    } else {
      thisRef.getUserData(getKey(property))
    }

    if (res == null) {
      val defaultValue = thisRef.default()
      setValue(thisRef, property, defaultValue)
      return defaultValue
    }
    return res
  }

  override fun setValue(thisRef: Caret, property: KProperty<*>, value: T) {
    if (thisRef == thisRef.editor.caretModel.primaryCaret) {
      thisRef.editor.putUserData(getKey(property), value)
    }
    thisRef.putUserData(getKey(property), value)
  }
}

/**
 * Function for delegated properties.
 * The property will be delegated to UserData and has non-nullable type.
 * [default] action will be executed if UserData doesn't have this property now.
 *   The result of [default] will be put to user data and returned.
 */
private fun <T> userDataOr(default: UserDataHolder.() -> T): ReadWriteProperty<UserDataHolder, T> = object : UserDataReadWriteProperty<UserDataHolder, T>() {
  override fun getValue(thisRef: UserDataHolder, property: KProperty<*>): T {
    return thisRef.getUserData(getKey(property)) ?: run<ReadWriteProperty<UserDataHolder, T>, T> {
      val defaultValue = thisRef.default()
      thisRef.putUserData(getKey(property), defaultValue)
      defaultValue
    }
  }

  override fun setValue(thisRef: UserDataHolder, property: KProperty<*>, value: T) {
    thisRef.putUserData(getKey(property), value)
  }
}

private abstract class UserDataReadWriteProperty<in R, T> : ReadWriteProperty<R, T> {
  private var key: Key<T>? = null
  protected fun getKey(property: KProperty<*>): Key<T> {
    if (key == null) {
      key = Key.create(property.name + " by userData()")
    }
    return key as Key<T>
  }
}
