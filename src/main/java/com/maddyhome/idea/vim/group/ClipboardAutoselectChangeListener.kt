/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.options.OptionConstants

/**
 * Keeps the platform's own PRIMARY updates in line with `autoselect` in 'clipboard'.
 *
 * The IDE copies every editor selection to PRIMARY on its own, so without `autoselect` IdeaVim
 * turns that off through the registry. The previous registry value is restored once `autoselect` is
 * back or IdeaVim is turned off, so a user who disabled it by hand keeps it disabled.
 */
internal object ClipboardAutoselectChangeListener : GlobalOptionChangeListener {
  private const val UPDATE_PRIMARY_REGISTRY_KEY = "editor.caret.update.primary.selection"

  private var suppression: Disposable? = null

  override fun onGlobalOptionChanged() {
    if (OptionConstants.clipboard_autoselect in injector.globalOptions().clipboard) {
      restore()
    } else {
      suppress()
    }
  }

  fun restore() {
    suppression?.let { Disposer.dispose(it) }
    suppression = null
  }

  private fun suppress() {
    // The suppression is a child of onOffDisposable, so it may already have been disposed with it
    if (suppression?.let { !Disposer.isDisposed(it) } == true) return
    val disposable = Disposer.newDisposable(VimPlugin.getInstance().onOffDisposable, "IdeaVimAutoselect")
    Registry.get(UPDATE_PRIMARY_REGISTRY_KEY).setValue(false, disposable)
    suppression = disposable
  }
}
