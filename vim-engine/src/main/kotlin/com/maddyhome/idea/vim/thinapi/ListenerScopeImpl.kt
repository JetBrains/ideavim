/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.models.CaretId
import com.intellij.vim.api.models.Mode
import com.intellij.vim.api.models.Range
import com.intellij.vim.api.scopes.ListenersScope
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.EditorListener
import com.maddyhome.idea.vim.common.Listener
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.common.MacroRecordingListener
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimPluginListener
import com.maddyhome.idea.vim.common.VimYankListener
import com.maddyhome.idea.vim.key.MappingOwner
import kotlinx.coroutines.runBlocking
import com.maddyhome.idea.vim.state.mode.Mode as EngineMode

class ListenerScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : ListenersScope {

  private abstract class ListenerBase(listenerOwner: ListenerOwner) : Listener {
    final override val owner: ListenerOwner = listenerOwner

    fun launch(block: suspend () -> Unit) {
      runBlocking { block() }
    }
  }

  override fun onModeChange(callback: suspend VimApi.(Mode) -> Unit) {
    val listener = object : ModeChangeListener, ListenerBase(listenerOwner) {
      override fun modeChanged(
        editor: VimEditor,
        oldMode: EngineMode,
      ) {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback(oldMode.toMode())
        }
      }
    }
    injector.listenersNotifier.modeChangeListeners.add(listener)
  }

  override fun onYank(callback: suspend VimApi.(Map<CaretId, Range.Simple>) -> Unit) {
    val listener = object : VimYankListener, ListenerBase(listenerOwner) {
      override fun yankPerformed(caretToRange: Map<ImmutableVimCaret, TextRange>) {
        val caretToRangeMap: Map<CaretId, Range.Simple> =
          caretToRange.map { (caret, range) ->
            CaretId(caret.id) to Range.Simple(range.startOffset, range.endOffset)
          }.toMap()
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback(caretToRangeMap)
        }
      }
    }
    injector.listenersNotifier.yankListeners.add(listener)
  }

  override fun onEditorCreate(callback: suspend VimApi.() -> Unit) {
    val listener = object : EditorListener, ListenerBase(listenerOwner) {
      override fun created(editor: VimEditor) {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback()
        }
      }
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override fun onEditorRelease(callback: suspend VimApi.() -> Unit) {
    val listener = object : EditorListener, ListenerBase(listenerOwner) {
      override fun released(editor: VimEditor) {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback()
        }
      }
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override fun onEditorFocusGain(callback: suspend VimApi.() -> Unit) {
    val listener = object : EditorListener, ListenerBase(listenerOwner) {
      override fun focusGained(editor: VimEditor) {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback()
        }
      }
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override fun onEditorFocusLost(callback: suspend VimApi.() -> Unit) {
    val listener = object : EditorListener, ListenerBase(listenerOwner) {
      override fun focusLost(editor: VimEditor) {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback()
        }
      }
    }
    injector.listenersNotifier.myEditorListeners.add(listener)
  }

  override fun onMacroRecordingStart(callback: suspend VimApi.() -> Unit) {
    val listener = object : MacroRecordingListener, ListenerBase(listenerOwner) {
      override fun recordingStarted() {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback()
        }
      }

      override fun recordingFinished() {
        // Not used in this listener
      }
    }
    injector.listenersNotifier.macroRecordingListeners.add(listener)
  }

  override fun onMacroRecordingFinish(callback: suspend VimApi.() -> Unit) {
    val listener = object : MacroRecordingListener, ListenerBase(listenerOwner) {
      override fun recordingStarted() {
        // Not used in this listener
      }

      override fun recordingFinished() {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback()
        }
      }
    }
    injector.listenersNotifier.macroRecordingListeners.add(listener)
  }

  override fun onIdeaVimEnabled(callback: suspend VimApi.() -> Unit) {
    val listener = object : VimPluginListener, ListenerBase(listenerOwner) {
      override fun turnedOn() {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback()
        }
      }

      override fun turnedOff() {
        // Not used in this listener
      }
    }
    injector.listenersNotifier.vimPluginListeners.add(listener)
  }

  override fun onIdeaVimDisabled(callback: suspend VimApi.() -> Unit) {
    val listener = object : VimPluginListener, ListenerBase(listenerOwner) {
      override fun turnedOn() {
        // Not used in this listener
      }

      override fun turnedOff() {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        launch {
          vimApi.callback()
        }
      }
    }
    injector.listenersNotifier.vimPluginListeners.add(listener)
  }
}
