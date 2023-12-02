/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.state.mode.Mode

public class VimListenersNotifier {
  public val modeChangeListeners: MutableList<ModeChangeListener> = mutableListOf()
  public val editorFocusListeners: MutableList<EditorFocusListener> = mutableListOf()
  public val macroRecordingListeners: MutableList<MacroRecordingListener> = mutableListOf()
  
  public fun notifyModeChanged(editor: VimEditor, oldMode: Mode) {
    modeChangeListeners.forEach { it.modeChanged(editor, oldMode) }
  }
  
  public fun notifyEditorFocusGained(editor: VimEditor) {
    editorFocusListeners.forEach { it.focusGained(editor) }
  }
  
  public fun notifyEditorFocusLost(editor: VimEditor) {
    editorFocusListeners.forEach { it.focusLost(editor) }
  }
  
  public fun notifyMacroRecordingStarted(editor: VimEditor, register: Char) {
    macroRecordingListeners.forEach { it.recordingStarted(editor, register) }
  }
  
  public fun notifyMacroRecordingFinished(editor: VimEditor, register: Char) {
    macroRecordingListeners.forEach { it.recordingFinished(editor, register) }
  }
}