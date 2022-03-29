package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.pushSelectMode
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

abstract class VimVisualMotionGroupBase : VimVisualMotionGroup {
  override val exclusiveSelection: Boolean
    get() = (injector.optionService.getOptionValue(
      OptionScope.GLOBAL,
      OptionConstants.selectionName
    ) as VimString).value == "exclusive"
  override val selectionAdj: Int
    get() = if (exclusiveSelection) 0 else 1

  override fun enterSelectMode(editor: VimEditor, subMode: CommandState.SubMode): Boolean {
    editor.commandState.pushSelectMode(subMode)
    editor.forEachCaret { it.vimSelectionStart = it.vimLeadSelectionOffset }
    return true
  }
}
