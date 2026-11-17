/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.intellij.openapi.util.registry.RegistryValueSource
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.options.OptionConstants
import org.jetbrains.annotations.TestOnly
import java.awt.GraphicsEnvironment

/**
 * Keeps the platform's own PRIMARY updates in line with `autoselect` in 'clipboard'.
 *
 * The IDE copies every editor selection to PRIMARY on its own, so without `autoselect` IdeaVim
 * turns that off through the registry. Only where PRIMARY exists: elsewhere the editor doesn't update it anyway, and
 * the same key also drives the terminal's copy on selection.
 *
 * The registry has no runtime-only values: whatever is set is saved with the IDE settings, also by autosave. So the
 * override is never treated as "previous value to put back": [restore] removes any `false` that was set
 * programmatically (IdeaVim's own, also one left by an earlier session or version), and keeps a `false` set by the user
 * in the Registry dialog. It also runs before the settings are saved on exit, so a clean exit leaves nothing behind.
 *
 * The other half of `autoselect`, the visual selection published by IdeaVim, is `PrimaryAutoselect` of vim-engine.
 */
internal object ClipboardAutoselectChangeListener : GlobalOptionChangeListener {
  private const val UPDATE_PRIMARY_REGISTRY_KEY = "editor.caret.update.primary.selection"

  // Headless AWT throws on the PRIMARY lookup instead of returning null
  private var systemSelectionSupported: () -> Boolean = {
    !GraphicsEnvironment.isHeadless() && CopyPasteManager.getInstance().isSystemSelectionSupported
  }

  override fun onGlobalOptionChanged() {
    if (OptionConstants.clipboard_autoselect in injector.globalOptions().clipboard || !systemSelectionSupported()) {
      restore()
    } else {
      suppress()
    }
  }

  fun restore() {
    val value = registryValue()
    if (!value.asBoolean() && value.getSource() == RegistryValueSource.SYSTEM) {
      value.resetToDefault()
    }
  }

  private fun suppress() {
    val value = registryValue()
    if (value.asBoolean()) value.setValue(false)
  }

  private fun registryValue(): RegistryValue = Registry.get(UPDATE_PRIMARY_REGISTRY_KEY)

  @TestOnly
  fun withSystemSelectionSupported(supported: Boolean, action: () -> Unit) {
    val old = systemSelectionSupported
    systemSelectionSupported = { supported }
    try {
      action()
    } finally {
      systemSelectionSupported = old
    }
  }
}
