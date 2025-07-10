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
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.common.MacroRecordingListener
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimPluginListener
import com.maddyhome.idea.vim.common.VimYankListener
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.maddyhome.idea.vim.state.mode.Mode as EngineMode

class ListenerScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : ListenersScope {
  private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)

  override suspend fun onModeChange(callback: suspend VimScope.(Mode) -> Unit) {
    val listener = object : ModeChangeListener {
      override fun modeChanged(
        editor: VimEditor,
        oldMode: EngineMode,
      ) {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback(oldMode.toMode())
        }
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.modeChangeListeners.add(listener)
  }

  override suspend fun onYank(callback: suspend VimScope.(Map<CaretId, Range.Simple>) -> Unit) {
    val listener = object : VimYankListener {
      override fun yankPerformed(caretToRange: Map<ImmutableVimCaret, TextRange>) {
        val caretToRangeMap: Map<CaretId, Range.Simple> =
          caretToRange.map { (caret, range) ->
            CaretId(caret.id) to Range.Simple(range.startOffset, range.endOffset)
          }.toMap()
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback(caretToRangeMap)
        }
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.yankListeners.add(listener)
  }

  override suspend fun onEditorCreate(callback: suspend VimScope.() -> Unit) {
    val listener = object : EditorListener {
      override fun created(editor: VimEditor) {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback()
        }
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override suspend fun onEditorRelease(callback: suspend VimScope.() -> Unit) {
    val listener = object : EditorListener {
      override fun released(editor: VimEditor) {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback()
        }
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override suspend fun onEditorFocusGain(callback: suspend VimScope.() -> Unit) {
    val listener = object : EditorListener {
      override fun focusGained(editor: VimEditor) {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback()
        }
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override suspend fun onEditorFocusLost(callback: suspend VimScope.() -> Unit) {
    val listener = object : EditorListener {
      override fun focusLost(editor: VimEditor) {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback()
        }
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override suspend fun onMacroRecordingStart(callback: suspend VimScope.() -> Unit) {
    val listener = object : MacroRecordingListener {
      override fun recordingStarted() {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback()
        }
      }

      override fun recordingFinished() {
        // Not used in this listener
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.macroRecordingListeners.add(listener)
  }

  override suspend fun onMacroRecordingFinish(callback: suspend VimScope.() -> Unit) {
    val listener = object : MacroRecordingListener {
      override fun recordingStarted() {
        // Not used in this listener
      }

      override fun recordingFinished() {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback()
        }
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.macroRecordingListeners.add(listener)
  }

  override suspend fun onIdeaVimEnabled(callback: suspend VimScope.() -> Unit) {
    val listener = object : VimPluginListener {
      override fun turnedOn() {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback()
        }
      }

      override fun turnedOff() {
        // Not used in this listener
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.vimPluginListeners.add(listener)
  }

  override suspend fun onIdeaVimDisabled(callback: suspend VimScope.() -> Unit) {
    val listener = object : VimPluginListener {
      override fun turnedOn() {
        // Not used in this listener
      }

      override fun turnedOff() {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback()
        }
      }

      override val owner: ListenerOwner
        get() = listenerOwner
    }
    injector.listenersNotifier.vimPluginListeners.add(listener)
  }

  override suspend fun onGlobalOptionChange(optionName: String, callback: suspend VimScope.() -> Unit) {
    val option = injector.optionGroup.getOption(optionName) ?: return
    val listener = object : GlobalOptionChangeListener {
      override fun onGlobalOptionChanged() {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        coroutineScope.launch {
          vimScope.callback()
        }
      }
    }
    // todo: this listener should be added to the VimListenerNotifier
    injector.optionGroup.addGlobalOptionChangeListener(option, listener)
  }
}
