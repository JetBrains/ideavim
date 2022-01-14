/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.model.options.helpers

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.services.OptionService

object ClipboardOptionHelper {
  var ideaputDisabled = false
    private set

  class IdeaputDisabler : AutoCloseable {
    private val containedBefore: Boolean

    init {
      val optionValue = (VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "clipboard") as VimString).value
      containedBefore = optionValue.contains(ClipboardOptionsData.ideaput)
      VimPlugin.getOptionService().removeValue(OptionService.Scope.GLOBAL, "clipboard", ClipboardOptionsData.ideaput, "clipboard")
      ideaputDisabled = true
    }

    override fun close() {
      if (containedBefore) VimPlugin.getOptionService().appendValue(OptionService.Scope.GLOBAL, "clipboard", ClipboardOptionsData.ideaput, "clipboard")
      ideaputDisabled = false
    }
  }
}
