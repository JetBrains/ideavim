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
      val optionValue = (VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "clipboard", null) as VimString).value
      containedBefore = optionValue.contains(ClipboardOptionsData.ideaput)
      VimPlugin.getOptionService().removeValue(OptionService.Scope.GLOBAL, "clipboard", ClipboardOptionsData.ideaput, null, "clipboard")
      ideaputDisabled = true
    }

    override fun close() {
      if (containedBefore) VimPlugin.getOptionService().appendValue(OptionService.Scope.GLOBAL, "clipboard", ClipboardOptionsData.ideaput, null, "clipboard")
      ideaputDisabled = false
    }
  }
}
