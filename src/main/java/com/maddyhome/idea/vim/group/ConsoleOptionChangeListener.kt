/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.maddyhome.idea.vim.api.LocalOptionInitialisationScenario
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.isEnabledConsole
import com.maddyhome.idea.vim.helper.removeCaretsVisualAttributes
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener

/**
 * Reacts to runtime changes of the 'ideaeditor' option and enables or disables Vim in all currently open console
 * editors without requiring a restart or plugin toggle.
 *
 * Enabling initialises Vim listeners, shortcuts and local options for every open console editor that has not yet been
 * set up, e.g. consoles that were opened while the option didn't list them. Disabling tears Vim down again and resets
 * the caret shape to the IDE default.
 */
internal object ConsoleOptionChangeListener : GlobalOptionChangeListener {
  override fun onGlobalOptionChanged() {
    for (editor in EditorFactory.getInstance().allEditors) {
      if (editor.isDisposed) continue
      if (!editor.isConsole()) continue
      if (editor.isEnabledConsole()) {
        VimListenerManager.EditorListeners.add(editor, injector.fallbackWindow, LocalOptionInitialisationScenario.NEW)
        editor.updateCaretsVisualAttributes()
      } else {
        VimListenerManager.EditorListeners.remove(editor)
        editor.removeCaretsVisualAttributes()
      }
    }
  }

  private fun Editor.isConsole() = EditorHelper.isPythonConsole(this) || EditorHelper.isRunConsole(this)
}
