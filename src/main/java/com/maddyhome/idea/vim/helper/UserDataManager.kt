/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("UserDataManager")
@file:Suppress("ObjectPropertyName")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.maddyhome.idea.vim.api.CaretRegisterStorageBase
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.group.visual.vimLeadSelectionOffset
import com.maddyhome.idea.vim.ui.ExOutputPanel
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * @author Alex Plate
 */

// NOTE: Carets, including the primary caret, can be replaced when the caret is moved in visual block mode.
// No attempt is made to maintain this user data (apart from vimLastColum which is set during caret movement)
// Non-transient data will be lost!

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

// Keep a track of the column that we intended to navigate to but were unable to. This might be because of inlays,
// virtual indent or moving from the end of a long line to the end of a short line. Keep a track of the position when
// the value is set, if it's not the same during get, we've been moved by IJ and so no longer valid
var Caret.vimLastColumn: Int
  get() {
    if (visualPosition != _vimLastColumnPos) {
      vimLastColumn = inlayAwareVisualColumn
    }
    return _vimLastColumn
  }
  set(value) {
    _vimLastColumn = value
    _vimLastColumnPos = visualPosition
  }
private var Caret._vimLastColumn: Int by userDataCaretToEditorOr { (this as Caret).inlayAwareVisualColumn }
private var Caret._vimLastColumnPos: VisualPosition? by userDataCaretToEditor()

// TODO: Is this a per-caret setting? This data is non-transient, so could be lost during visual block motion
var Caret.vimLastVisualOperatorRange: VisualChange? by userDataCaretToEditor()

// Transient data. Does not need to be restored during visual block motion
var Caret.vimInsertStart: RangeMarker by userDataOr {
  (this as Caret).editor.document.createRangeMarker(this.offset, this.offset)
}

// TODO: Data could be lost during visual block motion
var Caret.registerStorage: CaretRegisterStorageBase? by userDataCaretToEditor()

// ------------------ Editor
fun unInitializeEditor(editor: Editor) {
  editor.vimLastSelectionType = null
  editor.vimStateMachine = null
  editor.vimMorePanel = null
  editor.vimExOutput = null
  editor.vimLastHighlighters = null
}

var Editor.vimLastSearch: String? by userData()
var Editor.vimLastHighlighters: MutableCollection<RangeHighlighter>? by userData()
var Editor.vimIncsearchCurrentMatchOffset: Int? by userData()

/***
 * @see :help visualmode()
 */
var Editor.vimLastSelectionType: SelectionType? by userData()
var Editor.vimStateMachine: VimStateMachine? by userData()
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
var Editor.vimChangeActionSwitchMode: VimStateMachine.Mode? by userData()

/**
 * Function for delegated properties.
 * The property will be delegated to UserData and has nullable type.
 */
fun <T> userData(): ReadWriteProperty<UserDataHolder, T?> =
  object : UserDataReadWriteProperty<UserDataHolder, T?>() {
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
private fun <T> userDataCaretToEditor(): ReadWriteProperty<Caret, T?> =
  object : UserDataReadWriteProperty<Caret, T?>() {
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
private fun <T> userDataCaretToEditorOr(default: UserDataHolder.() -> T): ReadWriteProperty<Caret, T> =
  object : UserDataReadWriteProperty<Caret, T>() {
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
private fun <T> userDataOr(default: UserDataHolder.() -> T): ReadWriteProperty<UserDataHolder, T> =
  object : UserDataReadWriteProperty<UserDataHolder, T>() {
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
