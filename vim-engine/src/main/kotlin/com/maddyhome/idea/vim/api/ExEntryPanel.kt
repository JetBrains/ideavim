package com.maddyhome.idea.vim.api

interface ExEntryPanel {
  fun isActive(): Boolean
  fun clearCurrentAction()
  fun setCurrentActionPromptCharacter(char: Char)
}
