package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.ExEntryPanel

@Service
class IjExEntryPanel : ExEntryPanel {
  override fun isActive(): Boolean {
    return com.maddyhome.idea.vim.ui.ex.ExEntryPanel.getInstance().isActive
  }

  override fun clearCurrentAction() {
    com.maddyhome.idea.vim.ui.ex.ExEntryPanel.getInstance().entry.clearCurrentAction()
  }

  override fun setCurrentActionPromptCharacter(char: Char) {
    com.maddyhome.idea.vim.ui.ex.ExEntryPanel.getInstance().entry.setCurrentActionPromptCharacter(char)
  }
}
