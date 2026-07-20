/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimKeymapGroup
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.vimscript.model.commands.mapping.ParseMapCommandArguments

class KeymapGroup : VimKeymapGroup {
  override fun loadKeymap(
    editor: VimEditor,
    argument: String,
  ) {
    if (!injector.vimscriptExecutor.executingFile) {
      throw exExceptionMessage("E105", ":loadkeymap")
    }

    argument.split("\n").filter { isNotWhite(it) }.forEach { line ->
      val parsed = ParseMapCommandArguments.parseKeymapEntry(line)
        ?: throw exExceptionMessage("E474.arg", line)
      injector.keyGroup
        .putKeyMapping(
          setOf(MappingMode.LANG),
          parsed.fromKeys,
          MappingOwner.IdeaVim.Keymap,
          injector.parser.parseKeys(parsed.secondArgument),
          true,
        )
    }
  }

  private fun isNotWhite(string: String): Boolean = string.trim().isNotEmpty() && !string.startsWith("\"")
}