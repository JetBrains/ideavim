package com.maddyhome.idea.vim.api

interface VimSelectionModel {
  val selectionStart: Int
  val selectionEnd: Int

  fun hasSelection(): Boolean
}
