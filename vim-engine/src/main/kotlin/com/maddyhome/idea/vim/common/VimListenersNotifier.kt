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
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.concurrent.ConcurrentLinkedDeque

@Internal // please do not use this class in your plugins, API is not final and will be changed in future releases
class VimListenersNotifier {
  val modeChangeListeners: MutableCollection<ModeChangeListener> = ConcurrentLinkedDeque()
  val myEditorListeners: MutableCollection<EditorListener> = ConcurrentLinkedDeque()
  val macroRecordingListeners: MutableCollection<MacroRecordingListener> = ConcurrentLinkedDeque()
  val vimPluginListeners: MutableCollection<VimPluginListener> = ConcurrentLinkedDeque()
  val isReplaceCharListeners: MutableCollection<IsReplaceCharListener> = ConcurrentLinkedDeque()
  
  fun notifyModeChanged(editor: VimEditor, oldMode: Mode) {
    modeChangeListeners.forEach { it.modeChanged(editor, oldMode) }
  }

  fun notifyEditorCreated(editor: VimEditor) {
    myEditorListeners.forEach { it.created(editor) }
  }

  fun notifyEditorReleased(editor: VimEditor) {
    myEditorListeners.forEach { it.released(editor) }
  }

  fun notifyEditorFocusGained(editor: VimEditor) {
    myEditorListeners.forEach { it.focusGained(editor) }
  }

  fun notifyEditorFocusLost(editor: VimEditor) {
    myEditorListeners.forEach { it.focusLost(editor) }
  }

  fun notifyMacroRecordingStarted() {
    macroRecordingListeners.forEach { it.recordingStarted() }
  }
  
  fun notifyMacroRecordingFinished() {
    macroRecordingListeners.forEach { it.recordingFinished() }
  }

  fun notifyPluginTurnedOn() {
    vimPluginListeners.forEach { it.turnedOn() }
  }

  fun notifyPluginTurnedOff() {
    vimPluginListeners.forEach { it.turnedOff() }
  }

  fun notifyIsReplaceCharChanged(editor: VimEditor) {
    isReplaceCharListeners.forEach { it.isReplaceCharChanged(editor) }
  }

  fun reset() {
    modeChangeListeners.clear()
    myEditorListeners.clear()
    macroRecordingListeners.clear()
    vimPluginListeners.clear()
    isReplaceCharListeners.clear()
  }
}