/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.editor.EditorFactory
import com.maddyhome.idea.vim.api.LocalOptionInitialisationScenario
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.removeCaretsVisualAttributes
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener

/**
 * Reacts to runtime changes of the `pythonconsole` toggle option and enables or disables Vim in all currently open
 * Python console editors without requiring a restart or plugin toggle.
 *
 * When `set pythonconsole`: initialises Vim listeners, shortcuts, and local options for every open Python console
 * editor that has not yet been set up (e.g. consoles that were opened while the option was off).
 *
 * When `set nopythonconsole`: tears down Vim from every open Python console editor and resets the caret shape to the
 * IDE default.
 */
internal object PythonConsoleOptionChangeListener : GlobalOptionChangeListener {
  override fun onGlobalOptionChanged() {
    val enabled = injector.globalIjOptions().pythonconsole
    for (editor in EditorFactory.getInstance().allEditors) {
      if (editor.isDisposed) continue
      if (!EditorHelper.isPythonConsole(editor)) continue
      if (enabled) {
        VimListenerManager.EditorListeners.add(editor, injector.fallbackWindow, LocalOptionInitialisationScenario.NEW)
        editor.updateCaretsVisualAttributes()
      } else {
        VimListenerManager.EditorListeners.remove(editor)
        editor.removeCaretsVisualAttributes()
      }
    }
  }
}
