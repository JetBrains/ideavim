/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Mode
import com.intellij.vim.api.Range
import com.intellij.vim.api.scopes.ListenersScope
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.EditorListener
import com.maddyhome.idea.vim.common.IsReplaceCharListener
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.common.MacroRecordingListener
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimPluginListener
import com.maddyhome.idea.vim.common.VimYankListener
import com.maddyhome.idea.vim.impl.state.toMappingMode

class ListenerScopeImpl(
  private val listenerOwner: ListenerOwner
): ListenersScope {
  override fun onModeChange(callback: VimScope.(Mode) -> Unit) {
    val listener = object : ModeChangeListener {
      override fun modeChanged(
        editor: VimEditor,
        oldMode: com.maddyhome.idea.vim.state.mode.Mode,
      ) {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback(oldMode.toMappingMode().toMode())
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.modeChangeListeners.add(listener)
  }

  override fun onYank(callback: VimScope.(Map<CaretId, Range>) -> Unit) {
    val listener = object : VimYankListener {
      override fun yankPerformed(caretToRange: Map<ImmutableVimCaret, TextRange>) {
        val caretToRangeMap: Map<CaretId, Range> =
          caretToRange.map {
              (caret, range) -> CaretId(caret.id) to Range(range.startOffset, range.endOffset)
          }.toMap()
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback(caretToRangeMap)
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.yankListeners.add(listener)
  }

  override fun onEditorCreate(callback: VimScope.() -> Unit) {
    val listener = object : EditorListener {
      override fun created(editor: VimEditor) {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback()
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override fun onEditorRelease(callback: VimScope.() -> Unit) {
    val listener = object : EditorListener {
      override fun released(editor: VimEditor) {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback()
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override fun onEditorFocusGain(callback: VimScope.() -> Unit) {
    val listener = object : EditorListener {
      override fun focusGained(editor: VimEditor) {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback()
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override fun onEditorFocusLost(callback: VimScope.() -> Unit) {
    val listener = object : EditorListener {
      override fun focusLost(editor: VimEditor) {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback()
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override fun onMacroRecordingStart(callback: VimScope.() -> Unit) {
    val listener = object : MacroRecordingListener {
      override fun recordingStarted() {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback()
      }

      override fun recordingFinished() {
        // Not used in this listener
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.macroRecordingListeners.add(listener)
  }

  override fun onMacroRecordingFinish(callback: VimScope.() -> Unit) {
    val listener = object : MacroRecordingListener {
      override fun recordingStarted() {
        // Not used in this listener
      }

      override fun recordingFinished() {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback()
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.macroRecordingListeners.add(listener)
  }

  override fun onPluginTurnOn(callback: VimScope.() -> Unit) {
    val listener = object : VimPluginListener {
      override fun turnedOn() {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback()
      }

      override fun turnedOff() {
        // Not used in this listener
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.vimPluginListeners.add(listener)
  }

  override fun onPluginTurnOff(callback: VimScope.() -> Unit) {
    val listener = object : VimPluginListener {
      override fun turnedOn() {
        // Not used in this listener
      }

      override fun turnedOff() {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback()
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.vimPluginListeners.add(listener)
  }

  override fun onReplaceCharChange(callback: VimScope.() -> Unit) {
    val listener = object : IsReplaceCharListener {
      override fun isReplaceCharChanged(editor: VimEditor) {
        val vimScope = VimScopeImpl(listenerOwner)
        vimScope.callback()
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.isReplaceCharListeners.add(listener)
  }
}
