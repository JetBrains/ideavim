/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.vimscript.services.VimRcService
import kotlin.io.path.exists
import kotlin.io.path.readText

object KeymapChangeListener : EffectiveOptionValueChangeListener {
  override fun onEffectiveValueChanged(editor: VimEditor) {
    injector.keyGroup.removeKeyMapping(MappingOwner.IdeaVim.Keymap)

    val name = injector.optionGroup.getOptionValue(Options.keymap, OptionAccessScope.EFFECTIVE(editor)).value
    if (name.isEmpty()) return

    val content = readKeymap(name)
    if (content == null) {
      injector.messages.showErrorMessage(editor, exExceptionMessage("E544").message)
      return
    }

    val executor = injector.vimscriptExecutor
    val wasExecutingFile = executor.executingFile
    executor.executingFile = true
    try {
      executor.execute(
        content,
        editor,
        injector.executionContextManager.getEditorExecutionContext(editor),
        skipHistory = true,
      )
    } finally {
      executor.executingFile = wasExecutingFile
    }
  }

  private fun readKeymap(name: String): String? {
    val userFile = VimRcService.getXdgConfigHome()?.resolve("ideavim/keymap/$name.vim")
    if (userFile != null && userFile.exists()) {
      return userFile.readText(Charsets.UTF_8)
    }
    return javaClass.classLoader.getResourceAsStream("keymap/$name.vim")
      ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
  }
}
