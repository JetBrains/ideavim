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
public class VimListenersNotifier {
  public val modeChangeListeners: MutableCollection<ModeChangeListener> = ConcurrentLinkedDeque()
  public val myEditorListeners: MutableCollection<EditorListener> = ConcurrentLinkedDeque()
  public val macroRecordingListeners: MutableCollection<MacroRecordingListener> = ConcurrentLinkedDeque()
  public val vimPluginListeners: MutableCollection<VimPluginListener> = ConcurrentLinkedDeque()
  
  public fun notifyModeChanged(editor: VimEditor, oldMode: Mode) {
    modeChangeListeners.forEach { it.modeChanged(editor, oldMode) }
  }

  public fun notifyEditorCreated(editor: VimEditor) {
    myEditorListeners.forEach { it.created(editor) }
  }

  public fun notifyEditorReleased(editor: VimEditor) {
    myEditorListeners.forEach { it.released(editor) }
  }

  public fun notifyEditorFocusGained(editor: VimEditor) {
    myEditorListeners.forEach { it.focusGained(editor) }
  }

  public fun notifyEditorFocusLost(editor: VimEditor) {
    myEditorListeners.forEach { it.focusLost(editor) }
  }

  public fun notifyMacroRecordingStarted(editor: VimEditor, register: Char) {
    macroRecordingListeners.forEach { it.recordingStarted(editor, register) }
  }
  
  public fun notifyMacroRecordingFinished(editor: VimEditor, register: Char) {
    macroRecordingListeners.forEach { it.recordingFinished(editor, register) }
  }

  public fun notifyPluginTurnedOn() {
    vimPluginListeners.forEach { it.turnedOn() }
  }

  public fun notifyPluginTurnedOff() {
    vimPluginListeners.forEach { it.turnedOff() }
  }
}