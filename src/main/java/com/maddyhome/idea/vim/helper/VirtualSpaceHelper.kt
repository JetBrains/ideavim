/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("VirtualSpaceHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener
import com.maddyhome.idea.vim.state.mode.Mode

/**
 * The editor's own virtual space value, saved while 'virtualedit' keeps virtual space enabled in this editor.
 */
private var Editor.virtualSpaceBeforeVirtualEdit: Boolean? by userData()

/**
 * True if the editor took virtual space from the IDE setting before 'virtualedit' changed it, and should keep following
 * that setting. Setting the value in the editor stops it following the IDE
 */
private var Editor.followsIdeVirtualSpace: Boolean? by userData()

private val ideVirtualSpace: Boolean
  get() = EditorSettingsExternalizable.getInstance().isVirtualSpace

internal object VirtualSpaceUpdater : ModeChangeListener, EffectiveOptionValueChangeListener {
  override fun modeChanged(editor: VimEditor, oldMode: Mode) = editor.updateVirtualSpace()

  override fun onEffectiveValueChanged(editor: VimEditor) {
    val application = ApplicationManager.getApplication()
    if (application.isDispatchThread) {
      editor.updateVirtualSpace()
      editor.leaveInactiveVirtualSpace()
    } else {
      // Options can be set from a plugin outside the EDT
      application.invokeLater({ onEffectiveValueChanged(editor) }, { editor.ij.isDisposed })
    }
  }
}

fun enableVirtualSpace(editor: Editor) {
  if (editor.virtualSpaceBeforeVirtualEdit == null) {
    editor.virtualSpaceBeforeVirtualEdit = editor.settings.isVirtualSpace
    if (editor.followsIdeVirtualSpace == null) {
      editor.followsIdeVirtualSpace = editor.settings.isVirtualSpace == ideVirtualSpace
    }
  }
  editor.settings.isVirtualSpace = true
}

/**
 * Gives the editor its own virtual space value back. Returns false if 'virtualedit' didn't change it
 */
fun restoreVirtualSpace(editor: Editor): Boolean {
  val savedVirtualSpace = editor.virtualSpaceBeforeVirtualEdit ?: return false
  editor.virtualSpaceBeforeVirtualEdit = null
  if (!editor.isDisposed) {
    editor.settings.isVirtualSpace = if (editor.followsIdeVirtualSpace == true) ideVirtualSpace else savedVirtualSpace
  }
  return true
}

fun followIdeVirtualSpace() {
  for (editor in EditorFactory.getInstance().allEditors) {
    if (editor.followsIdeVirtualSpace == true && editor.virtualSpaceBeforeVirtualEdit == null) {
      editor.settings.isVirtualSpace = ideVirtualSpace
    }
  }
}
